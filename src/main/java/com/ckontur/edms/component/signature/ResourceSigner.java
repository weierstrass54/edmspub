package com.ckontur.edms.component.signature;

import com.ckontur.edms.model.CertifiedKeyPair;
import io.vavr.control.Try;
import org.springframework.core.io.Resource;

public interface ResourceSigner {
    boolean validateSign(Resource resource);

    Try<Resource> sign(Resource resource, CertifiedKeyPair certifiedKeyPair, String description);
}
