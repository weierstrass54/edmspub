package com.ckontur.edms.component.web;

import com.ckontur.edms.exception.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.vavr.control.Option;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.stream.Collectors;

import static io.vavr.API.*;
import static io.vavr.Predicates.instanceOf;

@Slf4j
@RestControllerAdvice
public class WebErrorHandler {

    @Value("${logging.web.log-404-errors:false}")
    protected boolean log404Errors;

    @Value("${logging.web.stacktrace-on-4xx-errors:false}")
    protected boolean withStacktraceOn4xxErrors;

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public ErrorMessage any(CachedHttpServletRequest request, Throwable t) {
        ErrorMessage errorMessage = getErrorMessage(request, t, true);
        log.error("{}", errorMessage, t);
        return errorMessage;
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorMessage httpNotFound(CachedHttpServletRequest request, NoHandlerFoundException e) {
        ErrorMessage errorMessage = getErrorMessage(request, e, withStacktraceOn4xxErrors);
        if (log404Errors) {
            log.error("{}", errorMessage);
        }
        return errorMessage;
    }

    @ExceptionHandler({
        InvalidEnumException.class,
        InvalidArgumentException.class,
        MissingPathVariableException.class,
        MissingRequestCookieException.class,
        MissingRequestHeaderException.class,
        MissingMatrixVariableException.class,
        ServletRequestBindingException.class,
        MethodArgumentNotValidException.class,
        MissingServletRequestParameterException.class,
        UnsatisfiedServletRequestParameterException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorMessage httpBadRequest(CachedHttpServletRequest request, Throwable e) {
        return defaultClientException(request, e);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorMessage httpNotFound(CachedHttpServletRequest request, NotFoundException e) {
        return defaultClientException(request, e);
    }

    @ExceptionHandler({
        AccessDeniedException.class,
        InvalidDocumentException.class,
        AuthenticationFailedException.class
    })
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public ErrorMessage httpForbidden(CachedHttpServletRequest request, Throwable e) {
        return defaultClientException(request, e);
    }

    protected ErrorMessage defaultClientException(CachedHttpServletRequest request, Throwable t) {
        ErrorMessage errorMessage = getErrorMessage(request, t, withStacktraceOn4xxErrors);
        log.error("{}. Stacktrace: {}", errorMessage, errorMessage.getStacktrace());
        return errorMessage;
    }

    protected ErrorMessage getErrorMessage(CachedHttpServletRequest request, Throwable t, boolean withStacktrace) {
        String method = request.getMethod();
        String query = Option.of(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI))
            .map(Object::toString)
            .getOrElse(request::getRequestURI) + getQueryString(request);
        String body = request.getBodyAsString();
        String errorMessage = Option.of(t).map(th -> Match(th).of(
            Case($(instanceOf(MethodArgumentNotValidException.class)), manve ->
                manve.getBindingResult().getAllErrors().stream().map(ObjectError::getDefaultMessage).collect(Collectors.joining(", "))),
            Case($(instanceOf(NullPointerException.class)), __ -> "NPE."),
            Case($(), Throwable::getMessage)
        )).getOrElse("");
        String[] stacktrace = withStacktrace ? getStacktrace(t) : new String[0];

        return new ErrorMessage(method, query, body, errorMessage, stacktrace);
    }

    private String getQueryString(HttpServletRequest request) {
        return Option.of(request.getQueryString()).map(qs -> "?" + qs).getOrElse("");
    }

    private String[] getStacktrace(Throwable e) {
        return Arrays.stream(
            Option.of(e).map(Throwable::getStackTrace).getOrElse(() -> Thread.currentThread().getStackTrace())
        ).map(StackTraceElement::toString).toArray(String[]::new);
    }

    @Getter
    @RequiredArgsConstructor
    protected static class ErrorMessage {
        private final String method;
        private final String query;
        private final String body;
        private final String errorMessage;

        @JsonIgnore
        private final String[] stacktrace;

        @Override
        public String toString() {
            return "ErrorMessage{" +
                "method='" + method + '\'' +
                ", query='" + query + '\'' +
                ", body=" + body +
                ", errorMessage='" + errorMessage + '\'' +
                ", stacktrace=" + Arrays.toString(stacktrace) +
                '}';
        }
    }

}
