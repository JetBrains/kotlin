/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka


/**
 * Marks declarations that are **internal** to Dokka core artifact.
 * It means that this API is marked as **public** either for historical or technical reasons.
 * It is not intended to be used outside of the Dokka project, has no behaviour guarantees,
 * and may lack clear semantics, documentation and backward compatibility.
 *
 * If you are using such API, it is strongly suggested to migrate from it in order
 * to keep backwards compatibility with future Dokka versions.
 * Typically, the easiest way to do so is to copy-paste the corresponding utility into
 * your own project.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal Dokka API not intended for public use"
)
@Target(
    AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD,
    AnnotationTarget.PROPERTY, AnnotationTarget.TYPEALIAS
)
@Retention(AnnotationRetention.BINARY)
public annotation class InternalDokkaApi()
