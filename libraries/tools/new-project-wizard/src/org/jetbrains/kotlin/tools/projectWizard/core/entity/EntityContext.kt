package org.jetbrains.kotlin.tools.projectWizard.core.entity

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class EntityContext<E : Entity, ER : EntityReference> {
    private val entities = mutableMapOf<ER, E>()

    open fun addEntity(entityReference: ER, createEntityByName: (String) -> E): E =
        createEntityByName(entityReference.original.path).also {
            entities[entityReference.original] = it
        }

    fun getEntity(entityReference: ER): E? = entities[entityReference.original]
    fun getAll(): List<E> = entities.values.toList()
}

abstract class ValuedEntityContext<E : EntityWithValue<Any>, ER : EntityReference> : EntityContext<E, ER>() {
    private val values = mutableMapOf<String, Any>()

    operator fun get(entityReference: ER) =
        values[entityReference.original.path]

    operator fun get(entityPath: String) =
        values[entityPath]

    open operator fun set(entityReference: ER, value: Any) {
        values[entityReference.original.path] = value
    }

    open operator fun set(entityPath: String, value: Any) {
        values[entityPath] = value
    }

    val allValues: Map<String, Any> = values
}

@Suppress("UNCHECKED_CAST")
fun <E : Entity, A : E, ER : EntityReference> entityDelegate(
    entityContext: EntityContext<E, ER>,
    createEntityByPath: (String) -> A
) = object : ReadOnlyProperty<Any, A> {
    override fun getValue(thisRef: Any, property: KProperty<*>): A =
        entityContext.getEntity(property as ER) as? A
            ?: entityContext.addEntity(property, createEntityByPath) as A
}

