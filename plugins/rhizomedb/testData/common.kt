package com.jetbrains.rhizomedb

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

typealias EID = Int

annotation class GeneratedEntityType
annotation class Attribute
annotation class Many

interface Entity {
    val eid: EID
}

sealed class EntityAttribute<E : Entity, T : Any>()

sealed class Attributes<E : Entity>() {
    inner class Required<V : Any> : EntityAttribute<E, V>()
    inner class Optional<V : Any> : EntityAttribute<E, V>()
    inner class Many<V : Any> : EntityAttribute<E, V>()
}

abstract class EntityType<E : Entity>() : Attributes<E>() {
    fun all(): Set<E> = TODO()
    fun single(): E = TODO()
    fun singleOrNull(): E? = TODO()
}

fun <E : Entity, T : Any> entity(entityAttribute: EntityAttribute<E, T>, value: T): E? = TODO()
fun <E : Entity, T : Any> entities(entityAttribute: EntityAttribute<E, T>, value: T): Set<E> = TODO()

operator fun <E : Entity, V : Any> E.get(attribute: Attributes<E>.Required<V>): V = TODO()
operator fun <E : Entity, V : Any> E.get(attribute: Attributes<E>.Optional<V>): V? = TODO()
operator fun <E : Entity, V : Any> E.get(attribute: Attributes<E>.Many<V>): Set<V> = TODO()