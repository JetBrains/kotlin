package org.checkerframework.checker.nullness.compatqual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Java 7 compatibility annotation without dependency on Java 8 classes.
 *
 * @see org.checkerframework.checker.nullness.qual.NonNull
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface NonNullDecl {}
