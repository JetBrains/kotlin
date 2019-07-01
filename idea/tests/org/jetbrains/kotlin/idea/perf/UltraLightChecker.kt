/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.elements.KtLightNullabilityAnnotation
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.load.kotlin.NON_EXISTENT_CLASS_NAME
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.junit.Assert

fun UsefulTestCase.forceUsingOldLightClassesForTest() {
    KtUltraLightSupport.forceUsingOldLightClasses = true
    Disposer.register(testRootDisposable, Disposable {
        KtUltraLightSupport.forceUsingOldLightClasses = false
    })
}

object UltraLightChecker {
    fun checkClassEquivalence(file: KtFile) {
        for (ktClass in allClasses(file)) {
            checkClassEquivalence(ktClass)
        }
    }

    fun checkForReleaseCoroutine(sourceFileText: String, module: Module) {
        if (sourceFileText.contains("//RELEASE_COROUTINE_NEEDED")) {
            TestCase.assertTrue(
                "Test should be runned under language version that supports released coroutines",
                module.languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines)
            )
        }
    }

    fun allClasses(file: KtFile): List<KtClassOrObject> =
        SyntaxTraverser.psiTraverser(file).filter(KtClassOrObject::class.java).toList()


    fun checkFacadeEquivalence(
        fqName: FqName,
        searchScope: GlobalSearchScope,
        project: Project
    ): KtLightClassForFacade? {

        val oldForceFlag = KtUltraLightSupport.forceUsingOldLightClasses
        KtUltraLightSupport.forceUsingOldLightClasses = true
        val gold = KtLightClassForFacade.createForFacadeNoCache(fqName, searchScope, project)
        KtUltraLightSupport.forceUsingOldLightClasses = false
        val ultraLightClass = KtLightClassForFacade.createForFacadeNoCache(fqName, searchScope, project) ?: return null
        KtUltraLightSupport.forceUsingOldLightClasses = oldForceFlag

        checkClassEquivalenceByRendering(gold, ultraLightClass)

        return ultraLightClass
    }

    fun checkClassEquivalence(ktClass: KtClassOrObject): KtUltraLightClass? {
        val gold = KtLightClassForSourceDeclaration.createNoCache(ktClass, forceUsingOldLightClasses = true)
        val ultraLightClass = LightClassGenerationSupport.getInstance(ktClass.project).createUltraLightClass(ktClass) ?: return null

        checkClassEquivalenceByRendering(gold, ultraLightClass)

        return ultraLightClass
    }

    private fun checkClassEquivalenceByRendering(gold: PsiClass?, ultraLightClass: PsiClass) {
        if (gold != null) {
            Assert.assertFalse(gold.javaClass.name.contains("Ultra"))
        }

        val goldText = gold?.renderClass().orEmpty()
        val ultraText = ultraLightClass.renderClass()

        if (goldText != ultraText) {
            Assert.assertEquals(
                "// Classic implementation:\n$goldText",
                "// Ultra-light implementation:\n$ultraText"
            )
        }
    }

    private fun PsiAnnotation.renderAnnotation() =
        "@" + qualifiedName + "(" + parameterList.attributes.joinToString { it.name + "=" + (it.value?.text ?: "?") } + ")"

    private fun PsiModifierListOwner.renderModifiers(typeIfApplicable: PsiType? = null): String {
        val buffer = StringBuilder()
        for (annotation in annotations) {
            if (annotation is KtLightNullabilityAnnotation<*> && skipRenderingNullability(typeIfApplicable)) {
                continue
            }

            buffer.append(annotation.renderAnnotation())
            buffer.append(if (this is PsiParameter) " " else "\n")
        }
        for (modifier in PsiModifier.MODIFIERS.filter(::hasModifierProperty)) {
            buffer.append(modifier).append(" ")
        }
        return buffer.toString()
    }

    private fun PsiModifierListOwner.skipRenderingNullability(typeIfApplicable: PsiType?) =
        isPrimitiveOrNonExisting(typeIfApplicable) || isPrivateOrParameterInPrivateMethod()

    private val NON_EXISTENT_QUALIFIED_CLASS_NAME = NON_EXISTENT_CLASS_NAME.replace("/", ".")

    private fun isPrimitiveOrNonExisting(typeIfApplicable: PsiType?): Boolean {
        if (typeIfApplicable is PsiPrimitiveType) return true
        if (typeIfApplicable?.getCanonicalText(false) == NON_EXISTENT_QUALIFIED_CLASS_NAME) return true

        return typeIfApplicable is PsiPrimitiveType
    }

    private fun PsiType.renderType() = getCanonicalText(true)

    private fun PsiReferenceList?.renderRefList(keyword: String): String {
        if (this == null || this.referencedTypes.isEmpty()) return ""
        return " " + keyword + " " + referencedTypes.joinToString { it.renderType() }
    }

    private fun PsiVariable.renderVar(): String {
        var result = this.renderModifiers(type) + type.renderType() + " " + name
        if (this is PsiParameter && this.isVarArgs) {
            result += " /* vararg */"
        }
        computeConstantValue()?.let { result += " /* constant value $it */" }
        return result
    }

    private fun PsiTypeParameterListOwner.renderTypeParams() =
        if (typeParameters.isEmpty()) ""
        else "<" + typeParameters.joinToString {
            val bounds =
                if (it.extendsListTypes.isNotEmpty())
                    " extends " + it.extendsListTypes.joinToString(" & ", transform = { it.renderType() })
                else ""
            it.name!! + bounds
        } + "> "

    private fun PsiMethod.renderMethod() =
        renderModifiers(returnType) +
                (if (isVarArgs) "/* vararg */ " else "") +
                renderTypeParams() +
                (returnType?.renderType() ?: "") + " " +
                name +
                "(" + parameterList.parameters.joinToString { it.renderModifiers(it.type) + it.type.renderType() } + ")" +
                (this as? PsiAnnotationMethod)?.defaultValue?.let { " default " + it.text }.orEmpty() +
                throwsList.referencedTypes.let { thrownTypes ->
                    if (thrownTypes.isEmpty()) ""
                    else " throws " + thrownTypes.joinToString { it.renderType() }
                } +
                ";"

    private fun PsiEnumConstant.renderEnumConstant(): String {
        val initializingClass = initializingClass ?: return name

        return buildString {
            appendln("$name {")
            append(initializingClass.renderMembers())
            append("}")
        }
    }

    fun PsiClass.renderClass(): String {
        val classWord = when {
            isAnnotationType -> "@interface"
            isInterface -> "interface"
            isEnum -> "enum"
            else -> "class"
        }

        return buildString {
            append(renderModifiers())
            append("$classWord ")
            append("$name /* $qualifiedName*/")
            append(renderTypeParams())
            append(extendsList.renderRefList("extends"))
            append(implementsList.renderRefList("implements"))
            appendln(" {")

            if (isEnum) {
                append(
                    fields
                        .filterIsInstance<PsiEnumConstant>()
                        .joinToString(",\n") { it.renderEnumConstant() }.prependDefaultIndent()
                )
                append(";\n\n")
            }

            append(renderMembers())
            append("}")
        }
    }

    private fun PsiClass.renderMembers(): String {
        return buildString {
            appendSorted(
                fields
                    .filterNot { it is PsiEnumConstant }
                    .map { it.renderVar().prependDefaultIndent() + ";\n\n" }
            )

            appendSorted(
                methods
                    .map { it.renderMethod().prependDefaultIndent() + "\n\n" }
            )

            appendSorted(
                innerClasses.map { "class ${it.name} ...\n\n".prependDefaultIndent() }
            )
        }
    }

    private fun StringBuilder.appendSorted(list: List<String>) {
        append(list.sorted().joinToString(""))
    }

    private fun String.prependDefaultIndent() = prependIndent("  ")
}
