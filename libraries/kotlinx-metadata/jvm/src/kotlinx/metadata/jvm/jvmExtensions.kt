/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.*
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil

/**
 * A visitor to visit JVM extensions for a function.
 */
open class JvmFunctionExtensionVisitor @JvmOverloads constructor(
    private val delegate: JvmFunctionExtensionVisitor? = null
) : KmFunctionExtensionVisitor {
    /**
     * Visits the JVM signature of the function, in the JVM-based format: `"equals(Ljava/lang/Object;)Z"`,
     * or null if the JVM signature of this function is unknown.
     *
     * @param desc the signature of the function
     */
    open fun visit(desc: String?) {
        delegate?.visit(desc)
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
    /**
     * Visits JVM signatures of field and accessors generated for the property.
     *
     * @param fieldName the name of the field, or `null` if this property has no field
     * @param fieldTypeDesc the type of the field in the JVM-based format (`"Ljava/lang/Object;"`), or `null` if this property has no field
     * @param getterDesc the signature of the property getter in the JVM-based format: `"getX()Ljava/lang/Object;"`,
     *                   or `null` if this property has no getter or its signature is unknown
     * @param setterDesc the signature of the property setter in the JVM-based format: `"setX(Ljava/lang/Object;)V"`,
     *                   or `null` if this property has no setter or its signature is unknown
     */
    open fun visit(fieldName: String?, fieldTypeDesc: String?, getterDesc: String?, setterDesc: String?) {
        delegate?.visit(fieldName, fieldTypeDesc, getterDesc, setterDesc)
    }

    /**
     * Visits the JVM signature of a synthetic method which is generated to store annotations on a property in the bytecode,
     * in the JVM-based format: `"getX$annotations()V"`
     *
     * @param desc the signature of the synthetic method
     */
    open fun visitSyntheticMethodForAnnotations(desc: String?) {
        delegate?.visitSyntheticMethodForAnnotations(desc)
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
    /**
     * Visits the JVM signature of the constructor, in the JVM-based format: `"<init>(Ljava/lang/Object;)V"`,
     * or null if the JVM signature of this constructor is unknown.
     *
     * @param desc the signature of the constructor
     */
    open fun visit(desc: String?) {
        delegate?.visit(desc)
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
