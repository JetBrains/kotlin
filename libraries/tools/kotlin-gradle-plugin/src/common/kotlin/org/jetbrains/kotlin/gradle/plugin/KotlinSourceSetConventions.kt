/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/* Source Sets */

@ExperimentalKotlinGradlePluginApi
fun NamedDomainObjectProvider<KotlinSourceSet>.dependencies(handler: KotlinDependencyHandler.() -> Unit) = configure { sourceSet ->
    sourceSet.dependencies(handler)
}

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.commonMain: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.commonTest: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.nativeMain: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.nativeTest: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.appleMain: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.appleTest: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.iosMain: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.iosTest: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.tvosMain: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.tvosTest: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.watchosMain: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.watchosTest: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.macosMain: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.macosTest: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.linuxMain: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.linuxTest: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.mingwMain: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.mingwTest: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.androidNativeMain: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.androidNativeTest: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.jvmMain: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.jvmTest: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.jsMain: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.jsTest: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

@ExperimentalKotlinGradlePluginApi
val NamedDomainObjectContainer<KotlinSourceSet>.androidMain: NamedDomainObjectProvider<KotlinSourceSet> by SourceSetConvention

/* Helpers */

private object SourceSetConvention :
    ReadOnlyProperty<NamedDomainObjectContainer<KotlinSourceSet>, NamedDomainObjectProvider<KotlinSourceSet>> {
    override fun getValue(
        thisRef: NamedDomainObjectContainer<KotlinSourceSet>, property: KProperty<*>,
    ): NamedDomainObjectProvider<KotlinSourceSet> {
        val name = property.name
        return if (name in thisRef.names) thisRef.named(name) else thisRef.register(name)
    }
}
