/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")
@file:JvmName("JvmExtensionsKt") // for stability. Probably we should drop Kt ending for easier calls from Java

package kotlin.metadata.jvm

import kotlin.metadata.*
import kotlin.metadata.jvm.internal.jvm

/**
 * Metadata of local delegated properties used somewhere inside this class (but not in a nested class).
 * Note that for classes produced by the Kotlin compiler, such properties will have default accessors.
 *
 * The order of local delegated properties in this list is important. The Kotlin compiler generates the corresponding property's index
 * at the call site, so that reflection would be able to load the metadata of the property with that index at runtime.
 * If an incorrect index is used, either the `KProperty<*>` object passed to delegate methods will point to the wrong property
 * at runtime, or an exception will be thrown.
 */
public val KmClass.localDelegatedProperties: MutableList<KmProperty>
    get() = jvm.localDelegatedProperties

/**
 * Name of the module where this class is declared.
 */
public var KmClass.moduleName: String?
    get() = jvm.moduleName
    set(value) {
        jvm.moduleName = value
    }

/**
 * JVM internal name of the original class this anonymous object is copied from. This value is set for anonymous objects
 * copied from bodies of inline functions to the use site by the Kotlin compiler.
 */
public var KmClass.anonymousObjectOriginName: String?
    get() = jvm.anonymousObjectOriginName
    set(value) {
        jvm.anonymousObjectOriginName = value
    }

/**
 * JVM-specific flags of the class, consisting of [JvmFlag.Class] flags.
 */
@Deprecated(
    "Flag API is deprecated. Please use corresponding member extensions on KmClass, such as KmClass.hasMethodBodiesInInterface",
    level = DeprecationLevel.ERROR
)
public var KmClass.jvmFlags: Int
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
public val KmPackage.localDelegatedProperties: MutableList<KmProperty>
    get() = jvm.localDelegatedProperties

/**
 * Name of the module where this package fragment is declared.
 */
public var KmPackage.moduleName: String?
    get() = jvm.moduleName
    set(value) {
        jvm.moduleName = value
    }

/**
 * JVM signature of the function, or null if the JVM signature of this function is unknown.
 *
 * Example: `JvmMethodSignature("equals", "(Ljava/lang/Object;)Z")`.
 */
public var KmFunction.signature: JvmMethodSignature?
    get() = jvm.signature
    set(value) {
        jvm.signature = value
    }

/**
 * JVM internal name of the original class the lambda class for this function is copied from. This value is set for lambdas
 * copied from bodies of inline functions to the use site by the Kotlin compiler.
 */
public var KmFunction.lambdaClassOriginName: String?
    get() = jvm.lambdaClassOriginName
    set(value) {
        jvm.lambdaClassOriginName = value
    }

/**
 * JVM-specific flags of the property, consisting of [JvmFlag.Property] flags.
 */
@Deprecated(
    "Flag API is deprecated. Please use corresponding member extensions on KmProperty, such as KmProperty.isMovedFromInterfaceCompanion",
    level = DeprecationLevel.ERROR
)
public var KmProperty.jvmFlags: Int
    get() = jvm.jvmFlags
    set(value) {
        jvm.jvmFlags = value
    }

/**
 * JVM signature of the backing field of the property, or `null` if this property has no backing field.
 *
 * Example: `JvmFieldSignature("X", "Ljava/lang/Object;")`.
 */
public var KmProperty.fieldSignature: JvmFieldSignature?
    get() = jvm.fieldSignature
    set(value) {
        jvm.fieldSignature = value
    }

/**
 * JVM signature of the property getter, or `null` if this property has no getter or its signature is unknown.
 *
 * Example: `JvmMethodSignature("getX", "()Ljava/lang/Object;")`.
 */
public var KmProperty.getterSignature: JvmMethodSignature?
    get() = jvm.getterSignature
    set(value) {
        jvm.getterSignature = value
    }

/**
 * JVM signature of the property setter, or `null` if this property has no setter or its signature is unknown.
 *
 * Example: `JvmMethodSignature("setX", "(Ljava/lang/Object;)V")`.
 */
public var KmProperty.setterSignature: JvmMethodSignature?
    get() = jvm.setterSignature
    set(value) {
        jvm.setterSignature = value
    }

/**
 * JVM signature of a synthetic method which is generated to store annotations on a property in the bytecode.
 *
 * Example: `JvmMethodSignature("getX$annotations", "()V")`.
 */
public var KmProperty.syntheticMethodForAnnotations: JvmMethodSignature?
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
public var KmProperty.syntheticMethodForDelegate: JvmMethodSignature?
    get() = jvm.syntheticMethodForDelegate
    set(value) {
        jvm.syntheticMethodForDelegate = value
    }

/**
 * JVM signature of the constructor, or null if the JVM signature of this constructor is unknown.
 *
 * Example: `JvmMethodSignature("<init>", "(Ljava/lang/Object;)V")`.
 */
public var KmConstructor.signature: JvmMethodSignature?
    get() = jvm.signature
    set(value) {
        jvm.signature = value
    }

/**
 * Annotations on the type parameter.
 */
public val KmTypeParameter.annotations: MutableList<KmAnnotation>
    get() = jvm.annotations

/**
 * `true` if the type is seen as a raw type in Java.
 */
public var KmType.isRaw: Boolean
    get() = jvm.isRaw
    set(value) {
        jvm.isRaw = value
    }

/**
 * Annotations on the type.
 */
public val KmType.annotations: MutableList<KmAnnotation>
    get() = jvm.annotations
