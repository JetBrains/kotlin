/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.backend.jvm.extensions.ClassGenerator
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.codegen.inline.coroutines.FOR_INLINE_SUFFIX
import org.jetbrains.kotlin.codegen.`when`.WhenByEnumsMapping
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.isFileClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
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
    class Stripped(val methodInfo: Map<Method, AbiMethodInfo>, val prune: Boolean = false) : AbiClassInfo()
    object Deleted : AbiClassInfo()
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
class JvmAbiClassBuilderInterceptor(
    private val removeDataClassCopyIfConstructorIsPrivate: Boolean,
    private val removePrivateClasses: Boolean,
) : ClassGeneratorExtension {
    private var abiClassInfoBuilder = JvmAbiClassInfoBuilder(removePrivateClasses)

    fun buildAbiClassInfoAndReleaseResources(): Map<String, AbiClassInfo> {
        return abiClassInfoBuilder.buildClassInfo().also {
            abiClassInfoBuilder = JvmAbiClassInfoBuilder(removePrivateClasses)
        }
    }

    override fun generateClass(generator: ClassGenerator, declaration: IrClass?): ClassGenerator =
        AbiInfoClassGenerator(generator, declaration)

    private inner class AbiInfoClassGenerator(
        private val delegate: ClassGenerator,
        irClass: IrClass?,
    ) : ClassGenerator by delegate {
        private val isPrivateClass = irClass != null && DescriptorVisibilities.isPrivate(irClass.visibility)
        private val isDataClass = irClass != null && irClass.isData
        private val removeClassFromAbi = shouldRemoveFromAbi(irClass, removePrivateClasses)

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        private val primaryConstructorIsNotInAbi =
            irClass?.primaryConstructor?.visibility?.let(DescriptorVisibilities::isPrivate) == true

        lateinit var internalName: String
        lateinit var superInterfaces: List<String>
        var localOrAnonymousClass = false
        var keepClassAsIs = false
        val methodInfos = mutableMapOf<Method, AbiMethodInfo>()
        val maskedMethods = mutableSetOf<Method>() // Methods which should be stripped even if they are marked as KEEP

        override fun defineClass(
            version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>
        ) {
            // Always keep annotation classes
            // TODO: Investigate whether there are cases where we can remove annotation classes from the ABI.
            keepClassAsIs = keepClassAsIs || access and Opcodes.ACC_ANNOTATION != 0

            internalName = name
            superInterfaces = interfaces.asList()
            delegate.defineClass(version, access, name, signature, superName, interfaces)
        }

        override fun visitEnclosingMethod(owner: String, name: String?, desc: String?) {
            localOrAnonymousClass = true
            delegate.visitEnclosingMethod(owner, name, desc)
        }

        override fun newMethod(
            declaration: IrFunction?, access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?
        ): MethodVisitor {
            if (keepClassAsIs || removeClassFromAbi) {
                // We don't care about methods when we remove or keep this class completely.
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

            if (isDataClass && removeDataClassCopyIfConstructorIsPrivate &&
                (name == "copy" || name == "copy${JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX}")
            ) {
                if (primaryConstructorIsNotInAbi) {
                    return delegate.newMethod(declaration, access, name, desc, signature, exceptions)
                }
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
            if (keepClassAsIs || desc != JvmAnnotationNames.METADATA_DESC)
                return delegate

            return object : AnnotationVisitor(Opcodes.API_VERSION, delegate) {
                override fun visit(name: String?, value: Any?) {
                    if ((name == JvmAnnotationNames.METADATA_EXTRA_INT_FIELD_NAME) && (value is Int)) {
                        keepClassAsIs = keepClassAsIs || value and JvmAnnotationNames.METADATA_PUBLIC_ABI_FLAG != 0
                    }
                    super.visit(name, value)
                }
            }
        }

        override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
            abiClassInfoBuilder.addInnerClass(name, outerName)
            delegate.visitInnerClass(name, outerName, innerName, access)
        }

        override fun done(generateSmapCopyToAnnotation: Boolean) {
            // Remove local or anonymous classes unless they are in the scope of an inline function and
            // strip non-inline methods from all other classes.
            val classInfo = when {
                keepClassAsIs -> AbiClassInfo.Public
                removeClassFromAbi -> AbiClassInfo.Deleted
                localOrAnonymousClass -> AbiClassInfo.Deleted
                isWhenMappingClass -> AbiClassInfo.Deleted
                else -> {
                    for (method in maskedMethods) {
                        methodInfos[method] = AbiMethodInfo.STRIP
                    }
                    AbiClassInfo.Stripped(methodInfos)
                }
            }
            abiClassInfoBuilder.recordInitialClassInfo(internalName, classInfo, superInterfaces)
            delegate.done(generateSmapCopyToAnnotation)
        }

        private val isWhenMappingClass: Boolean
            get() = internalName.endsWith(WhenByEnumsMapping.MAPPINGS_CLASS_NAME_POSTFIX)
    }
}

private fun shouldRemoveFromAbi(irClass: IrClass?, removePrivateClasses: Boolean): Boolean = when {
    irClass == null -> false
    irClass.isFileClass -> false
    removePrivateClasses -> irClass.isEffectivelyPrivate()
    else -> false
}
