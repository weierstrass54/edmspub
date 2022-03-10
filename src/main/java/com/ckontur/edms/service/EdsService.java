package com.ckontur.edms.service;

import com.ckontur.edms.component.crypto.KeysGenerator;
import com.ckontur.edms.model.CertifiedKeyPair;
import com.ckontur.edms.model.User;
import com.ckontur.edms.repository.CertifiedKeyPairRepository;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EdsService {
    private final KeysGenerator keysGenerator;
    private final CertifiedKeyPairRepository certifiedKeyPairRepository;

    public Try<CertifiedKeyPair> getOrGenerate(User user) {
        return certifiedKeyPairRepository.findByUserId(user.getId())
            .map(Try::success)
            .getOrElseTry(() ->
                keysGenerator.generate(user.getLogin())
                    .flatMap(ckp -> certifiedKeyPairRepository.create(user.getId(), ckp))
            );
    }
}
