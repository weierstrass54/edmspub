package com.ckontur.edms.component.signature;

import com.ckontur.edms.model.CertifiedKeyPair;
import io.vavr.collection.List;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.function.Function;

@Component
public class PdfResourceSigner implements ResourceSigner {
    @Override
    @SuppressWarnings("unchecked") // this is not good but necessary
    public boolean validateSign(Resource resource) {
        return Try.of(() -> resource.getInputStream().readAllBytes())
            .flatMap(document ->
                Try.withResources(() -> PDDocument.load(document))
                    .of(pdf -> Try.sequence(
                        List.ofAll(pdf.getSignatureDictionaries())
                            .map(pd -> Try.of(() -> {
                                CMSSignedData cmsSigned = new CMSSignedData(
                                    new CMSProcessableByteArray(pd.getSignedContent(document)),
                                    pd.getContents(document)
                                );
                                SignerInformationStore sis = cmsSigned.getSignerInfos();
                                Store<X509CertificateHolder> cs = cmsSigned.getCertificates();
                                return Try.sequence(
                                    List.ofAll(sis.getSigners()).map(si -> Try.of(() -> {
                                        X509CertificateHolder x509 = (X509CertificateHolder) cs.getMatches(si.getSID()).iterator().next();
                                        return si.verify(new JcaSimpleSignerInfoVerifierBuilder().build(x509));
                                    }))
                                )
                                .map(v -> v.fold(true, (a, b) -> a && b))
                                .getOrElse(false);
                            }))
                        )
                        .map(v -> v.fold(true, (a, b) -> a && b))
                        .getOrElse(false)
                    )
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
                    () -> PDDocument.load(document.toByteArray())
                )
                .of(pdf -> {
                    PdfSignature pdfSignature = new PdfSignature(certifiedKeyPair.getPrivateKey(), List.of(certifiedKeyPair.getX509Certificate()));

                    PDSignature pdSignature = new PDSignature();
                    pdSignature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
                    pdSignature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
                    pdSignature.setName(certifiedKeyPair.getX509Certificate().getIssuerX500Principal().getName());
                    pdSignature.setReason(description);
                    pdSignature.setSignDate(Calendar.getInstance());

                    SignatureOptions signatureOptions = new SignatureOptions();
                    signatureOptions.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);
                    pdf.addSignature(pdSignature, pdfSignature, signatureOptions);

                    document.reset();
                    pdf.saveIncremental(document);
                    return document;
                })
            )
            .map(document -> new ByteArrayResource(document.toByteArray()));
    }

    @RequiredArgsConstructor
    private static class PdfSignature implements SignatureInterface {
        private final PrivateKey privateKey;
        private final List<X509Certificate> certificateChain;

        @Override
        public byte[] sign(InputStream content) throws IOException {
            return Try.of(CMSSignedDataGenerator::new)
                .flatMap(gen -> Try.of(() -> {
                    gen.addCertificates(new JcaCertStore(certificateChain.toJavaList()));
                    gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder().build()).build(
                            new JcaContentSignerBuilder("SHA256WithRSA").build(privateKey),
                            certificateChain.get(0)
                        )
                    );
                    return gen;
                }))
                .flatMap(gen -> Try.of(() -> gen.generate(
                    new CMSProcessableByteArray(new ASN1ObjectIdentifier(CMSObjectIdentifiers.data.getId()), content.readAllBytes()),
                    false
                )))
                .flatMap(sd -> Try.of(sd::getEncoded))
                .getOrElseThrow((Function<Throwable, IOException>) IOException::new);
        }
    }
}
