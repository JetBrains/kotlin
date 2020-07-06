/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.MethodSignature
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.PairProcessor
import com.intellij.util.ref.DebugReflectionUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.asJava.elements.KtLightNullabilityAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightPsiArrayInitializerMemberValue
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.load.kotlin.NON_EXISTENT_CLASS_NAME
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScript
import org.junit.Assert
import kotlin.test.assertFails

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

        val secondULInstance = LightClassGenerationSupport.getInstance(ktClass.project).createUltraLightClass(ktClass)
        Assert.assertNotNull(secondULInstance)
        Assert.assertTrue(ultraLightClass !== secondULInstance)
        secondULInstance!!
        Assert.assertEquals(ultraLightClass.ownMethods.size, secondULInstance.ownMethods.size)
        Assert.assertTrue(ultraLightClass.ownMethods.containsAll(secondULInstance.ownMethods))

        checkClassEquivalenceByRendering(gold, ultraLightClass)

        return ultraLightClass
    }

    fun checkScriptEquivalence(script: KtScript): KtLightClass {

        val ultraLightScript: KtLightClass?

        val oldForceFlag = KtUltraLightSupport.forceUsingOldLightClasses
        try {
            KtUltraLightSupport.forceUsingOldLightClasses = false
            ultraLightScript = KotlinAsJavaSupport.getInstance(script.project).getLightClassForScript(script)
            TestCase.assertTrue(ultraLightScript is KtUltraLightClassForScript)
            ultraLightScript!!
            val gold = KtLightClassForScript.createNoCache(script, forceUsingOldLightClasses = true)
            checkClassEquivalenceByRendering(gold, ultraLightScript)
        } finally {
            KtUltraLightSupport.forceUsingOldLightClasses = oldForceFlag
        }

        return ultraLightScript!!
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

    private fun PsiAnnotation.renderAnnotation(): String {

        val renderedAttributes = parameterList.attributes.map {
            val attributeValue = it.value?.renderAnnotationMemberValue() ?: "?"

            val name = when {
                it.name === null && qualifiedName?.startsWith("java.lang.annotation.") == true -> "value"
                else -> it.name
            }

            if (name !== null) "$name = $attributeValue" else attributeValue
        }
        return "@$qualifiedName(${renderedAttributes.joinToString()})"
    }


    private fun PsiModifierListOwner.renderModifiers(typeIfApplicable: PsiType? = null): String {
        val annotationsBuffer = mutableListOf<String>()
        for (annotation in annotations) {
            if (annotation is KtLightNullabilityAnnotation<*> && skipRenderingNullability(typeIfApplicable)) {
                continue
            }

            annotationsBuffer.add(
                annotation.renderAnnotation() + (if (this is PsiParameter) " " else "\n")
            )
        }
        annotationsBuffer.sort()

        val resultBuffer = StringBuffer(annotationsBuffer.joinToString(separator = ""))
        for (modifier in PsiModifier.MODIFIERS.filter(::hasModifierProperty)) {
            resultBuffer.append(modifier).append(" ")
        }
        return resultBuffer.toString()
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

    private fun PsiReferenceList?.renderRefList(keyword: String, sortReferences: Boolean = true): String {
        if (this == null) return ""

        val references = referencedTypes
        if (references.isEmpty()) return ""

        val referencesTypes = references.map { it.renderType() }.toTypedArray()

        if (sortReferences) referencesTypes.sort()

        return " " + keyword + " " + referencesTypes.joinToString()
    }

    private fun PsiVariable.renderVar(): String {
        var result = this.renderModifiers(type) + type.renderType() + " " + name
        if (this is PsiParameter && this.isVarArgs) {
            result += " /* vararg */"
        }

        if (hasInitializer()) {
            result += " = ${initializer?.text} /* initializer type: ${initializer?.type?.renderType()} */"
        }

        computeConstantValue()?.let { result += " /* constant value $it */" }

        return result
    }

    private fun Array<PsiTypeParameter>.renderTypeParams() =
        if (isEmpty()) ""
        else "<" + joinToString {
            val bounds =
                if (it.extendsListTypes.isNotEmpty())
                    " extends " + it.extendsListTypes.joinToString(" & ", transform = { it.renderType() })
                else ""
            it.name!! + bounds
        } + "> "

    private fun PsiAnnotationMemberValue.renderAnnotationMemberValue(): String = when (this) {
        is KtLightPsiArrayInitializerMemberValue -> "{${initializers.joinToString { it.renderAnnotationMemberValue() }}}"
        is PsiAnnotation -> renderAnnotation()
        else -> text
    }

    private fun PsiMethod.renderMethod() =
        renderModifiers(returnType) +
                (if (isVarArgs) "/* vararg */ " else "") +
                typeParameters.renderTypeParams() +
                (returnType?.renderType() ?: "") + " " +
                name +
                "(" + parameterList.parameters.joinToString { it.renderModifiers(it.type) + it.type.renderType() } + ")" +
                (this as? PsiAnnotationMethod)?.defaultValue?.let { " default " + it.renderAnnotationMemberValue() }.orEmpty() +
                throwsList.referencedTypes.let { thrownTypes ->
                    if (thrownTypes.isEmpty()) ""
                    else " throws " + thrownTypes.joinToString { it.renderType() }
                } +
                ";" +
                "// ${getSignature(PsiSubstitutor.EMPTY).renderSignature()}"

    private fun MethodSignature.renderSignature(): String {
        val typeParams = typeParameters.renderTypeParams()
        val paramTypes = parameterTypes.joinToString(prefix = "(", postfix = ")") { it.renderType() }
        val name = if (isConstructor) ".ctor" else name
        return "$typeParams $name$paramTypes"
    }

    private fun PsiEnumConstant.renderEnumConstant(): String {
        val initializingClass = initializingClass ?: return name

        return buildString {
            appendLine("$name {")
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
            append(typeParameters.renderTypeParams())
            append(extendsList.renderRefList("extends"))
            append(implementsList.renderRefList("implements"))
            appendLine(" {")

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

    private fun checkDescriptorLeakOnElement(element: PsiElement) {
        DebugReflectionUtil.walkObjects(
            10,
            mapOf(element to element.javaClass.name),
            Any::class.java,
            { true },
            PairProcessor { value, backLink ->
                if (value is DeclarationDescriptor) {
                    assertFails {
                        """Leaked descriptor ${value.javaClass.name} in ${element.javaClass.name}\n$backLink"""
                    }
                }
                true
            })
    }

    fun checkDescriptorsLeak(lightClass: KtLightClass) {
        checkDescriptorLeakOnElement(lightClass)
        lightClass.methods.forEach {
            checkDescriptorLeakOnElement(it)
            it.parameterList.parameters.forEach { parameter -> checkDescriptorLeakOnElement(parameter) }
        }
        lightClass.fields.forEach { checkDescriptorLeakOnElement(it) }
    }
}