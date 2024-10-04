/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.plugin.sandbox

import kotlin.reflect.KClass

annotation class AllOpen
annotation class AllOpen2

annotation class DummyFunction

@Target(AnnotationTarget.FUNCTION)
annotation class TestTopLevelPrivateSuspendFun
annotation class ExternalClassWithNested
annotation class NestedClassAndMaterializeMember
annotation class MyInterfaceSupertype
annotation class CompanionWithFoo

annotation class MySerializable
annotation class CoreSerializer

annotation class AllPublic(val visibility: Visibility)

@Target(AnnotationTarget.TYPE)
annotation class Positive

@Target(AnnotationTarget.TYPE)
annotation class Negative

enum class Visibility {
    Public, Internal, Private, Protected
}

annotation class SupertypeWithTypeArgument(val kClass: KClass<*>)

annotation class MetaSupertype

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.PROPERTY_GETTER)
annotation class MyInlineable

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.PROPERTY_GETTER)
annotation class MyNotInlineable

annotation class AllPropertiesConstructor

annotation class AddAnnotations

@Retention(AnnotationRetention.RUNTIME)
annotation class SimpleAnnotation(val x: Int)

@Retention(AnnotationRetention.RUNTIME)
annotation class ArrayAnnotation(val annotations: Array<SimpleAnnotation>)

@Retention(AnnotationRetention.RUNTIME)
annotation class AnnotationToAdd(
    val booleanValue: Boolean,
    val byteValue: Byte,
    val charValue: Char,
    val doubleValue: Double,
    val floatValue: Float,
    val intValue: Int,
    val longValue: Long,
    val shortValue: Short,
    val stringValue: String,
    vararg val vararg: ArrayAnnotation
)

annotation class AddNestedClassesBasedOnArgument(val kClass: KClass<*>)

annotation class AddNestedGeneratedClass

annotation class GeneratedEntityType

@Retention(AnnotationRetention.SOURCE)
annotation class EmitMetadata(val value: Int)

@Retention(AnnotationRetention.SOURCE)
annotation class GenerateBodyUsingEmittedMetadata
