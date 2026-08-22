package com.yuvraj.productservice.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the caller's identity (resolved by {@link CurrentUserArgumentResolver}
 * from the gateway-provided headers) into a controller method parameter.
 *
 * <p>Usage: {@code public ... create(@CurrentUser AuthenticatedUser user)}
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
