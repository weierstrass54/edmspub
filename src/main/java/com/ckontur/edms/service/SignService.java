package com.ckontur.edms.service;

import com.ckontur.edms.component.signature.OpenXMLResourceSigner;
import com.ckontur.edms.component.signature.PdfResourceSigner;
import com.ckontur.edms.model.CertifiedKeyPair;
import com.ckontur.edms.model.DocumentType;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SignService {
    private final PdfResourceSigner pdfResourceSigner;
    private final OpenXMLResourceSigner openXMLResourceSigner;

    public Try<Resource> sign(Resource resource, DocumentType type, CertifiedKeyPair ckp, String notes) {
        return switch (type) {
            case PDF -> pdfResourceSigner.sign(resource, ckp, notes);
            case OPEN_XML -> openXMLResourceSigner.sign(resource, ckp, notes);
        };
    }
}
