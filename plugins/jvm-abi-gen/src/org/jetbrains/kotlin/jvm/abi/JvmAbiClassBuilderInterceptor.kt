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
import org.jetbrains.kotlin.codegen.`when`.WhenByEnumsMapping
import org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.effectiveVisibility
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

data class Member(val name: String?, val descriptor: String?)

enum class AbiMethodInfo {
    KEEP,
    STRIP,
}

sealed class AbiClassInfo {
    object Keep : AbiClassInfo()
    object Delete : AbiClassInfo()
    class Strip(val memberInfo: Map<Member, AbiMethodInfo>) : AbiClassInfo()
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
class JvmAbiClassBuilderInterceptor(private val deleteNonPublicAbi: Boolean) : ClassBuilderInterceptorExtension {
    val abiClassInfo: MutableMap<String, AbiClassInfo> = mutableMapOf()

    override fun interceptClassBuilderFactory(
        interceptedFactory: ClassBuilderFactory,
        bindingContext: BindingContext,
        diagnostics: DiagnosticSink
    ): ClassBuilderFactory = object : DelegatingClassBuilderFactory(interceptedFactory) {
        override fun newClassBuilder(origin: JvmDeclarationOrigin): DelegatingClassBuilder {
            val descriptor = origin.descriptor as? ClassDescriptor
            val effectiveVisibility = descriptor?.effectiveVisibility()?.toVisibility() ?: Visibilities.Public

            return AbiInfoClassBuilder(interceptedFactory.newClassBuilder(origin), effectiveVisibility)
        }
    }

    private inner class AbiInfoClassBuilder(
        private val delegate: ClassBuilder,
        private val classVisibility: Visibility,
    ) : DelegatingClassBuilder() {
        private lateinit var internalName: String

        private var localOrAnonymousClass = false
        private var keepEverything = false

        private val memberInfos = mutableMapOf<Member, AbiMethodInfo>()
        private val maskedMethods = mutableSetOf<Member>() // Methods which should be stripped even if they are marked as KEEP

        override fun getDelegate(): ClassBuilder = delegate

        override fun defineClass(
            origin: PsiElement?,
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String,
            interfaces: Array<out String>
        ) {
            // Always keep annotation classes
            // TODO: Investigate whether there are cases where we can remove annotation classes from the ABI.
            keepEverything = keepEverything || access and Opcodes.ACC_ANNOTATION != 0
            internalName = name
            super.defineClass(origin, version, access, name, signature, superName, interfaces)
        }

        override fun visitOuterClass(owner: String, name: String?, desc: String?) {
            localOrAnonymousClass = true
            super.visitOuterClass(owner, name, desc)
        }

        override fun newField(
            origin: JvmDeclarationOrigin,
            access: Int,
            name: String,
            desc: String,
            signature: String?,
            value: Any?
        ): FieldVisitor {
            val effectiveVisibility = (origin.descriptor as? MemberDescriptor)?.effectiveVisibility()?.toVisibility() ?: Visibilities.Public

            val info: AbiMethodInfo? = when {
                Visibilities.isPrivate(effectiveVisibility) -> null
                !deleteNonPublicAbi -> AbiMethodInfo.KEEP
                !effectiveVisibility.isPublicAPI -> null
                else -> AbiMethodInfo.KEEP
            }

            if (info != null) {
                memberInfos[Member(name, desc)] = info
            }

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

            if (keepEverything) {
                return super.newMethod(origin, access, name, desc, signature, exceptions)
            }

            if (name == "<clinit>") {
                //remove all static initializers
                return super.newMethod(origin, access, name, desc, signature, exceptions)
            }

            if (name.startsWith("access\$") && access and Opcodes.ACC_SYNTHETIC != 0) {
                //remove all synthetic access methods
                return super.newMethod(origin, access, name, desc, signature, exceptions)
            }

            if (access and (Opcodes.ACC_NATIVE or Opcodes.ACC_ABSTRACT) != 0) {
                memberInfos[Member(name, desc)] = AbiMethodInfo.KEEP
                return super.newMethod(origin, access, name, desc, signature, exceptions)
            }

            val isPrivateClass = Visibilities.isPrivate(classVisibility)

            // inline suspend functions are a special case: Unless they use reified type parameters,
            // we will transform the original method and generate a $$forInline method for the inliner.
            // Only the latter needs to be kept, the former can be stripped. Unfortunately, there is no
            // metadata to indicate this (the inliner simply first checks for a method such as `f$$forInline`
            // and then checks for `f` if this method doesn't exist) so we have to remember to strip the
            // original methods if there was a $$forInline version.
            if (name.endsWith(FOR_INLINE_SUFFIX) && !isPrivateClass) {
                // Note that origin.descriptor is null on the JVM BE in this case.
                memberInfos[Member(name, desc)] = AbiMethodInfo.KEEP
                maskedMethods += Member(name.removeSuffix(FOR_INLINE_SUFFIX), desc)
                return super.newMethod(origin, access, name, desc, signature, exceptions)
            }

            // Remove private functions from the ABI jars
            val descriptor = origin.descriptor as? MemberDescriptor
            if (access and Opcodes.ACC_PRIVATE != 0 && descriptor?.visibility?.let(DescriptorVisibilities::isPrivate) == true) {
                return super.newMethod(origin, access, name, desc, signature, exceptions)
            }

            if (deleteNonPublicAbi) {
                val effectiveVisibility = descriptor?.effectiveVisibility()?.toVisibility() ?: Visibilities.Public
                if (!effectiveVisibility.isPublicAPI) {
                    return super.newMethod(origin, access, name, desc, signature, exceptions)
                }
            }

            // Copy inline functions verbatim
            if (origin.descriptor?.safeAs<FunctionDescriptor>()?.isInline == true && !isPrivateClass) {
                memberInfos[Member(name, desc)] = AbiMethodInfo.KEEP
            } else {
                memberInfos[Member(name, desc)] = AbiMethodInfo.STRIP
            }
            return super.newMethod(origin, access, name, desc, signature, exceptions)
        }

        // Parse the public ABI flag from the Kotlin metadata annotation
        override fun newAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
            val delegate = super.newAnnotation(desc, visible)
            if (keepEverything || desc != JvmAnnotationNames.METADATA_DESC)
                return delegate

            return object : AnnotationVisitor(Opcodes.API_VERSION, delegate) {
                override fun visit(name: String?, value: Any?) {
                    if ((name == JvmAnnotationNames.METADATA_EXTRA_INT_FIELD_NAME) && (value is Int)) {
                        keepEverything = keepEverything || value and JvmAnnotationNames.METADATA_PUBLIC_ABI_FLAG != 0
                    }
                    super.visit(name, value)
                }
            }
        }

        override fun done() {
            // Remove local or anonymous classes unless they are in the scope of an inline function and
            // strip non-inline methods from all other classes.
            abiClassInfo[internalName] = when {
                keepEverything -> AbiClassInfo.Keep
                deleteNonPublicAbi && !classVisibility.isPublicAPI -> AbiClassInfo.Delete
                localOrAnonymousClass || isWhenMappingClass -> AbiClassInfo.Delete
                else -> {
                    for (method in maskedMethods) {
                        memberInfos.replace(method, AbiMethodInfo.STRIP)
                    }
                    AbiClassInfo.Strip(memberInfos)
                }
            }
            super.done()
        }

        private val isWhenMappingClass: Boolean
            get() = internalName.endsWith(WhenByEnumsMapping.MAPPINGS_CLASS_NAME_POSTFIX)
    }
}
