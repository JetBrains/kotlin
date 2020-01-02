package org.jetbrains.kotlin.tools.projectWizard.core.entity

import org.jetbrains.kotlin.tools.projectWizard.SettingsOwner
import org.jetbrains.kotlin.tools.projectWizard.core.Plugin
import org.jetbrains.kotlin.tools.projectWizard.core.PluginReference
import org.jetbrains.kotlin.tools.projectWizard.core.path
import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaGetter

interface Entity {
    val path: String
}
abstract class EntityBase: Entity {
    final override fun equals(other: Any?): Boolean = other.safeAs<Entity>()?.path == path
    final override fun hashCode(): Int = path.hashCode()
    final override fun toString(): String = path
}

abstract class EntityWithValue<out T : Any> : EntityBase()

typealias EntityReference = KProperty1<out SettingsOwner, Entity>

val EntityReference.path
    get() = "${plugin.path}.$name"

@Suppress("UNCHECKED_CAST")
val <EP : EntityReference> EP.original
    get() = plugin.declaredMemberProperties.first { it.name == name } as EP

@Suppress("UNCHECKED_CAST")
val EntityReference.plugin: PluginReference
    get() = javaGetter!!.declaringClass.kotlin as PluginReference

