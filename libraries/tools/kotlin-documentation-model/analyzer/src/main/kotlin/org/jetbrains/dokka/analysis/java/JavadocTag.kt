/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java

import org.jetbrains.dokka.InternalDokkaApi

@InternalDokkaApi
internal sealed class JavadocTag(
    val name: String
)

internal object AuthorJavadocTag : JavadocTag("author")
internal object DeprecatedJavadocTag : JavadocTag("deprecated")
internal object DescriptionJavadocTag : JavadocTag("description")
internal object ReturnJavadocTag : JavadocTag("return")
internal object SinceJavadocTag : JavadocTag("since")

internal class ParamJavadocTag(
    val paramName: String,
    val paramIndex: Int
) : JavadocTag(name) {
    companion object {
        const val name: String = "param"
    }
}

internal class SeeJavadocTag(
    val qualifiedReference: String
) : JavadocTag(name) {
    companion object {
        const val name: String = "see"
    }
}

internal sealed class ThrowingExceptionJavadocTag(
    name: String,
    val exceptionQualifiedName: String?
) : JavadocTag(name)

internal class ThrowsJavadocTag(exceptionQualifiedName: String?) : ThrowingExceptionJavadocTag(name, exceptionQualifiedName) {
    companion object {
        const val name: String = "throws"
    }
}

internal class ExceptionJavadocTag(exceptionQualifiedName: String?) : ThrowingExceptionJavadocTag(name, exceptionQualifiedName) {
    companion object {
        const val name: String = "exception"
    }
}
