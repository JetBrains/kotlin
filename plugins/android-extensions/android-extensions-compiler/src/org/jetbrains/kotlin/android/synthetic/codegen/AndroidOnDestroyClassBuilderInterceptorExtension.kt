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
import org.jetbrains.kotlin.android.synthetic.codegen.AbstractAndroidExtensionsExpressionCodegenExtension.Companion.ON_DESTROY_METHOD_NAME
import org.jetbrains.kotlin.android.synthetic.descriptors.ContainerOptionsProxy
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.kotlin.android.synthetic.codegen.AbstractAndroidExtensionsExpressionCodegenExtension.Companion.CLEAR_CACHE_METHOD_NAME
import org.jetbrains.kotlin.psi.KtElement

abstract class AbstractAndroidOnDestroyClassBuilderInterceptorExtension : ClassBuilderInterceptorExtension {
    override fun interceptClassBuilderFactory(
            interceptedFactory: ClassBuilderFactory,
            bindingContext: BindingContext,
            diagnostics: DiagnosticSink
    ): ClassBuilderFactory {
        return AndroidOnDestroyClassBuilderFactory(interceptedFactory, bindingContext)
    }

    abstract fun getGlobalCacheImpl(element: KtElement): CacheImplementation

    private inner class AndroidOnDestroyClassBuilderFactory(
            private val delegateFactory: ClassBuilderFactory,
            val bindingContext: BindingContext
    ) : ClassBuilderFactory {

        override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassBuilder {
            return AndroidOnDestroyCollectorClassBuilder(delegateFactory.newClassBuilder(origin), bindingContext)
        }

        override fun getClassBuilderMode() = delegateFactory.classBuilderMode

        override fun asText(builder: ClassBuilder?): String? {
            return delegateFactory.asText((builder as AndroidOnDestroyCollectorClassBuilder).delegateClassBuilder)
        }

        override fun asBytes(builder: ClassBuilder?): ByteArray? {
            return delegateFactory.asBytes((builder as AndroidOnDestroyCollectorClassBuilder).delegateClassBuilder)
        }

        override fun close() {
            delegateFactory.close()
        }
    }

    private inner class AndroidOnDestroyCollectorClassBuilder(
            internal val delegateClassBuilder: ClassBuilder,
            val bindingContext: BindingContext
    ) : DelegatingClassBuilder() {
        private var currentClass: KtClass? = null
        private var currentClassName: String? = null

        override fun getDelegate() = delegateClassBuilder

        override fun defineClass(
                origin: PsiElement?,
                version: Int,
                access: Int,
                name: String,
                signature: String?,
                superName: String,
                interfaces: Array<out String>
        ) {
            if (origin is KtClass) {
                currentClass = origin
                currentClassName = name
            }
            super.defineClass(origin, version, access, name, signature, superName, interfaces)
        }

        override fun newMethod(
                origin: JvmDeclarationOrigin,
                access: Int,
                name: String,
                desc: String,
                signature: String?,
                exceptions: Array<out String>?
        ): MethodVisitor {
            return object : MethodVisitor(Opcodes.API_VERSION, super.newMethod(origin, access, name, desc, signature, exceptions)) {
                override fun visitInsn(opcode: Int) {
                    if (opcode == Opcodes.RETURN) {
                        generateClearCacheMethodCall()
                    }

                    super.visitInsn(opcode)
                }

                private fun generateClearCacheMethodCall() {
                    val currentClass = currentClass
                    if (name != ON_DESTROY_METHOD_NAME || currentClass == null) return
                    if (Type.getArgumentTypes(desc).isNotEmpty()) return
                    if (Type.getReturnType(desc) != Type.VOID_TYPE) return

                    val containerType = currentClassName?.let { Type.getObjectType(it) } ?: return

                    val container = bindingContext.get(BindingContext.CLASS, currentClass) ?: return
                    val entityOptions = ContainerOptionsProxy.create(container)
                    if (!entityOptions.containerType.isFragment || !(entityOptions.cache ?: getGlobalCacheImpl(currentClass)).hasCache) return

                    val iv = InstructionAdapter(this)
                    iv.load(0, containerType)
                    iv.invokevirtual(currentClassName, CLEAR_CACHE_METHOD_NAME, "()V", false)
                }
            }
        }
    }

}