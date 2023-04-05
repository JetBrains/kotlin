/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib

import kotlinx.metadata.*
import kotlinx.metadata.internal.common.KmModuleFragment
import kotlinx.metadata.klib.impl.klibExtensions

val KmFunction.annotations: MutableList<KmAnnotation>
    get() = klibExtensions.annotations

var KmFunction.uniqId: UniqId?
    get() = klibExtensions.uniqId
    set(value) {
        klibExtensions.uniqId = value
    }

var KmFunction.file: KlibSourceFile?
    get() = klibExtensions.file
    set(value) {
        klibExtensions.file = value
    }

val KmClass.annotations: MutableList<KmAnnotation>
    get() = klibExtensions.annotations

var KmClass.uniqId: UniqId?
    get() = klibExtensions.uniqId
    set(value) {
        klibExtensions.uniqId = value
    }

var KmClass.file: KlibSourceFile?
    get() = klibExtensions.file
    set(value) {
        klibExtensions.file = value
    }

val KmClass.klibEnumEntries: MutableList<KlibEnumEntry>
    get() = klibExtensions.enumEntries

val KmProperty.annotations: MutableList<KmAnnotation>
    get() = klibExtensions.annotations

val KmProperty.setterAnnotations: MutableList<KmAnnotation>
    get() = klibExtensions.setterAnnotations

val KmProperty.getterAnnotations: MutableList<KmAnnotation>
    get() = klibExtensions.getterAnnotations

var KmProperty.uniqId: UniqId?
    get() = klibExtensions.uniqId
    set(value) {
        klibExtensions.uniqId = value
    }

var KmProperty.file: Int?
    get() = klibExtensions.file
    set(value) {
        klibExtensions.file = value
    }

var KmProperty.compileTimeValue: KmAnnotationArgument?
    get() = klibExtensions.compileTimeValue
    set(value) {
        klibExtensions.compileTimeValue = value
    }

val KmType.annotations: MutableList<KmAnnotation>
    get() = klibExtensions.annotations

val KmConstructor.annotations: MutableList<KmAnnotation>
    get() = klibExtensions.annotations

var KmConstructor.uniqId: UniqId?
    get() = klibExtensions.uniqId
    set(value) {
        klibExtensions.uniqId = value
    }

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

val KmModuleFragment.moduleFragmentFiles: MutableList<KlibSourceFile>
    get() = klibExtensions.moduleFragmentFiles

val KmTypeParameter.annotations: MutableList<KmAnnotation>
    get() = klibExtensions.annotations

var KmTypeParameter.uniqId: UniqId?
    get() = klibExtensions.uniqId
    set(value) {
        klibExtensions.uniqId = value
    }

var KmTypeAlias.uniqId: UniqId?
    get() = klibExtensions.uniqId
    set(value) {
        klibExtensions.uniqId = value
    }

val KmValueParameter.annotations: MutableList<KmAnnotation>
    get() = klibExtensions.annotations
