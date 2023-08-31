/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java

import com.intellij.psi.PsiMethod
import org.jetbrains.dokka.InternalDokkaApi

@InternalDokkaApi
public sealed class JavadocTag(
    public val name: String
)

public object AuthorJavadocTag : JavadocTag("author")
public object DeprecatedJavadocTag : JavadocTag("deprecated")
public object DescriptionJavadocTag : JavadocTag("description")
public object ReturnJavadocTag : JavadocTag("return")
public object SinceJavadocTag : JavadocTag("since")

public class ParamJavadocTag(
    public val method: PsiMethod,
    public val paramName: String,
    public val paramIndex: Int
) : JavadocTag(name) {
    public companion object {
        public const val name: String = "param"
    }
}

public class SeeJavadocTag(
    public val qualifiedReference: String
) : JavadocTag(name) {
    public companion object {
        public const val name: String = "see"
    }
}

public sealed class ThrowingExceptionJavadocTag(
    name: String,
    public val exceptionQualifiedName: String?
) : JavadocTag(name)

public class ThrowsJavadocTag(exceptionQualifiedName: String?) : ThrowingExceptionJavadocTag(name, exceptionQualifiedName) {
    public companion object {
        public const val name: String = "throws"
    }
}

public class ExceptionJavadocTag(exceptionQualifiedName: String?) : ThrowingExceptionJavadocTag(name, exceptionQualifiedName) {
    public companion object {
        public const val name: String = "exception"
    }
}
