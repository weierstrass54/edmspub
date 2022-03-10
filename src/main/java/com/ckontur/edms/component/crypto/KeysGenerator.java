package com.ckontur.edms.component.crypto;

import com.ckontur.edms.model.CertifiedKeyPair;
import com.ckontur.edms.utils.DateUtils;
import io.vavr.control.Try;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Date;

@Component
public class KeysGenerator {
    public Try<CertifiedKeyPair> generate(String name) {
        return generateRSAKeyPair()
            .flatMap(keyPair -> generateX509(keyPair, name)
                .map(x509 -> new CertifiedKeyPair(keyPair.getPrivate(), keyPair.getPublic(), x509))
            );
    }

    private Try<KeyPair> generateRSAKeyPair() {
        return Try.of(() -> KeyPairGenerator.getInstance("RSA"))
            .peek(kpg -> kpg.initialize(2048))
            .map(KeyPairGenerator::generateKeyPair);
    }

    private Try<X509Certificate> generateX509(KeyPair keyPair, String name) {
        X500Name dnName = new X500Name("dc=" + name);
        BigInteger certSerialNumber = new BigInteger(Long.toString(System.currentTimeMillis()));
        Date start = DateUtils.of(LocalDateTime.now());
        Date end = DateUtils.of(LocalDateTime.now().plusYears(1));

        return Try.of(() -> new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate()))
            .flatMap(signer -> Try.of(() -> {
                JcaX509v3CertificateBuilder certBuilder =
                    new JcaX509v3CertificateBuilder(dnName, certSerialNumber, start, end, dnName, keyPair.getPublic());
                certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
                return certBuilder.build(signer);
            }))
            .flatMap(certHolder -> Try.of(() ->
                new JcaX509CertificateConverter().setProvider(new BouncyCastleProvider()).getCertificate(certHolder)
            ));
    }
}
