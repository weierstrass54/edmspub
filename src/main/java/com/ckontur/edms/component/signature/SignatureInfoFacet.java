package com.ckontur.edms.component.signature;

import org.apache.poi.poifs.crypt.dsig.SignatureInfo;
import org.apache.poi.poifs.crypt.dsig.facets.OOXMLSignatureFacet;
import org.w3c.dom.Document;

import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignatureException;
import java.util.ArrayList;
import java.util.List;

public class SignatureInfoFacet extends OOXMLSignatureFacet {
    private static final String ID_PACKAGE_OBJECT = "idPackageObject";

    @Override
    public void preSign(SignatureInfo signatureInfo, Document document, List<Reference> references, List<XMLObject> objects) throws XMLSignatureException {
        List<XMLStructure> objectContent = new ArrayList<>();
        addSignatureTime(signatureInfo, document, objectContent);
        XMLObject xo = signatureInfo.getSignatureFactory().newXMLObject(objectContent, ID_PACKAGE_OBJECT, null, null);
        objects.add(xo);
        addSignatureInfo(signatureInfo, document, references, objects);
    }
}
