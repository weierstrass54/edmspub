package com.ckontur.edms.component;

import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.converter.pdf.PdfConverter;
import org.apache.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Component
@RequiredArgsConstructor
public class Docx2PdfConverter {
    private final PdfConverter pdfConverter;

    public Try<Resource> convert(Resource resource) {
        return Try.of(() -> resource.getInputStream().readAllBytes())
            .flatMap(document ->
                Try.withResources(
                    () -> OPCPackage.open(new ByteArrayInputStream(document))
                )
                .of(opc -> {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    pdfConverter.convert(new XWPFDocument(opc), baos, PdfOptions.create());
                    return new ByteArrayResource(baos.toByteArray());
                })
            );
    }
}
