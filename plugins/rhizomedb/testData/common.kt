package com.jetbrains.rhizomedb

import kotlin.reflect.KClass

annotation class GeneratedEntityType(
    val baseType: KClass<out EntityType<*>> = EntityType::class
)

annotation class ValueAttribute(vararg val indexing: Indexing)
annotation class ReferenceAttribute(vararg val refFlags: RefFlags)

annotation class EntityConstructor