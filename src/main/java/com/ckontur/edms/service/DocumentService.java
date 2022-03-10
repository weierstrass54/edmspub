package com.ckontur.edms.service;

import com.ckontur.edms.component.DummyEmailService;
import com.ckontur.edms.exception.InvalidArgumentException;
import com.ckontur.edms.exception.InvalidDocumentException;
import com.ckontur.edms.model.Document;
import com.ckontur.edms.model.DocumentType;
import com.ckontur.edms.model.Permission;
import com.ckontur.edms.model.User;
import com.ckontur.edms.repository.*;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class DocumentService {
    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final SignRouteTemplateRepository signRouteTemplateRepository;
    private final DocumentRepository documentRepository;
    private final CertifiedKeyPairRepository certifiedKeyPairRepository;
    private final SignService signService;
    private final DummyEmailService emailService;

    public Option<Resource> findById(Long id) {
        return documentRepository.findById(id)
            .flatMap(document -> fileRepository.download(document.getPath()));
    }

    public Option<Try<List<Document>>> uploadDocuments(User user, MultipartFile[] files, Long signTemplateId) {
        return signRouteTemplateRepository.findById(signTemplateId).map(signRouteTemplate ->
            uploadDocuments(user, files, signRouteTemplate.getUserIds())
        );
    }

    public Try<List<Document>> uploadDocuments(User user, MultipartFile[] files, List<Long> signerIds) {
        return Try.of(() ->
            userRepository.findAllByIds(signerIds).filter(u -> u.getPermissions().contains(Permission.SIGN))
        ).filter(
            signers -> signers.size() == signerIds.size(),
            () -> new InvalidArgumentException("Один из указанных подписантов не имеет права подписывать документы.")
        ).flatMap(signers ->
            Try.sequence(
                Stream.ofAll(Arrays.stream(files))
                    .map(file -> fileRepository.upload(file)
                        .flatMap(fileName -> documentRepository.create(
                            user, signers, DocumentType.of(file), fileName, file.getSize(), LocalDateTime.now()
                        ))
                    )
                .toList()
            )
            .peek(__ -> emailService.sendMessage(signers.head().getEmail(), "Требуется подписание нового документа.", "Тело письма.."))
        ).map(List::ofAll);
    }

    public Option<Try<Document>> signById(User user, Long id, String notes) {
        return documentRepository.findById(id).flatMap(document ->
            fileRepository.download(document.getPath()).map(file ->
                certifiedKeyPairRepository.findByUserId(user.getId()).toTry(
                    () -> new InvalidDocumentException("Отсутствует ЭЦП у пользователя #" + user.getId())
                ).flatMap(ckp ->
                    document.getSignatureRoute().find(sr -> !sr.isSigned())
                        .filter(sr -> sr.getUser().getId().equals(user.getId())).toTry(() ->
                            new InvalidDocumentException("Пользователь #" + user.getId() + " не должен подписывать этот документ.")
                        ).flatMap(__ ->
                            signService.sign(file, document.getType(), ckp, notes).flatMap(signedFile ->
                                fileRepository.upload(signedFile, document.getPath(), document.getType())
                            )
                        )
                ).flatMap(path ->
                    documentRepository.sign(document, user, notes)
                ).peek(doc ->
                    doc.getSignatureRoute().find(sr -> !sr.isSigned()).forEach(sr ->
                        emailService.sendMessage(sr.getUser().getEmail(), "Требуется подписание документа.", "Тело письма..")
                    )
                )
            )
        );
    }

    public Option<Try<Document>> deleteById(Long id) {
        return documentRepository.deleteById(id)
            .map(document ->
                document.flatMap(d ->
                    fileRepository.delete(d.getPath())
                ).flatMap(__ -> document)
            );
    }
}
