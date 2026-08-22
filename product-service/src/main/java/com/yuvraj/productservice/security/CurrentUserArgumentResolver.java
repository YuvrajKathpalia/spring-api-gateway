package com.yuvraj.productservice.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * Resolves an {@link AuthenticatedUser} for any controller parameter annotated with
 * {@link CurrentUser}, reading the identity headers the gateway injected after it
 * verified the JWT:
 * <ul>
 *   <li>{@code X-User-Id}</li>
 *   <li>{@code X-User-Email}</li>
 *   <li>{@code X-User-Roles}</li>
 * </ul>
 *
 * <p>It also logs {@code Current User} / {@code Current Role} on every call — a simple,
 * visible proof that header propagation from the gateway is working end to end.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final Logger log = LoggerFactory.getLogger(CurrentUserArgumentResolver.class);

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USER_EMAIL = "X-User-Email";
    private static final String HEADER_USER_ROLES = "X-User-Roles";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(AuthenticatedUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        String userIdHeader = webRequest.getHeader(HEADER_USER_ID);
        if (userIdHeader == null || userIdHeader.isBlank()) {
            // Should never happen when reached through the gateway; guards direct/misrouted calls.
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Missing " + HEADER_USER_ID + " header — request did not pass through the gateway");
        }

        String email = webRequest.getHeader(HEADER_USER_EMAIL);
        String roles = webRequest.getHeader(HEADER_USER_ROLES);

        // Visible proof that identity propagated from the gateway.
        log.info("Current User : {}", userIdHeader);
        log.info("Current Role : {}", roles);

        return new AuthenticatedUser(Long.valueOf(userIdHeader), email, roles);
    }
}
