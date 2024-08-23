/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.metadata.internal.common

import kotlin.metadata.*
import kotlin.metadata.internal.extensions.*

internal val KmClass.builtins: BuiltInClassExtension
    get() = getExtension(BuiltInClassExtension.TYPE) as BuiltInClassExtension

internal val KmPackage.builtins: BuiltInPackageExtension
    get() = getExtension(BuiltInPackageExtension.TYPE) as BuiltInPackageExtension

internal val KmFunction.builtins: BuiltInFunctionExtension
    get() = getExtension(BuiltInFunctionExtension.TYPE) as BuiltInFunctionExtension

internal val KmProperty.builtins: BuiltInPropertyExtension
    get() = getExtension(BuiltInPropertyExtension.TYPE) as BuiltInPropertyExtension

internal val KmConstructor.builtins: BuiltInConstructorExtension
    get() = getExtension(BuiltInConstructorExtension.TYPE) as BuiltInConstructorExtension

// TODO (KT-71235): annotations on enum entries

internal val KmValueParameter.builtins: BuiltInValueParameterExtension
    get() = getExtension(BuiltInValueParameterExtension.TYPE) as BuiltInValueParameterExtension

internal val KmTypeParameter.builtins: BuiltInTypeParameterExtension
    get() = getExtension(BuiltInTypeParameterExtension.TYPE) as BuiltInTypeParameterExtension

internal val KmType.builtins: BuiltInTypeExtension
    get() = getExtension(BuiltInTypeExtension.TYPE) as BuiltInTypeExtension

internal class BuiltInClassExtension : KmClassExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(BuiltInClassExtension::class)
    }
}

internal class BuiltInPackageExtension : KmPackageExtension {
    var fqName: String? = null

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(BuiltInPackageExtension::class)
    }
}

internal class BuiltInFunctionExtension : KmFunctionExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(BuiltInFunctionExtension::class)
    }
}

internal class BuiltInPropertyExtension : KmPropertyExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()
    val getterAnnotations: MutableList<KmAnnotation> = mutableListOf()
    val setterAnnotations: MutableList<KmAnnotation> = mutableListOf()
    var compileTimeValue: KmAnnotationArgument? = null

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(BuiltInPropertyExtension::class)
    }
}

internal class BuiltInConstructorExtension : KmConstructorExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(BuiltInConstructorExtension::class)
    }
}

internal class BuiltInValueParameterExtension : KmValueParameterExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(BuiltInValueParameterExtension::class)
    }
}

internal class BuiltInTypeParameterExtension : KmTypeParameterExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(BuiltInTypeParameterExtension::class)
    }
}

internal class BuiltInTypeExtension : KmTypeExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override val type: KmExtensionType
        get() = TYPE

    override fun hashCode(): Int =
        annotations.hashCode()

    override fun equals(other: Any?): Boolean =
        other is BuiltInTypeExtension && annotations == other.annotations

    companion object {
        val TYPE: KmExtensionType = KmExtensionType(BuiltInTypeExtension::class)
    }
}
