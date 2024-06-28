/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlinx.metadata.klib

import kotlin.metadata.*
import kotlin.metadata.internal.extensions.*

abstract class KlibFunctionExtensionVisitor : KmFunctionExtension {

    abstract fun visitAnnotation(annotation: KmAnnotation)

    abstract fun visitUniqId(uniqId: UniqId)

    abstract fun visitFile(file: KlibSourceFile)

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibFunctionExtensionVisitor::class)
    }
}

abstract class KlibClassExtensionVisitor : KmClassExtension {

    abstract fun visitAnnotation(annotation: KmAnnotation)

    abstract fun visitUniqId(uniqId: UniqId)

    abstract fun visitFile(file: KlibSourceFile)

    abstract fun visitEnumEntry(entry: KlibEnumEntry)

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibClassExtensionVisitor::class)
    }
}

abstract class KlibTypeExtensionVisitor : KmTypeExtension {

    abstract fun visitAnnotation(annotation: KmAnnotation)

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibTypeExtensionVisitor::class)
    }
}

abstract class KlibPropertyExtensionVisitor : KmPropertyExtension {

    abstract fun visitAnnotation(annotation: KmAnnotation)

    abstract fun visitGetterAnnotation(annotation: KmAnnotation)

    abstract fun visitSetterAnnotation(annotation: KmAnnotation)

    abstract fun visitFile(file: Int)

    abstract fun visitUniqId(uniqId: UniqId)

    abstract fun visitCompileTimeValue(value: KmAnnotationArgument)

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibPropertyExtensionVisitor::class)
    }
}

abstract class KlibConstructorExtensionVisitor : KmConstructorExtension {

    abstract fun visitAnnotation(annotation: KmAnnotation)

    abstract fun visitUniqId(uniqId: UniqId)

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibConstructorExtensionVisitor::class)
    }
}

abstract class KlibTypeParameterExtensionVisitor : KmTypeParameterExtension {

    abstract fun visitAnnotation(annotation: KmAnnotation)

    abstract fun visitUniqId(uniqId: UniqId)

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibTypeParameterExtensionVisitor::class)
    }
}

abstract class KlibPackageExtensionVisitor : KmPackageExtension {

    abstract fun visitFqName(name: String)

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibPackageExtensionVisitor::class)
    }
}

abstract class KlibModuleFragmentExtensionVisitor : KmModuleFragmentExtension {

    abstract fun visitFile(file: KlibSourceFile)

    abstract fun visitFqName(fqName: String)

    abstract fun visitClassName(className: ClassName)

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibModuleFragmentExtensionVisitor::class)
    }
}

abstract class KlibTypeAliasExtensionVisitor : KmTypeAliasExtension {

    abstract fun visitUniqId(uniqId: UniqId)

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibTypeAliasExtensionVisitor::class)
    }
}

abstract class KlibValueParameterExtensionVisitor : KmValueParameterExtension {

    abstract fun visitAnnotation(annotation: KmAnnotation)

    override val type: KmExtensionType
        get() = TYPE

    companion object {
        val TYPE = KmExtensionType(KlibValueParameterExtensionVisitor::class)
    }
}
