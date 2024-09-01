/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.metadata.jvm.internal

import kotlin.metadata.*
import kotlin.metadata.internal.extensions.*
import kotlin.metadata.jvm.*

internal val KmClass.jvm: JvmClassExtension
    get() = getExtension(JvmClassExtension.TYPE) as JvmClassExtension

internal val KmPackage.jvm: JvmPackageExtension
    get() = getExtension(JvmPackageExtension.TYPE) as JvmPackageExtension

internal val KmFunction.jvm: JvmFunctionExtension
    get() = getExtension(JvmFunctionExtension.TYPE) as JvmFunctionExtension

internal val KmProperty.jvm: JvmPropertyExtension
    get() = getExtension(JvmPropertyExtension.TYPE) as JvmPropertyExtension

internal val KmConstructor.jvm: JvmConstructorExtension
    get() = getExtension(JvmConstructorExtension.TYPE) as JvmConstructorExtension

internal val KmTypeParameter.jvm: JvmTypeParameterExtension
    get() = getExtension(JvmTypeParameterExtension.TYPE) as JvmTypeParameterExtension

internal val KmType.jvm: JvmTypeExtension
    get() = getExtension(JvmTypeExtension.TYPE) as JvmTypeExtension


internal class JvmClassExtension : KmClassExtension {
    val localDelegatedProperties: MutableList<KmProperty> = ArrayList(0)
    var moduleName: String? = null
    var anonymousObjectOriginName: String? = null
    var jvmFlags: Int = 0

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(JvmClassExtension::class)
    }
}

internal class JvmPackageExtension : KmPackageExtension {
    val localDelegatedProperties: MutableList<KmProperty> = ArrayList(0)
    var moduleName: String? = null

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        @JvmField
        val TYPE: KmExtensionType = KmExtensionType(JvmPackageExtension::class)
    }
}

internal class JvmFunctionExtension : KmFunctionExtension {
    var signature: JvmMethodSignature? = null
    var lambdaClassOriginName: String? = null

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        @JvmField
        val TYPE: KmExtensionType = KmExtensionType(JvmFunctionExtension::class)
    }
}

internal class JvmPropertyExtension : KmPropertyExtension {
    var jvmFlags: Int = 0
    var fieldSignature: JvmFieldSignature? = null
    var getterSignature: JvmMethodSignature? = null
    var setterSignature: JvmMethodSignature? = null
    var syntheticMethodForAnnotations: JvmMethodSignature? = null
    var syntheticMethodForDelegate: JvmMethodSignature? = null

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        @JvmField
        val TYPE: KmExtensionType = KmExtensionType(JvmPropertyExtension::class)
    }
}

internal class JvmConstructorExtension : KmConstructorExtension {
    var signature: JvmMethodSignature? = null

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        @JvmField
        val TYPE: KmExtensionType = KmExtensionType(JvmConstructorExtension::class)
    }
}

internal class JvmTypeParameterExtension : KmTypeParameterExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        @JvmField
        val TYPE: KmExtensionType = KmExtensionType(JvmTypeParameterExtension::class)
    }
}

internal class JvmTypeExtension : KmTypeExtension {
    var isRaw: Boolean = false
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override val type: KmExtensionType
        get() = TYPE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JvmTypeExtension

        if (isRaw != other.isRaw) return false
        if (annotations != other.annotations) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isRaw.hashCode()
        result = 31 * result + annotations.hashCode()
        return result
    }

    companion object {
        @JvmField
        val TYPE: KmExtensionType = KmExtensionType(JvmTypeExtension::class)
    }
}

