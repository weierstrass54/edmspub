package com.ckontur.edms.model;

import com.ckontur.edms.exception.InvalidArgumentException;
import com.ckontur.edms.exception.InvalidEnumException;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Getter
@RequiredArgsConstructor
public enum DocumentType {
    OPEN_XML("open_xml"), PDF("pdf");

    private final String value;

    public static DocumentType of(String value) {
        return Stream.of(values())
            .filter(p -> p.name().equals(value.toUpperCase()))
            .getOrElseThrow(() -> new InvalidEnumException("Типа документа " + value + " не существует."));
    }

    public static DocumentType of(MultipartFile file) {
        return Option.of(file.getContentType())
            .flatMap(contentType -> switch(contentType) {
                case "application/pdf" -> Option.of(PDF);
                case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        -> Option.of(OPEN_XML);
                default -> Option.none();
            }).getOrElseThrow(() -> new InvalidArgumentException("Файл имеет неподдерживаемый MIME-type."));
    }
}
