/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.DelegatingClassBuilderFactory
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.Method

enum class AbiMethodInfo {
    KEEP,
    STRIP,
}

sealed class AbiClassInfo {
    object Public : AbiClassInfo()
    class Stripped(val methodInfo: Map<Method, AbiMethodInfo>) : AbiClassInfo()
}

/**
 * Record ABI information for all classes in the current compilation unit.
 *
 * This needs to be a `ClassBuilderInterceptor`, since we need the descriptors
 * in order to know which methods can be safely stripped from the output.
 * On the other hand we cannot produce any output in this pass, since we need
 * to know which classes are part of the public ABI in order to produce the
 * correct `InnerClasses` attributes.
 *
 * ---
 *
 * Classes which are private, local, or anonymous can be stripped unless
 * they are marked as part of the public ABI by a bit in the Kotlin
 * metadata annotation. Public ABI classes have to be kept verbatim, since
 * they are copied to all call sites of a surrounding inline function.
 *
 * If we keep a class we will strip the bodies from all non-inline function,
 * remove private functions, and copy inline functions verbatim. There is one
 * exception to this for inline suspend functions.
 *
 * For an inline suspend function `f` we will usually generate two methods,
 * a non-inline method `f` and an inline method `f$$forInline`. The former can
 * be stripped. However, if `f` is not callable directly, we only generate a
 * single inline method `f` which should be kept.
 */
class JvmAbiClassBuilderInterceptor : ClassBuilderInterceptorExtension {
    val abiClassInfo: MutableMap<String, AbiClassInfo> = mutableMapOf()

    override fun interceptClassBuilderFactory(interceptedFactory: ClassBuilderFactory, bindingContext: BindingContext, diagnostics: DiagnosticSink): ClassBuilderFactory =
        object : DelegatingClassBuilderFactory(interceptedFactory) {
            override fun newClassBuilder(origin: JvmDeclarationOrigin): DelegatingClassBuilder {
                val descriptor = origin.descriptor as? ClassDescriptor
                val isPrivate = descriptor?.visibility?.let(DescriptorVisibilities::isPrivate) ?: false
                return AbiInfoClassBuilder(interceptedFactory.newClassBuilder(origin), isPrivate)
            }
        }

    private inner class AbiInfoClassBuilder(
        private val delegate: ClassBuilder,
        private val isPrivateClass: Boolean
    ) : DelegatingClassBuilder() {
        lateinit var internalName: String
        var localOrAnonymousClass = false
        var publicAbi = false
        val methodInfos = mutableMapOf<Method, AbiMethodInfo>()
        val maskedMethods = mutableSetOf<Method>() // Methods which should be stripped even if they are marked as KEEP

        override fun getDelegate(): ClassBuilder = delegate

        override fun defineClass(origin: PsiElement?, version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>) {
            // Always keep annotation classes
            if (access and Opcodes.ACC_ANNOTATION != 0) {
                publicAbi = true
            }

            internalName = name
            super.defineClass(origin, version, access, name, signature, superName, interfaces)
        }

        override fun visitOuterClass(owner: String, name: String?, desc: String?) {
            localOrAnonymousClass = true
            super.visitOuterClass(owner, name, desc)
        }

        override fun newMethod(origin: JvmDeclarationOrigin, access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
            if (publicAbi) {
                return super.newMethod(origin, access, name, desc, signature, exceptions)
            }

            // inline suspend functions are a special case: Unless they use reified type parameters,
            // we will transform the original method and generate a $$forInline method for the inliner.
            // Only the latter needs to be kept, the former can be stripped. Unfortunately, there is no
            // metadata to indicate this (the inliner simply first checks for a method such as `f$$forInline`
            // and then checks for `f` if this method doesn't exist) so we have to remember to strip the
            // original methods if there was a $$forInline version.
            if (name.endsWith(FOR_INLINE_SUFFIX) && !isPrivateClass) {
                // Note that origin.descriptor is null on the JVM BE in this case.
                methodInfos[Method(name, desc)] = AbiMethodInfo.KEEP
                maskedMethods += Method(name.removeSuffix(FOR_INLINE_SUFFIX), desc)
                return super.newMethod(origin, access, name, desc, signature, exceptions)
            }

            // Remove private functions from the ABI jars
            val descriptor = origin.descriptor as? MemberDescriptor
            if (access and Opcodes.ACC_PRIVATE != 0 && descriptor?.visibility?.let(DescriptorVisibilities::isPrivate) == true
                || name == "<clinit>" || name.startsWith("access\$") && access and Opcodes.ACC_SYNTHETIC != 0) {
                return super.newMethod(origin, access, name, desc, signature, exceptions)
            }

            // Copy inline functions verbatim
            if (origin.descriptor?.safeAs<FunctionDescriptor>()?.isInline == true && !isPrivateClass) {
                methodInfos[Method(name, desc)] = AbiMethodInfo.KEEP
            } else {
                methodInfos[Method(name, desc)] = AbiMethodInfo.STRIP
            }
            return super.newMethod(origin, access, name, desc, signature, exceptions)
        }

        // Parse the public ABI flag from the Kotlin metadata annotation
        override fun newAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
            val delegate = super.newAnnotation(desc, visible)
            if (publicAbi || desc != JvmAnnotationNames.METADATA_DESC)
                return delegate

            return object : AnnotationVisitor(Opcodes.API_VERSION, delegate) {
                override fun visit(name: String?, value: Any?) {
                    if ((name == JvmAnnotationNames.METADATA_EXTRA_INT_FIELD_NAME) && (value is Int)) {
                        publicAbi = publicAbi || value and JvmAnnotationNames.METADATA_PUBLIC_ABI_FLAG != 0
                    }
                    super.visit(name, value)
                }
            }
        }

        override fun done() {
            // Remove local or anonymous classes unless they are in the scope of an inline function and
            // strip non-inline methods from all other classe.
            when {
                publicAbi ->
                    abiClassInfo[internalName] = AbiClassInfo.Public
                !localOrAnonymousClass -> {
                    for (method in maskedMethods) {
                        methodInfos.replace(method, AbiMethodInfo.STRIP)
                    }
                    abiClassInfo[internalName] = AbiClassInfo.Stripped(methodInfos)
                }
            }
            super.done()
        }
    }
}
