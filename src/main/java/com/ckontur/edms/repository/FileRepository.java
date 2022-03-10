package com.ckontur.edms.repository;

import com.ckontur.edms.model.DocumentType;
import io.minio.*;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.management.InstanceAlreadyExistsException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static io.vavr.API.*;
import static io.vavr.Patterns.$Failure;
import static io.vavr.Patterns.$Success;
import static io.vavr.Predicates.instanceOf;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FileRepository {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
    private final MinioClient minioClient;

    @Value("${edms.minio.bucket:edms}")
    private String minioBucket;

    public Option<Resource> download(String fileName) {
        return Try.withResources(() -> minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(minioBucket)
                .object(fileName)
                .build()
        ))
        .of(is -> new ByteArrayResource(is.readAllBytes()))
        .transform(resource -> Match(resource).of(
            Case($Success($()), Option::of),
            Case($Failure($()), t -> {
                log.error("Downloading file error: {}", t.getMessage(), t);
                return Option.none();
            })
        ));
    }

    public Try<String> upload(MultipartFile file) {
        return Try.of(file::getInputStream)
            .flatMap(is -> upload(fileName(file), file.getSize(), file.getContentType(), is));
    }

    public Try<String> upload(Resource resource, String fileName, DocumentType type) {
        return Try.of(resource::getInputStream)
            .flatMap(is -> Try.of(resource::contentLength).flatMap(cl -> upload(fileName, cl, type.getValue(), is)));
    }

    private Try<String> upload(String fileName, Long size, String contentType, InputStream is) {
        return Try.of(() -> {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(minioBucket)
                    .object(fileName)
                    .stream(is, size, -1)
                    .contentType(contentType)
                    .build()
            );
            return fileName;
        });
    }

    public Try<String> delete(String fileName) {
        return Try.of(() -> {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(minioBucket)
                    .object(fileName)
                    .build()
            );
            return fileName;
        });
    }

    @PostConstruct
    private void init() {
        Try.of(() -> minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioBucket).build()))
            .filter(exists -> !exists, () -> new InstanceAlreadyExistsException("MinIO bucket already exists."))
            .flatMap(__ -> Try.of(() -> {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioBucket).build());
                return true;
            }))
            .getOrElseGet(throwable -> Match(throwable).of(
                Case($(instanceOf(InstanceAlreadyExistsException.class)), __ -> true),
                Case($(), t -> {
                    throw new RuntimeException(t);
                })
            ));
    }

    private static String fileName(MultipartFile file) {
        return Option.of(file.getOriginalFilename())
            .filter(f -> f.contains("."))
            .map(f -> {
                String name = f.substring(0, f.lastIndexOf("."));
                String extension = f.substring(f.lastIndexOf(".") + 1);
                return name + "_" + LocalDateTime.now().format(DTF) + "." + extension;
            })
            .getOrElse(file.getOriginalFilename() + "_" + LocalDateTime.now().format(DTF));
    }
}
