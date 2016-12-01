/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.noarg

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

class NoArgClassBuilderInterceptorExtension : ClassBuilderInterceptorExtension {
    override fun interceptClassBuilderFactory(
            interceptedFactory: ClassBuilderFactory,
            bindingContext: BindingContext,
            diagnostics: DiagnosticSink
    ): ClassBuilderFactory = NoArgClassBuilderFactory(interceptedFactory, bindingContext)

    private inner class NoArgClassBuilderFactory(
            private val delegateFactory: ClassBuilderFactory,
            val bindingContext: BindingContext
    ) : ClassBuilderFactory {

        override fun newClassBuilder(origin: JvmDeclarationOrigin): ClassBuilder {
            return AllOpenClassBuilder(delegateFactory.newClassBuilder(origin), bindingContext)
        }

        override fun getClassBuilderMode() = delegateFactory.classBuilderMode

        override fun asText(builder: ClassBuilder?): String? {
            return delegateFactory.asText((builder as AllOpenClassBuilder).delegateClassBuilder)
        }

        override fun asBytes(builder: ClassBuilder?): ByteArray? {
            return delegateFactory.asBytes((builder as AllOpenClassBuilder).delegateClassBuilder)
        }

        override fun close() {
            delegateFactory.close()
        }
    }

    private inner class AllOpenClassBuilder(
            internal val delegateClassBuilder: ClassBuilder,
            val bindingContext: BindingContext
    ) : DelegatingClassBuilder() {
        override fun getDelegate() = delegateClassBuilder

        private var superClassInternalName = ""
        private var hasSpecialAnnotation = false
        private var noArgConstructorGenerated = false

        override fun defineClass(
                origin: PsiElement?,
                version: Int,
                access: Int,
                name: String,
                signature: String?,
                superName: String,
                interfaces: Array<out String>
        ) {
            super.defineClass(origin, version, access, name, signature, superName, interfaces)

            hasSpecialAnnotation = false
            noArgConstructorGenerated = false
            superClassInternalName = superName

            if (origin is KtClass) {
                val descriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, origin] as? ClassDescriptor
                if (descriptor != null && descriptor.kind == ClassKind.CLASS && origin.isNoArgClass()) {
                    hasSpecialAnnotation = true
                }
            }
        }

        private fun KtClass.isNoArgClass() = this.getUserData(NO_ARG_CLASS_KEY) ?: false

        override fun newMethod(
                origin: JvmDeclarationOrigin,
                access: Int,
                name: String,
                desc: String,
                signature: String?,
                exceptions: Array<out String>?
        ): MethodVisitor {
            if (name == "<init>" && desc == "()V") {
                noArgConstructorGenerated = true
            }

            return super.newMethod(origin, access, name, desc, signature, exceptions)
        }

        override fun done() {
            val superClassInternalName = this.superClassInternalName
            if (hasSpecialAnnotation && !noArgConstructorGenerated && superClassInternalName.isNotEmpty()) {
                super.newMethod(JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PUBLIC, "<init>", "()V", null, null).apply {
                    visitCode()
                    visitVarInsn(Opcodes.ALOAD, 0)
                    visitMethodInsn(Opcodes.INVOKESPECIAL, superClassInternalName, "<init>", "()V", false)
                    visitInsn(Opcodes.RETURN)
                    visitMaxs(1, 1)
                    visitEnd()
                }
            }

            super.done()
        }
    }
}