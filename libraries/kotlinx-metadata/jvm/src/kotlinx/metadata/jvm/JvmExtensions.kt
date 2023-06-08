/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")
@file:JvmName("JvmExtensionsKt") // for stability. Probably we should drop Kt ending for easier calls from Java

package kotlinx.metadata.jvm

import kotlinx.metadata.*
import kotlinx.metadata.jvm.internal.jvm

/**
 * Metadata of local delegated properties used somewhere inside this class (but not in a nested class).
 * Note that for classes produced by the Kotlin compiler, such properties will have default accessors.
 *
 * The order of local delegated properties in this list is important. The Kotlin compiler generates the corresponding property's index
 * at the call site, so that reflection would be able to load the metadata of the property with that index at runtime.
 * If an incorrect index is used, either the `KProperty<*>` object passed to delegate methods will point to the wrong property
 * at runtime, or an exception will be thrown.
 */
val KmClass.localDelegatedProperties: MutableList<KmProperty>
    get() = jvm.localDelegatedProperties

/**
 * Name of the module where this class is declared.
 */
var KmClass.moduleName: String?
    get() = jvm.moduleName
    set(value) {
        jvm.moduleName = value
    }

/**
 * JVM internal name of the original class this anonymous object is copied from. This value is set for anonymous objects
 * copied from bodies of inline functions to the use site by the Kotlin compiler.
 */
var KmClass.anonymousObjectOriginName: String?
    get() = jvm.anonymousObjectOriginName
    set(value) {
        jvm.anonymousObjectOriginName = value
    }

/**
 * JVM-specific flags of the class, consisting of [JvmFlag.Class] flags.
 */
var KmClass.jvmFlags: Flags
    get() = jvm.jvmFlags
    set(value) {
        jvm.jvmFlags = value
    }

/**
 * Metadata of local delegated properties used somewhere inside this package fragment (but not in any class).
 * Note that for classes produced by the Kotlin compiler, such properties will have default accessors.
 *
 * The order of local delegated properties in this list is important. The Kotlin compiler generates the corresponding property's index
 * at the call site, so that reflection would be able to load the metadata of the property with that index at runtime.
 * If an incorrect index is used, either the `KProperty<*>` object passed to delegate methods will point to the wrong property
 * at runtime, or an exception will be thrown.
 */
val KmPackage.localDelegatedProperties: MutableList<KmProperty>
    get() = jvm.localDelegatedProperties

/**
 * Name of the module where this package fragment is declared.
 */
var KmPackage.moduleName: String?
    get() = jvm.moduleName
    set(value) {
        jvm.moduleName = value
    }

/**
 * JVM signature of the function, or null if the JVM signature of this function is unknown.
 *
 * Example: `JvmMethodSignature("equals", "(Ljava/lang/Object;)Z")`.
 */
var KmFunction.signature: JvmMethodSignature?
    get() = jvm.signature
    set(value) {
        jvm.signature = value
    }

/**
 * JVM internal name of the original class the lambda class for this function is copied from. This value is set for lambdas
 * copied from bodies of inline functions to the use site by the Kotlin compiler.
 */
var KmFunction.lambdaClassOriginName: String?
    get() = jvm.lambdaClassOriginName
    set(value) {
        jvm.lambdaClassOriginName = value
    }

/**
 * JVM-specific flags of the property, consisting of [JvmFlag.Property] flags.
 */
var KmProperty.jvmFlags: Flags
    get() = jvm.jvmFlags
    set(value) {
        jvm.jvmFlags = value
    }

/**
 * JVM signature of the backing field of the property, or `null` if this property has no backing field.
 *
 * Example: `JvmFieldSignature("X", "Ljava/lang/Object;")`.
 */
var KmProperty.fieldSignature: JvmFieldSignature?
    get() = jvm.fieldSignature
    set(value) {
        jvm.fieldSignature = value
    }

/**
 * JVM signature of the property getter, or `null` if this property has no getter or its signature is unknown.
 *
 * Example: `JvmMethodSignature("getX", "()Ljava/lang/Object;")`.
 */
var KmProperty.getterSignature: JvmMethodSignature?
    get() = jvm.getterSignature
    set(value) {
        jvm.getterSignature = value
    }

/**
 * JVM signature of the property setter, or `null` if this property has no setter or its signature is unknown.
 *
 * Example: `JvmMethodSignature("setX", "(Ljava/lang/Object;)V")`.
 */
var KmProperty.setterSignature: JvmMethodSignature?
    get() = jvm.setterSignature
    set(value) {
        jvm.setterSignature = value
    }

/**
 * JVM signature of a synthetic method which is generated to store annotations on a property in the bytecode.
 *
 * Example: `JvmMethodSignature("getX$annotations", "()V")`.
 */
var KmProperty.syntheticMethodForAnnotations: JvmMethodSignature?
    get() = jvm.syntheticMethodForAnnotations
    set(value) {
        jvm.syntheticMethodForAnnotations = value
    }

/**
 * JVM signature of a synthetic method for properties which delegate to another property,
 * which constructs and returns a property reference object.
 * See https://kotlinlang.org/docs/delegated-properties.html#delegating-to-another-property.
 *
 * Example: `JvmMethodSignature("getX$delegate", "()Ljava/lang/Object;")`.
 */
var KmProperty.syntheticMethodForDelegate: JvmMethodSignature?
    get() = jvm.syntheticMethodForDelegate
    set(value) {
        jvm.syntheticMethodForDelegate = value
    }

/**
 * JVM signature of the constructor, or null if the JVM signature of this constructor is unknown.
 *
 * Example: `JvmMethodSignature("<init>", "(Ljava/lang/Object;)V")`.
 */
var KmConstructor.signature: JvmMethodSignature?
    get() = jvm.signature
    set(value) {
        jvm.signature = value
    }

/**
 * Annotations on the type parameter.
 */
val KmTypeParameter.annotations: MutableList<KmAnnotation>
    get() = jvm.annotations

/**
 * `true` if the type is seen as a raw type in Java.
 */
var KmType.isRaw: Boolean
    get() = jvm.isRaw
    set(value) {
        jvm.isRaw = value
    }

/**
 * Annotations on the type.
 */
val KmType.annotations: MutableList<KmAnnotation>
    get() = jvm.annotations
