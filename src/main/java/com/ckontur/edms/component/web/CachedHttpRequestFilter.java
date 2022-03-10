package com.ckontur.edms.component.web;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

//@Component
public class CachedHttpRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        CachedHttpServletRequest cachedRequest = new CachedHttpServletRequest(httpServletRequest);
        chain.doFilter(cachedRequest, response);
    }
}
