/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.utils.getOrPut
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty


internal inline fun <reified T : Any> kotlinPluginService(
    crossinline factory: Project.() -> T,
): ReadOnlyProperty<Project, T> = object : ReadOnlyProperty<Project, T> {
    private val serviceKey = this::class.java.name
    override fun getValue(thisRef: Project, property: KProperty<*>): T {
        assert(property.name.startsWith("kotlin")) { "kotlinPluginService ${property.name} should start with 'kotlin' as convention" }
        return thisRef.extraProperties.getOrPut(serviceKey) {
            thisRef.factory()
        }
    }
}
