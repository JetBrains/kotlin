/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlinx.metadata.klib.impl

import kotlinx.metadata.klib.KlibEnumEntry
import kotlinx.metadata.klib.KlibSourceFile
import kotlinx.metadata.klib.UniqId
import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment
import kotlin.metadata.internal.extensions.*

internal val KmFunction.klibExtensions: KlibFunctionExtension
    get() = getExtension(KlibFunctionExtension.TYPE) as KlibFunctionExtension

internal val KmClass.klibExtensions: KlibClassExtension
    get() = getExtension(KlibClassExtension.TYPE) as KlibClassExtension

internal val KmType.klibExtensions: KlibTypeExtension
    get() = getExtension(KlibTypeExtension.TYPE) as KlibTypeExtension

internal val KmProperty.klibExtensions: KlibPropertyExtension
    get() = getExtension(KlibPropertyExtension.TYPE) as KlibPropertyExtension

internal val KmConstructor.klibExtensions: KlibConstructorExtension
    get() = getExtension(KlibConstructorExtension.TYPE) as KlibConstructorExtension

internal val KmTypeParameter.klibExtensions: KlibTypeParameterExtension
    get() = getExtension(KlibTypeParameterExtension.TYPE) as KlibTypeParameterExtension

internal val KmPackage.klibExtensions: KlibPackageExtension
    get() = getExtension(KlibPackageExtension.TYPE) as KlibPackageExtension

internal val KmModuleFragment.klibExtensions: KlibModuleFragmentExtension
    get() = getExtension(KlibModuleFragmentExtension.TYPE) as KlibModuleFragmentExtension

internal val KmTypeAlias.klibExtensions: KlibTypeAliasExtension
    get() = getExtension(KlibTypeAliasExtension.TYPE) as KlibTypeAliasExtension

internal class KlibFunctionExtension : KmFunctionExtension {
    var uniqId: UniqId? = null
    var file: KlibSourceFile? = null

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibFunctionExtension::class)
    }
}

internal class KlibClassExtension : KmClassExtension {
    val enumEntries: MutableList<KlibEnumEntry> = mutableListOf()
    var uniqId: UniqId? = null
    var file: KlibSourceFile? = null

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibClassExtension::class)
    }
}

internal class KlibTypeExtension : KmTypeExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override fun hashCode(): Int {
        return annotations.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KlibTypeExtension

        return annotations == other.annotations
    }

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibTypeExtension::class)
    }
}

internal class KlibPropertyExtension : KmPropertyExtension {
    var uniqId: UniqId? = null
    var file: Int? = null
    var compileTimeValue: KmAnnotationArgument? = null

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibPropertyExtension::class)
    }
}

internal class KlibConstructorExtension : KmConstructorExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()
    var uniqId: UniqId? = null

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibConstructorExtension::class)
    }
}

internal class KlibTypeParameterExtension : KmTypeParameterExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()
    var uniqId: UniqId? = null

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibTypeParameterExtension::class)
    }
}

internal class KlibPackageExtension : KmPackageExtension {
    var fqName: String? = null

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibPackageExtension::class)
    }
}

internal class KlibModuleFragmentExtension : KmModuleFragmentExtension {
    val moduleFragmentFiles: MutableList<KlibSourceFile> = ArrayList()
    var fqName: String? = null
    val className: MutableList<ClassName> = ArrayList()

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibModuleFragmentExtension::class)
    }
}

internal class KlibTypeAliasExtension : KmTypeAliasExtension {
    var uniqId: UniqId? = null

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibTypeAliasExtension::class)
    }
}

internal class KlibValueParameterExtension : KmValueParameterExtension {
    val annotations: MutableList<KmAnnotation> = mutableListOf()

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibValueParameterExtension::class)
    }
}
