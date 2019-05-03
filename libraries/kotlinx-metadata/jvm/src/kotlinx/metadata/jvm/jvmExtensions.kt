/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm

import kotlinx.metadata.*
import kotlinx.metadata.jvm.impl.jvm

val KmClass.localDelegatedProperties: MutableList<KmProperty>
    get() = jvm.localDelegatedProperties

var KmClass.moduleName: String?
    get() = jvm.moduleName
    set(value) {
        jvm.moduleName = value
    }

var KmClass.anonymousObjectOriginName: String?
    get() = jvm.anonymousObjectOriginName
    set(value) {
        jvm.anonymousObjectOriginName = value
    }

val KmPackage.localDelegatedProperties: MutableList<KmProperty>
    get() = jvm.localDelegatedProperties

var KmPackage.moduleName: String?
    get() = jvm.moduleName
    set(value) {
        jvm.moduleName = value
    }

var KmFunction.signature: JvmMethodSignature?
    get() = jvm.signature
    set(value) {
        jvm.signature = value
    }

var KmFunction.lambdaClassOriginName: String?
    get() = jvm.lambdaClassOriginName
    set(value) {
        jvm.lambdaClassOriginName = value
    }

var KmProperty.jvmFlags: Flags
    get() = jvm.jvmFlags
    set(value) {
        jvm.jvmFlags = value
    }

var KmProperty.fieldSignature: JvmFieldSignature?
    get() = jvm.fieldSignature
    set(value) {
        jvm.fieldSignature = value
    }

var KmProperty.getterSignature: JvmMethodSignature?
    get() = jvm.getterSignature
    set(value) {
        jvm.getterSignature = value
    }

var KmProperty.setterSignature: JvmMethodSignature?
    get() = jvm.setterSignature
    set(value) {
        jvm.setterSignature = value
    }

var KmProperty.syntheticMethodForAnnotations: JvmMethodSignature?
    get() = jvm.syntheticMethodForAnnotations
    set(value) {
        jvm.syntheticMethodForAnnotations = value
    }

var KmConstructor.signature: JvmMethodSignature?
    get() = jvm.signature
    set(value) {
        jvm.signature = value
    }

val KmTypeParameter.annotations: MutableList<KmAnnotation>
    get() = jvm.annotations

var KmType.isRaw: Boolean
    get() = jvm.isRaw
    set(value) {
        jvm.isRaw = value
    }

val KmType.annotations: MutableList<KmAnnotation>
    get() = jvm.annotations
