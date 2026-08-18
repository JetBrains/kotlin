/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.SyntheticElement
import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.parsers.JavadocParser
import org.jetbrains.dokka.model.doc.DocumentationNode

private const val ENUM_VALUEOF_TEMPLATE_PATH = "/dokka/docs/javadoc/EnumValueOf.java.template"
private const val ENUM_VALUES_TEMPLATE_PATH = "/dokka/docs/javadoc/EnumValues.java.template"

@InternalDokkaApi
public class SyntheticElementDocumentationProvider(
    private val javadocParser: JavadocParser,
    private val project: Project
) {
    public fun isDocumented(psiElement: PsiElement): Boolean = psiElement is PsiMethod
            && (psiElement.isSyntheticEnumValuesMethod() || psiElement.isSyntheticEnumValueOfMethod())

    public fun getDocumentation(psiElement: PsiElement, sourceSet: DokkaSourceSet): DocumentationNode? {
        val psiMethod = psiElement as? PsiMethod ?: return null
        return when {
            psiMethod.isSyntheticEnumValuesMethod() -> getDocumentationForEnumValuesMethod(sourceSet)
            psiMethod.isSyntheticEnumValueOfMethod() -> getDocumentationForEnumValueOfMethod(sourceSet)
            else ->  null
        }
    }

    public fun getDocumentationForEnumValuesMethod(sourceSet: DokkaSourceSet): DocumentationNode? {
        val someSyntheticMethod = loadSyntheticDoc(ENUM_VALUES_TEMPLATE_PATH) ?: return null
        return javadocParser.parseDocumentation(someSyntheticMethod, sourceSet)
    }

    public fun getDocumentationForEnumValueOfMethod(sourceSet: DokkaSourceSet): DocumentationNode? {
        val someSyntheticMethod = loadSyntheticDoc(ENUM_VALUEOF_TEMPLATE_PATH) ?: return null
        return javadocParser.parseDocumentation(someSyntheticMethod, sourceSet)
    }

    private fun loadSyntheticDoc(path: String): PsiMethod? {
        val text = javaClass.getResource(path)?.readText() ?: return null
        return JavaPsiFacade.getElementFactory(project).createMethodFromText(text.trim() + "void m();", null);
    }
}

private fun PsiMethod.isSyntheticEnumValuesMethod() = this.isSyntheticEnumFunction() && this.name == "values"
private fun PsiMethod.isSyntheticEnumValueOfMethod() = this.isSyntheticEnumFunction() && this.name == "valueOf"
private fun PsiMethod.isSyntheticEnumFunction() = this is SyntheticElement && this.containingClass?.isEnum == true

