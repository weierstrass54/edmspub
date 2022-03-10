package com.ckontur.edms.controller;

import com.ckontur.edms.exception.InvalidArgumentException;
import com.ckontur.edms.exception.NotFoundException;
import com.ckontur.edms.model.CertifiedKeyPair;
import com.ckontur.edms.model.Page;
import com.ckontur.edms.model.User;
import com.ckontur.edms.repository.SearchUserRepository;
import com.ckontur.edms.repository.UserRepository;
import com.ckontur.edms.service.EdsService;
import com.ckontur.edms.web.PageRequest;
import com.ckontur.edms.web.UserRequests;
import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.function.Function;

@Api(tags = {"Пользователи"})
@RequestMapping("/user")
@RestController
@RequiredArgsConstructor
@Timed(value = "requests.user", percentiles = {0.75, 0.9, 0.95, 0.99})
public class UserController {
    private final UserRepository userRepository;
    private final SearchUserRepository searchUserRepository;
    private final EdsService edsService;

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('UPLOAD')")
    public Page<User> findAllBySearchString(
        @RequestParam(value = "search", required = false) String searchString,
        @RequestParam(value = "page", required = false, defaultValue = "1") int page,
        @RequestParam(value = "size", required = false, defaultValue = "50") int size,
        @RequestParam(value = "sort", required = false, defaultValue = "ASC") String sort
    ) {
        return searchUserRepository.findAllBySearchString(searchString, PageRequest.of(page, size, PageRequest.Direction.of(sort)));
    }

    @PostMapping("/eds")
    @PreAuthorize("hasAuthority('SIGN')")
    public CertifiedKeyPair generate(@AuthenticationPrincipal Authentication principal) {
        return edsService.getOrGenerate((User) principal.getPrincipal())
            .getOrElseThrow((Function<Throwable, RuntimeException>) RuntimeException::new);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('VIEW', 'ADMIN')")
    public User findById(@PathVariable("id") Long id) {
        return userRepository.findById(id)
            .getOrElseThrow(() -> new NotFoundException("Пользователь " + id + " не найден."));
    }

    @PostMapping("")
    @PreAuthorize("hasAuthority('ADMIN')")
    public User create(@RequestBody @Valid UserRequests.CreateUser request) {
        return userRepository.create(request)
            .getOrElseThrow(() -> new InvalidArgumentException("Указаны неверные данные пользователя."));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public User updateById(@PathVariable("id") Long id, @RequestBody @Valid UserRequests.UpdateUser request) {
        return userRepository.updateById(id, request)
            .getOrElseThrow(() -> new InvalidArgumentException("Указаны неверные данные пользователя."))
            .getOrElseThrow(() -> new NotFoundException("Пользователь " + id + " не найден."));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public User deleteById(@PathVariable("id") Long id) {
        return userRepository.deleteById(id)
            .getOrElseThrow(() -> new AccessDeniedException("Невозможно удалить пользователя."))
            .getOrElseThrow(() -> new NotFoundException("Пользователь " + id + " не найден."));
    }
}
