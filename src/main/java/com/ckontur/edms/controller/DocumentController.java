package com.ckontur.edms.controller;

import com.ckontur.edms.exception.InvalidArgumentException;
import com.ckontur.edms.exception.InvalidDocumentException;
import com.ckontur.edms.exception.NotFoundException;
import com.ckontur.edms.model.Document;
import com.ckontur.edms.model.User;
import com.ckontur.edms.service.DocumentService;
import com.ckontur.edms.web.SignRequest;
import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.vavr.collection.List;
import io.vavr.collection.Set;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;

@Api(tags = {"Документы"})
@RequestMapping("/docs")
@RestController
@RequiredArgsConstructor
@Timed(value = "requests.docs", percentiles = {0.75, 0.9, 0.95, 0.99})
public class DocumentController {
    private final DocumentService documentService;

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW')")
    public ResponseEntity<Resource> downloadById(HttpServletRequest request, @PathVariable("id") Long id) {
        return documentService.findById(id)
            .map(resource -> {
                String contentType = Try.of(() -> request.getServletContext().getMimeType(resource.getFile().getAbsolutePath()))
                    .getOrElse(DocumentService.DEFAULT_CONTENT_TYPE);
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
            })
            .getOrElseThrow(() -> new NotFoundException("Документ " + id + " не найден."));
    }

    @PostMapping("/upload/signers")
    @PreAuthorize("hasAuthority('UPLOAD')")
    public List<Document> upload(
        @AuthenticationPrincipal Authentication principal,
        @RequestParam("files") MultipartFile[] files,
        @RequestParam(value = "signers") Set<Long> userIds
    ) {
        return documentService.uploadDocuments((User) principal.getPrincipal(), files, userIds.toList())
            .getOrElseThrow((t) -> Match(t).of(
                Case($(instanceOf(InvalidArgumentException.class)), __ -> __),
                Case($(), __ -> new InvalidDocumentException(t.getMessage(), t))
            ));
    }

    @PostMapping("/upload/template")
    @PreAuthorize("hasAuthority('UPLOAD')")
    public List<Document> upload(
        @AuthenticationPrincipal Authentication principal,
        @RequestParam("files") MultipartFile[] files,
        @RequestParam(value = "sign_template") Long signTemplateId
    ) {
        return documentService.uploadDocuments((User) principal.getPrincipal(), files, signTemplateId)
            .getOrElseThrow(() -> new NotFoundException("Шаблон маршрута подписей " + signTemplateId + " не найден."))
            .getOrElseThrow((t) -> Match(t).of(
                Case($(instanceOf(InvalidArgumentException.class)), __ -> __),
                Case($(), __ -> new InvalidDocumentException(t.getMessage(), t))
            ));
    }

    @PostMapping("/{id}/sign")
    @PreAuthorize("hasAuthority('SIGN')")
    public Document sign(
        @AuthenticationPrincipal Authentication principal,
        @PathVariable("id") Long id,
        @RequestBody @Valid SignRequest signRequest
    ) {
        return documentService.signById((User) principal.getPrincipal(), id, signRequest.getNotes())
            .map(document -> document.getOrElseThrow(InvalidDocumentException::new))
            .getOrElseThrow(() -> new NotFoundException("Документ " + id + " не найден."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE')")
    public Document delete(@PathVariable("id") Long id) {
        return documentService.deleteById(id)
            .getOrElseThrow(() -> new NotFoundException("Документ " + id + " не найден."))
            .getOrElseThrow((t) -> new InvalidDocumentException(t.getMessage(), t));
    }
}
