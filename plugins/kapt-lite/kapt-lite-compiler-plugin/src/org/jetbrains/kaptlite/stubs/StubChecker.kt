/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite.stubs

import com.intellij.lang.java.lexer.JavaLexer
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiElement
import org.jetbrains.kaptlite.diagnostic.DefaultErrorMessagesKaptLite
import org.jetbrains.kaptlite.diagnostic.ErrorsKaptLite
import org.jetbrains.kaptlite.signature.ClassSignature
import org.jetbrains.kaptlite.signature.SigType
import org.jetbrains.kaptlite.signature.SigTypeArgument
import org.jetbrains.kaptlite.stubs.model.JavaAnnotationStub
import org.jetbrains.kaptlite.stubs.model.JavaClassStub
import org.jetbrains.kaptlite.stubs.model.JavaMethodStub
import org.jetbrains.kaptlite.stubs.util.JavaClassName
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin

class StubChecker(private val state: GenerationState, private val diagnostics: DiagnosticSink) {
    private val languageLevel = when (state.target) {
        JvmTarget.JVM_1_6 -> LanguageLevel.JDK_1_6
        JvmTarget.JVM_1_8 -> LanguageLevel.JDK_1_8
        JvmTarget.JVM_9 -> LanguageLevel.JDK_1_9
        JvmTarget.JVM_10 -> LanguageLevel.JDK_10
        JvmTarget.JVM_11 -> LanguageLevel.JDK_11
        JvmTarget.JVM_12 -> LanguageLevel.JDK_12
    }

    fun checkClass(
        name: JavaClassName, signature: ClassSignature,
        enumValues: List<JavaClassStub.JavaEnumValue>,
        annotations: List<JavaAnnotationStub>,
        origin: JvmDeclarationOrigin
    ): Boolean {
        return checkName(name, origin)
                && checkType(signature.superClass, origin)
                && signature.interfaces.all { checkType(it, origin) }
                && signature.typeParameters.all { checkName(it.name, origin) && it.bounds.all { bound -> checkType(bound, origin) } }
                && enumValues.all { checkName(it.name, origin) }
                && checkAnnotations(annotations, origin)
    }

    fun checkField(name: String, type: SigType, annotations: List<JavaAnnotationStub>, origin: JvmDeclarationOrigin): Boolean {
        // TODO check field initializer
        return checkName(name, origin) && checkType(type, origin) && checkAnnotations(annotations, origin)
    }

    fun checkMethod(
        name: String, data: JavaMethodStub.MethodData,
        annotations: List<JavaAnnotationStub>,
        origin: JvmDeclarationOrigin
    ): Boolean {
        return checkName(name, origin)
                && data.typeParameters.all { checkName(it.name, origin) && it.bounds.all { bound -> checkType(bound, origin) } }
                && data.parameters.all { checkName(it.name, origin) && checkType(it.type, origin) }
                && data.exceptionTypes.all { checkType(it, origin) }
                && checkType(data.returnType, origin)
                && checkAnnotations(annotations, origin)
    }

    private fun checkAnnotations(annotations: List<JavaAnnotationStub>, origin: JvmDeclarationOrigin): Boolean {
        // TODO check annotation args
        return annotations.all { checkName(it.name, origin) }
    }

    private fun checkName(name: JavaClassName, origin: JvmDeclarationOrigin): Boolean {
        return (name.packageName.isEmpty() || name.packageName.split('.').all { checkName(it, origin) })
                && name.className.split('.').all { checkName(it, origin) }
    }

    private fun checkName(name: String, origin: JvmDeclarationOrigin): Boolean {
        if (!isValidIdentifier(name, languageLevel)) {
            report(origin) { ErrorsKaptLite.KAPT_INCOMPATIBLE_NAME.on(it, name) }
            return false
        }

        return true
    }

    private fun checkType(type: SigType, origin: JvmDeclarationOrigin): Boolean {
        return when (type) {
            is SigType.TypeVariable -> checkName(type.name, origin)
            is SigType.Array -> checkType(type.elementType, origin)
            is SigType.Class -> type.fqName.split('.').all { checkName(it, origin) }
            is SigType.Nested -> checkType(type.outer, origin) && checkName(type.name, origin)
            is SigType.Generic -> checkType(type.base, origin) && type.args.all { checkTypeArgument(it, origin) }
            is SigType.Primitive -> true
        }
    }

    private fun checkTypeArgument(arg: SigTypeArgument, origin: JvmDeclarationOrigin): Boolean {
        return when (arg) {
            SigTypeArgument.Unbound -> true
            is SigTypeArgument.Invariant -> checkType(arg.type, origin)
            is SigTypeArgument.Extends -> checkType(arg.type, origin)
            is SigTypeArgument.Super -> checkType(arg.type, origin)
        }
    }

    private fun report(origin: JvmDeclarationOrigin, factory: (PsiElement) -> Diagnostic) {
        val reportElement = findReportElement(origin)
        val diagnostic = factory(reportElement)
        diagnostics.reportFromPlugin(diagnostic, DefaultErrorMessagesKaptLite)
    }

    private fun findReportElement(origin: JvmDeclarationOrigin): PsiElement {
        val element = origin.element

        when (element) {
            is KtObjectDeclaration -> element.getObjectKeyword()?.let { return it }
            is KtConstructor<*> -> element.getConstructorKeyword()?.let { return it }
            is KtFunctionLiteral -> return element.lBrace
        }

        if (element is KtNamedDeclaration) {
            val identifier = element.nameIdentifier
            if (identifier != null) {
                return identifier
            }
        } else if (element != null) {
            return element
        }

        return state.files.first()
    }

    fun isValidIdentifier(name: String, languageLevel: LanguageLevel = LanguageLevel.JDK_1_8): Boolean {
        if (JavaLexer.isKeyword(name, languageLevel) || name.isEmpty() || !Character.isJavaIdentifierStart(name.first())) {
            return false
        }

        for (i in 1 until name.length) {
            if (!Character.isJavaIdentifierPart(name[i])) {
                return false
            }
        }

        return true
    }
}