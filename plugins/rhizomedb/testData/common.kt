package com.jetbrains.rhizomedb

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class GeneratedEntityType(vararg val mixins: KClass<out Mixin<*>>)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class EntityConstructor

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class ValueAttribute(vararg val flags: Indexing)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class TransientAttribute(vararg val flags: Indexing)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class RefAttribute(vararg val flags: RefFlags)