/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kaptlite

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor

typealias OriginConsumer = MutableMap<String, JvmDeclarationOrigin>

class KaptLiteClassBuilderInterceptorExtension(private val originConsumer: OriginConsumer) : ClassBuilderInterceptorExtension {
    override fun interceptClassBuilderFactory(
        interceptedFactory: ClassBuilderFactory,
        bindingContext: BindingContext,
        diagnostics: DiagnosticSink
    ): ClassBuilderFactory {
        return KaptLiteClassBuilderFactory(interceptedFactory, originConsumer)
    }
}

private class KaptLiteClassBuilderFactory(
    private val delegateFactory: ClassBuilderFactory,
    private val originConsumer: OriginConsumer
) : ClassBuilderFactory {
    override fun getClassBuilderMode() = delegateFactory.classBuilderMode

    override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassBuilder {
        return KaptLiteClassBuilder(delegateFactory.newClassBuilder(origin), origin, originConsumer)
    }

    override fun asText(builder: ClassBuilder?): String? {
        return delegateFactory.asText((builder as KaptLiteClassBuilder).delegateBuilder)
    }

    override fun asBytes(builder: ClassBuilder?): ByteArray? {
        return delegateFactory.asBytes((builder as KaptLiteClassBuilder).delegateBuilder)
    }

    override fun close() {
        delegateFactory.close()
    }
}

private class KaptLiteClassBuilder(
    val delegateBuilder: ClassBuilder,
    private val origin: JvmDeclarationOrigin,
    private val originConsumer: OriginConsumer
) : DelegatingClassBuilder() {
    override fun getDelegate() = delegateBuilder

    private lateinit var className: String

    override fun defineClass(
        origin: PsiElement?,
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String,
        interfaces: Array<out String>
    ) {
        className = name
        originConsumer[name] = this.origin
        super.defineClass(origin, version, access, name, signature, superName, interfaces)
    }

    override fun newField(
        origin: JvmDeclarationOrigin,
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        value: Any?
    ): FieldVisitor {
        originConsumer["$className#$name"] = origin
        return super.newField(origin, access, name, desc, signature, value)
    }

    override fun newMethod(
        origin: JvmDeclarationOrigin,
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        originConsumer["$className#$name$desc"] = origin
        return super.newMethod(origin, access, name, desc, signature, exceptions)
    }
}