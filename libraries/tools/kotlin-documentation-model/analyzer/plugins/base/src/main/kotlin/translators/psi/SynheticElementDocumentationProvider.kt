package org.jetbrains.dokka.base.translators.psi

import com.intellij.psi.*
import com.intellij.psi.javadoc.PsiDocComment
import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.base.translators.psi.parsers.JavadocParser
import org.jetbrains.dokka.model.doc.DocumentationNode

private const val ENUM_VALUEOF_TEMPLATE_PATH = "/dokka/docs/javadoc/EnumValueOf.java.template"
private const val ENUM_VALUES_TEMPLATE_PATH = "/dokka/docs/javadoc/EnumValues.java.template"

internal class SyntheticElementDocumentationProvider(
    private val javadocParser: JavadocParser,
    private val resolutionFacade: DokkaResolutionFacade
) {
    fun isDocumented(psiElement: PsiElement): Boolean = psiElement is PsiMethod
            && (psiElement.isSyntheticEnumValuesMethod() || psiElement.isSyntheticEnumValueOfMethod())

    fun getDocumentation(psiElement: PsiElement): DocumentationNode? {
        val psiMethod = psiElement as? PsiMethod ?: return null
        val templatePath = when {
            psiMethod.isSyntheticEnumValuesMethod() -> ENUM_VALUES_TEMPLATE_PATH
            psiMethod.isSyntheticEnumValueOfMethod() -> ENUM_VALUEOF_TEMPLATE_PATH
            else -> return null
        }
        val docComment = loadSyntheticDoc(templatePath) ?: return null
        return javadocParser.parseDocComment(docComment, psiElement)
    }

    private fun loadSyntheticDoc(path: String): PsiDocComment? {
        val text = javaClass.getResource(path)?.readText() ?: return null
        return JavaPsiFacade.getElementFactory(resolutionFacade.project).createDocCommentFromText(text)
    }
}

private fun PsiMethod.isSyntheticEnumValuesMethod() = this.isSyntheticEnumFunction() && this.name == "values"
private fun PsiMethod.isSyntheticEnumValueOfMethod() = this.isSyntheticEnumFunction() && this.name == "valueOf"
private fun PsiMethod.isSyntheticEnumFunction() = this is SyntheticElement && this.containingClass?.isEnum == true

