package org.jetbrains.dokka.analysis.java

import com.intellij.psi.PsiMethod
import org.jetbrains.dokka.InternalDokkaApi

@InternalDokkaApi
sealed class JavadocTag(val name: String)

object AuthorJavadocTag : JavadocTag("author")
object DeprecatedJavadocTag : JavadocTag("deprecated")
object DescriptionJavadocTag : JavadocTag("description")
object ReturnJavadocTag : JavadocTag("return")
object SinceJavadocTag : JavadocTag("since")

class ParamJavadocTag(
    val method: PsiMethod,
    val paramName: String,
    val paramIndex: Int
) : JavadocTag(name) {
    companion object {
        const val name: String = "param"
    }
}

class SeeJavadocTag(
    val qualifiedReference: String
) : JavadocTag(name) {
    companion object {
        const val name: String = "see"
    }
}

sealed class ThrowingExceptionJavadocTag(
    name: String,
    val exceptionQualifiedName: String?
) : JavadocTag(name)

class ThrowsJavadocTag(exceptionQualifiedName: String?) : ThrowingExceptionJavadocTag(name, exceptionQualifiedName) {
    companion object {
        const val name: String = "throws"
    }
}

class ExceptionJavadocTag(exceptionQualifiedName: String?) : ThrowingExceptionJavadocTag(name, exceptionQualifiedName) {
    companion object {
        const val name: String = "exception"
    }
}
