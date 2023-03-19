/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGenerator
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.DelegatingClassBuilderFactory
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.codegen.`when`.WhenByEnumsMapping
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.MemberDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
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
class JvmAbiClassBuilderInterceptor : ClassGeneratorExtension {
    val abiClassInfo: MutableMap<String, AbiClassInfo> = mutableMapOf()

    override fun generateClass(generator: ClassGenerator, declaration: IrClass?): ClassGenerator =
        AbiInfoClassGenerator(generator, declaration)

    private inner class AbiInfoClassGenerator(
        private val delegate: ClassGenerator,
        irClass: IrClass?,
    ) : ClassGenerator by delegate {
        private val isPrivateClass = irClass != null && DescriptorVisibilities.isPrivate(irClass.visibility)
        lateinit var internalName: String
        var localOrAnonymousClass = false
        var publicAbi = false
        val methodInfos = mutableMapOf<Method, AbiMethodInfo>()
        val maskedMethods = mutableSetOf<Method>() // Methods which should be stripped even if they are marked as KEEP

        override fun defineClass(
            version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>
        ) {
            // Always keep annotation classes
            // TODO: Investigate whether there are cases where we can remove annotation classes from the ABI.
            if (access and Opcodes.ACC_ANNOTATION != 0) {
                publicAbi = true
            }

            internalName = name
            delegate.defineClass(version, access, name, signature, superName, interfaces)
        }

        override fun visitEnclosingMethod(owner: String, name: String?, desc: String?) {
            localOrAnonymousClass = true
            delegate.visitEnclosingMethod(owner, name, desc)
        }

        override fun newMethod(
            declaration: IrFunction?, access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?
        ): MethodVisitor {
            if (publicAbi) {
                return delegate.newMethod(declaration, access, name, desc, signature, exceptions)
            }

            // inline suspend functions are a special case: Unless they use reified type parameters,
            // we will transform the original method and generate a $$forInline method for the inliner.
            // Only the latter needs to be kept, the former can be stripped. Unfortunately, there is no
            // metadata to indicate this (the inliner simply first checks for a method such as `f$$forInline`
            // and then checks for `f` if this method doesn't exist) so we have to remember to strip the
            // original methods if there was a $$forInline version.
            if (name.endsWith(FOR_INLINE_SUFFIX) && !isPrivateClass) {
                methodInfos[Method(name, desc)] = AbiMethodInfo.KEEP
                maskedMethods += Method(name.removeSuffix(FOR_INLINE_SUFFIX), desc)
                return delegate.newMethod(declaration, access, name, desc, signature, exceptions)
            }

            // Remove private functions from the ABI jars
            if (
                access and Opcodes.ACC_PRIVATE != 0 && declaration != null && DescriptorVisibilities.isPrivate(declaration.visibility)
                || name == "<clinit>" || name.startsWith("access\$") && access and Opcodes.ACC_SYNTHETIC != 0
            ) {
                return delegate.newMethod(declaration, access, name, desc, signature, exceptions)
            }

            // Copy inline functions verbatim
            if (declaration?.isInline == true && !isPrivateClass) {
                methodInfos[Method(name, desc)] = AbiMethodInfo.KEEP
            } else {
                methodInfos[Method(name, desc)] = AbiMethodInfo.STRIP
            }
            return delegate.newMethod(declaration, access, name, desc, signature, exceptions)
        }

        // Parse the public ABI flag from the Kotlin metadata annotation
        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor {
            val delegate = delegate.visitAnnotation(desc, visible)
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

        override fun done(generateSmapCopyToAnnotation: Boolean) {
            // Remove local or anonymous classes unless they are in the scope of an inline function and
            // strip non-inline methods from all other classes.
            when {
                publicAbi ->
                    abiClassInfo[internalName] = AbiClassInfo.Public
                !localOrAnonymousClass && !isWhenMappingClass -> {
                    for (method in maskedMethods) {
                        methodInfos.replace(method, AbiMethodInfo.STRIP)
                    }
                    abiClassInfo[internalName] = AbiClassInfo.Stripped(methodInfos)
                }
            }
            delegate.done(generateSmapCopyToAnnotation)
        }

        private val isWhenMappingClass: Boolean
            get() = internalName.endsWith(WhenByEnumsMapping.MAPPINGS_CLASS_NAME_POSTFIX)
    }
}
