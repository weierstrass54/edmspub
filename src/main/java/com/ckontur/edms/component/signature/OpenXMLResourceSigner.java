package com.ckontur.edms.component.signature;

import com.ckontur.edms.model.CertifiedKeyPair;
import com.ckontur.edms.utils.DateUtils;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Try;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.crypt.dsig.SignatureConfig;
import org.apache.poi.poifs.crypt.dsig.SignatureInfo;
import org.apache.poi.poifs.crypt.dsig.SignaturePart;
import org.apache.poi.poifs.crypt.dsig.facets.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Collections;

@Component
public class OpenXMLResourceSigner implements ResourceSigner {
    @Override
    public boolean validateSign(Resource resource) {
        return Try.of(() -> resource.getInputStream().readAllBytes())
            .flatMap(document ->
                Try.withResources(
                    () -> OPCPackage.open(new ByteArrayInputStream(document))
                )
                .of(opc -> {
                    SignatureConfig signatureConfig = new SignatureConfig();
                    SignatureInfo signatureInfo = new SignatureInfo();
                    signatureInfo.setOpcPackage(opc);
                    signatureInfo.setSignatureConfig(signatureConfig);
                    return Stream.ofAll(signatureInfo.getSignatureParts()).map(SignaturePart::validate)
                        .fold(true, (a, b) -> a && b);
                })
            ).getOrElse(false);
    }

    @Override
    public Try<Resource> sign(Resource resource, CertifiedKeyPair certifiedKeyPair, String description) {
        return Try.of(() -> resource.getInputStream().readAllBytes())
            .flatMap(content -> Try.of(() -> {
                ByteArrayOutputStream document = new ByteArrayOutputStream(content.length);
                document.write(content);
                return document;
            }))
            .flatMap(document ->
                Try.withResources(
                    () -> OPCPackage.open(new ByteArrayInputStream(document.toByteArray()))
                )
                .of(opc -> {
                    SignatureInfo signatureInfo = makeSignature(opc, certifiedKeyPair, description);
                    signatureInfo.confirmSignature();
                    document.reset();
                    opc.save(document);
                    return document;
                })
            )
            .map(document -> new ByteArrayResource(document.toByteArray()));
    }

    private static SignatureInfo makeSignature(OPCPackage opc, CertifiedKeyPair certifiedKeyPair, String description) {
        SignatureConfig signatureConfig = new SignatureConfig();
        signatureConfig.setAllowMultipleSignatures(true);
        signatureConfig.setKey(certifiedKeyPair.getPrivateKey());
        signatureConfig.setExecutionTime(DateUtils.of(LocalDateTime.now()));
        signatureConfig.setSignatureDescription(description);
        signatureConfig.setSignatureFacets(OpenXMLResourceSigner.getSignatureFacets(opc).toJavaList());
        signatureConfig.setSigningCertificateChain(Collections.singletonList(certifiedKeyPair.getX509Certificate()));

        SignatureInfo signatureInfo = new SignatureInfo();
        signatureInfo.setOpcPackage(opc);
        signatureInfo.setSignatureConfig(signatureConfig);
        return signatureInfo;
    }

    private static List<SignatureFacet> getSignatureFacets(OPCPackage opcPackage) {
        SignatureConfig signatureConfig = new SignatureConfig();
        SignatureInfo signatureInfo = new SignatureInfo();
        signatureInfo.setOpcPackage(opcPackage);
        signatureInfo.setSignatureConfig(signatureConfig);
        List<SignatureFacet> facets = List.of(
            new KeyInfoSignatureFacet(), new XAdESSignatureFacet(), new Office2010SignatureFacet()
        );
        if (signatureInfo.getSignatureParts().iterator().hasNext()) {
            return facets.prepend(new SignatureInfoFacet());
        }
        return facets.prepend(new OOXMLSignatureFacet());
    }
}
