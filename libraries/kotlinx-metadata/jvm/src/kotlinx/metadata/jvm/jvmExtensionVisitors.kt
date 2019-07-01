/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.*
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil

/**
 * A visitor containing the common code to visit JVM extensions for Kotlin declaration containers, such as classes and package fragments.
 */
abstract class JvmDeclarationContainerExtensionVisitor @JvmOverloads constructor(
    protected open val delegate: JvmDeclarationContainerExtensionVisitor? = null
) : KmDeclarationContainerExtensionVisitor {
    /**
     * Visits the metadata of a local delegated property used somewhere inside this container (but not in a nested declaration container).
     * Note that for classes produced by the Kotlin compiler, such properties will have default accessors.
     *
     * The order of visited local delegated properties is important. The Kotlin compiler generates the corresponding property's index
     * at the call site, so that reflection would be able to load the metadata of the property with that index at runtime.
     * If an incorrect index is used, either the `KProperty<*>` object passed to delegate methods will point to the wrong property
     * at runtime, or an exception will be thrown.
     *
     * @param flags property flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag and [Flag.Property] flags
     * @param name the name of the property
     * @param getterFlags property accessor flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag
     *   and [Flag.PropertyAccessor] flags
     * @param setterFlags property accessor flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag
     *   and [Flag.PropertyAccessor] flags
     */
    open fun visitLocalDelegatedProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor? =
        delegate?.visitLocalDelegatedProperty(flags, name, getterFlags, setterFlags)

    /**
     * Visits the name of the module where this container is declared.
     */
    open fun visitModuleName(name: String) {
        delegate?.visitModuleName(name)
    }
}

/**
 * A visitor to visit JVM extensions for a class.
 */
open class JvmClassExtensionVisitor @JvmOverloads constructor(
    delegate: JvmClassExtensionVisitor? = null
) : KmClassExtensionVisitor, JvmDeclarationContainerExtensionVisitor(delegate) {
    override val delegate: JvmClassExtensionVisitor?
        get() = super.delegate as JvmClassExtensionVisitor?

    final override val type: KmExtensionType
        get() = TYPE

    /**
     * Visits the JVM internal name of the original class this anonymous object is copied from. This method is called for
     * anonymous objects copied from bodies of inline functions to the use site by the Kotlin compiler.
     */
    open fun visitAnonymousObjectOriginName(internalName: String) {
        delegate?.visitAnonymousObjectOriginName(internalName)
    }

    /**
     * Visits the end of JVM extensions for the class.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }

    companion object {
        /**
         * The type of this extension visitor.
         *
         * @see KmExtensionType
         */
        @JvmField
        val TYPE: KmExtensionType = KmExtensionType(JvmClassExtensionVisitor::class)
    }
}

/**
 * A visitor to visit JVM extensions for a package fragment.
 */
open class JvmPackageExtensionVisitor @JvmOverloads constructor(
    delegate: JvmPackageExtensionVisitor? = null
) : KmPackageExtensionVisitor, JvmDeclarationContainerExtensionVisitor(delegate) {
    override val delegate: JvmPackageExtensionVisitor?
        get() = super.delegate as JvmPackageExtensionVisitor?

    final override val type: KmExtensionType
        get() = TYPE

    /**
     * Visits the end of JVM extensions for the package fragment.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }

    companion object {
        /**
         * The type of this extension visitor.
         *
         * @see KmExtensionType
         */
        @JvmField
        val TYPE: KmExtensionType = KmExtensionType(JvmPackageExtensionVisitor::class)
    }
}

/**
 * A visitor to visit JVM extensions for a function.
 */
open class JvmFunctionExtensionVisitor @JvmOverloads constructor(
    private val delegate: JvmFunctionExtensionVisitor? = null
) : KmFunctionExtensionVisitor {
    final override val type: KmExtensionType
        get() = TYPE

    /**
     * Visits the JVM signature of the function, or null if the JVM signature of this function is unknown.
     *
     * Example: `JvmMethodSignature("equals", "(Ljava/lang/Object;)Z")`
     *
     * @param signature the signature of the function
     */
    open fun visit(signature: JvmMethodSignature?) {
        delegate?.visit(signature)
    }

    /**
     * Visits the JVM internal name of the original class the lambda class for this function is copied from.
     * This information is present for lambdas copied from bodies of inline functions to the use site by the Kotlin compiler.
     */
    open fun visitLambdaClassOriginName(internalName: String) {
        delegate?.visitLambdaClassOriginName(internalName)
    }

    /**
     * Visits the end of JVM extensions for the function.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }

    companion object {
        /**
         * The type of this extension visitor.
         *
         * @see KmExtensionType
         */
        @JvmField
        val TYPE: KmExtensionType = KmExtensionType(JvmFunctionExtensionVisitor::class)
    }
}

/**
 * A visitor to visit JVM extensions for a property.
 */
