/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlinx.metadata.klib.impl.klibExtensions
import kotlin.metadata.*
import kotlin.metadata.internal.common.KmModuleFragment

var KmFunction.file: KlibSourceFile?
    get() = klibExtensions.file
    set(value) {
        klibExtensions.file = value
    }

var KmFunction.kdoc: String?
    get() = klibExtensions.kdoc
    set(value) {
        klibExtensions.kdoc = value
    }

var KmClass.file: KlibSourceFile?
    get() = klibExtensions.file
    set(value) {
        klibExtensions.file = value
    }

var KmClass.kdoc: String?
    get() = klibExtensions.kdoc
    set(value) {
        klibExtensions.kdoc = value
    }

var KmProperty.file: KlibSourceFile?
    get() = klibExtensions.file
    set(value) {
        klibExtensions.file = value
    }

var KmProperty.compileTimeValue: KmAnnotationArgument?
    get() = klibExtensions.compileTimeValue
    set(value) {
        klibExtensions.compileTimeValue = value
    }

var KmProperty.kdoc: String?
    get() = klibExtensions.kdoc
    set(value) {
        klibExtensions.kdoc = value
    }

var KmProperty.getterKdoc: String?
    get() = klibExtensions.getterKdoc
    set(value) {
        klibExtensions.getterKdoc = value
    }

var KmProperty.setterKdoc: String?
    get() = klibExtensions.setterKdoc
    set(value) {
        klibExtensions.setterKdoc = value
    }

val KmType.annotations: MutableList<KmAnnotation>
    get() = klibExtensions.annotations

var KmPackage.fqName: String?
    get() = klibExtensions.fqName
    set(value) {
        klibExtensions.fqName = value
    }

var KmModuleFragment.fqName: String?
    get() = klibExtensions.fqName
    set(value) {
        klibExtensions.fqName = value
    }

val KmModuleFragment.className: MutableList<ClassName>
    get() = klibExtensions.className

val KmTypeParameter.annotations: MutableList<KmAnnotation>
    get() = klibExtensions.annotations

var KmEnumEntry.ordinal: Int?
    get() = klibExtensions.ordinal
    set(value) {
        klibExtensions.ordinal = value
    }

var KmEnumEntry.kdoc: String?
    get() = klibExtensions.kdoc
    set(value) {
        klibExtensions.kdoc = value
    }

var KmTypeAlias.kdoc: String?
    get() = klibExtensions.kdoc
    set(value) {
        klibExtensions.kdoc = value
    }

var KmConstructor.kdoc: String?
    get() = klibExtensions.kdoc
    set(value) {
        klibExtensions.kdoc = value
    }
