/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.android.synthetic.codegen

import com.intellij.psi.PsiElement
import kotlinx.android.extensions.CacheImplementation
import org.jetbrains.kotlin.android.synthetic.codegen.AbstractAndroidExtensionsExpressionCodegenExtension.Companion.CLEAR_CACHE_METHOD_NAME
import org.jetbrains.kotlin.android.synthetic.codegen.AbstractAndroidExtensionsExpressionCodegenExtension.Companion.ON_DESTROY_METHOD_NAME
import org.jetbrains.kotlin.android.synthetic.descriptors.ContainerOptionsProxy
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.DelegatingClassBuilderFactory
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

abstract class AbstractAndroidOnDestroyClassBuilderInterceptorExtension :
    @Suppress("DEPRECATION_ERROR") org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension {
    override fun interceptClassBuilderFactory(
        interceptedFactory: ClassBuilderFactory,
        bindingContext: BindingContext,
        diagnostics: DiagnosticSink
    ): ClassBuilderFactory = AndroidOnDestroyClassBuilderFactory(interceptedFactory)

    abstract fun getGlobalCacheImpl(element: KtElement): CacheImplementation

    private inner class AndroidOnDestroyClassBuilderFactory(delegate: ClassBuilderFactory) : DelegatingClassBuilderFactory(delegate) {
        override fun newClassBuilder(origin: JvmDeclarationOrigin): DelegatingClassBuilder =
            AndroidOnDestroyCollectorClassBuilder(delegate.newClassBuilder(origin), origin.hasCache)

        private val JvmDeclarationOrigin.hasCache: Boolean
            get() = descriptor is ClassDescriptor && element is KtElement &&
                    ContainerOptionsProxy.create(descriptor as ClassDescriptor).let {
                        it.containerType.isFragment && (it.cache ?: getGlobalCacheImpl(element as KtElement)).hasCache
                    }
    }

    private class AndroidOnDestroyCollectorClassBuilder(
        private val delegate: ClassBuilder,
        private val hasCache: Boolean
    ) : DelegatingClassBuilder() {
        private lateinit var currentClassName: String
        private lateinit var superClassName: String
        private var hasOnDestroy = false

        override fun getDelegate() = delegate

        override fun defineClass(
            origin: PsiElement?,
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String,
            interfaces: Array<out String>
        ) {
            currentClassName = name
            superClassName = superName
            super.defineClass(origin, version, access, name, signature, superName, interfaces)
        }

        override fun done(generateSmapCopyToAnnotation: Boolean) {
            if (hasCache && !hasOnDestroy) {
                val mv = newMethod(
                    JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PUBLIC or Opcodes.ACC_SYNTHETIC, ON_DESTROY_METHOD_NAME, "()V",
                    null, null
                )
                mv.visitCode()
                mv.visitVarInsn(Opcodes.ALOAD, 0)
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superClassName, ON_DESTROY_METHOD_NAME, "()V", false)
                mv.visitInsn(Opcodes.RETURN)
                mv.visitMaxs(1, 1)
                mv.visitEnd()
            }
            super.done(generateSmapCopyToAnnotation)
        }

        override fun newMethod(
            origin: JvmDeclarationOrigin,
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            val mv = super.newMethod(origin, access, name, desc, signature, exceptions)
            if (!hasCache || name != ON_DESTROY_METHOD_NAME || desc != "()V") return mv
            hasOnDestroy = true
            return object : MethodVisitor(Opcodes.API_VERSION, mv) {
                override fun visitInsn(opcode: Int) {
                    if (opcode == Opcodes.RETURN) {
                        visitVarInsn(Opcodes.ALOAD, 0)
                        visitMethodInsn(Opcodes.INVOKEVIRTUAL, currentClassName, CLEAR_CACHE_METHOD_NAME, "()V", false)
                    }
                    super.visitInsn(opcode)
                }
            }
        }
    }
}