open class JvmPropertyExtensionVisitor @JvmOverloads constructor(
    private val delegate: JvmPropertyExtensionVisitor? = null
) : KmPropertyExtensionVisitor {
    final override val type: KmExtensionType
        get() = TYPE

    /**
     * Visits JVM signatures of field and accessors generated for the property.
     *
     * @param jvmFlags JVM-specific flags of the property, consisting of [JvmFlag.Property] flags
     * @param fieldSignature the signature of the backing field of the property, or `null` if this property has no backing field.
     *                       Example: `JvmFieldSignature("X", "Ljava/lang/Object;")`
     * @param getterSignature the signature of the property getter, or `null` if this property has no getter or its signature is unknown.
     *                        Example: `JvmMethodSignature("getX", "()Ljava/lang/Object;")`
     * @param setterSignature the signature of the property setter, or `null` if this property has no setter or its signature is unknown.
     *                        Example: `JvmMethodSignature("setX", "(Ljava/lang/Object;)V")`
     */
    open fun visit(
        jvmFlags: Flags,
        fieldSignature: JvmFieldSignature?,
        getterSignature: JvmMethodSignature?,
        setterSignature: JvmMethodSignature?
    ) {
        delegate?.visit(jvmFlags, fieldSignature, getterSignature, setterSignature)

        @Suppress("DEPRECATION_ERROR")
        visit(fieldSignature, getterSignature, setterSignature)
    }

    @Deprecated(
        "Use visit(Flags, JvmFieldSignature?, JvmMethodSignature?, JvmMethodSignature?) instead.",
        level = DeprecationLevel.ERROR,
        replaceWith = ReplaceWith("visit(flagsOf(), fieldSignature, getterSignature, setterSignature)", "kotlinx.metadata.flagsOf")
    )
    open fun visit(
        fieldSignature: JvmFieldSignature?,
        getterSignature: JvmMethodSignature?,
        setterSignature: JvmMethodSignature?
    ) {
        @Suppress("DEPRECATION_ERROR")
        delegate?.visit(fieldSignature, getterSignature, setterSignature)
    }

    /**
     * Visits the JVM signature of a synthetic method which is generated to store annotations on a property in the bytecode.
     *
     * Example: `JvmMethodSignature("getX$annotations", "()V")`
     *
     * @param signature the signature of the synthetic method
     */
    open fun visitSyntheticMethodForAnnotations(signature: JvmMethodSignature?) {
        delegate?.visitSyntheticMethodForAnnotations(signature)
    }

    /**
     * Visits the end of JVM extensions for the property.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }

    companion object {
        /**
         * The type of this extension visitor.
         *
         * @see KmExtensionType
         */
        @JvmField
        val TYPE: KmExtensionType = KmExtensionType(JvmPropertyExtensionVisitor::class)
    }
}

/**
 * A visitor to visit JVM extensions for a constructor.
 */
open class JvmConstructorExtensionVisitor @JvmOverloads constructor(
    private val delegate: JvmConstructorExtensionVisitor? = null
) : KmConstructorExtensionVisitor {
    final override val type: KmExtensionType
        get() = TYPE

    /**
     * Visits the JVM signature of the constructor, or null if the JVM signature of this constructor is unknown.
     *
     * Example: `JvmMethodSignature("<init>", "(Ljava/lang/Object;)V")`
     *
     * @param signature the signature of the constructor
     */
    open fun visit(signature: JvmMethodSignature?) {
        delegate?.visit(signature)
    }

    companion object {
        /**
         * The type of this extension visitor.
         *
         * @see KmExtensionType
         */
        @JvmField
        val TYPE: KmExtensionType = KmExtensionType(JvmConstructorExtensionVisitor::class)
    }
}

/**
 * A visitor to visit JVM extensions for a type parameter.
 */
open class JvmTypeParameterExtensionVisitor @JvmOverloads constructor(
    private val delegate: JvmTypeParameterExtensionVisitor? = null
) : KmTypeParameterExtensionVisitor {
    final override val type: KmExtensionType
        get() = TYPE

    /**
     * Visits an annotation on the type parameter.
     *
     * @param annotation the annotation on the type parameter
     */
    open fun visitAnnotation(annotation: KmAnnotation) {
        delegate?.visitAnnotation(annotation)
    }

    /**
     * Visits the end of JVM extensions for the type parameter.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }

    companion object {
        /**
         * The type of this extension visitor.
         *
         * @see KmExtensionType
         */
        @JvmField
        val TYPE: KmExtensionType = KmExtensionType(JvmTypeParameterExtensionVisitor::class)
    }
}

/**
 * A visitor to visit JVM extensions for a type.
 */
open class JvmTypeExtensionVisitor @JvmOverloads constructor(
    private val delegate: JvmTypeExtensionVisitor? = null
) : KmTypeExtensionVisitor {
    final override val type: KmExtensionType
        get() = TYPE

    /**
     * Visits the JVM-specific flags of a type.
     *
     * @param isRaw whether the type is seen as a raw type in Java
     */
    open fun visit(isRaw: Boolean) {
        delegate?.visit(isRaw)
    }

    /**
     * Visits an annotation on the type.
     *
     * @param annotation the annotation on the type
     */
    open fun visitAnnotation(annotation: KmAnnotation) {
        delegate?.visitAnnotation(annotation)
    }

    /**
     * Visits the end of JVM extensions for the type parameter.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }

    companion object {
        /**
         * The type of this extension visitor.
         *
         * @see KmExtensionType
         */
        @JvmField
        val TYPE: KmExtensionType = KmExtensionType(JvmTypeExtensionVisitor::class)

        /**
         * The type flexibility id, signifying that the visited type is a JVM platform type.
         *
         * @see KmTypeVisitor.visitFlexibleTypeUpperBound
         */
        const val PLATFORM_TYPE_ID = JvmProtoBufUtil.PLATFORM_TYPE_ID
    }
}
