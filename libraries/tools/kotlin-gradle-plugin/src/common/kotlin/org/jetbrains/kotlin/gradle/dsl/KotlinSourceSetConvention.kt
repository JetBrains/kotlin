/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.tooling.core.extrasReadWriteProperty
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Can be used to implement static conventions to access certain Source Sets:
 * Example: commonMain and commonTest conventions are implemented as:
 *
 * ```kotlin
 * val NamedDomainObjectContainer<KotlinSourceSet>.commonMain by KotlinSourceSetConvention
 *
 * val NamedDomainObjectContainer<KotlinSourceSet>.commonTest by KotlinSourceSetConvention
 * ```
 *
 * And then can be accessed in buildscripts as
 * ```kotlin
 * kotlin {
 *     sourceSets.commonMain.dependencies {
 *         // ...
 *     }
 *
 *     sourceSets.commonTest.dependencies {
 *         // ...
 *     }
 * }
 * ```
 */
@ExperimentalKotlinGradlePluginApi
object KotlinSourceSetConvention :
    ReadOnlyProperty<NamedDomainObjectContainer<KotlinSourceSet>, NamedDomainObjectProvider<KotlinSourceSet>> {

    internal class Trace : Throwable()

    override fun getValue(
        thisRef: NamedDomainObjectContainer<KotlinSourceSet>, property: KProperty<*>,
    ): NamedDomainObjectProvider<KotlinSourceSet> {
        val name = property.name
        val trace = Trace() // trace should be created eagarly to have better stacktrace
        return if (name in thisRef.names) {
            thisRef.named(name) { sourceSet ->
                if (sourceSet.isAccessedByKotlinSourceSetConventionAt == null) {
                    sourceSet.isAccessedByKotlinSourceSetConventionAt = trace
                }
            }
        } else {
            thisRef.register(name) { sourceSet ->
                sourceSet.isAccessedByKotlinSourceSetConventionAt = trace
            }
        }
    }

    /**
     * @return the stacktrace when the user was using a [KotlinSourceSetConvention] that was already created.
     * This will be null if SourceSet was never accessed by convention DSL.
     */
    @Suppress("UnusedReceiverParameter") // Diagnostic is wrong
    internal var KotlinSourceSet.isAccessedByKotlinSourceSetConventionAt: Trace?
            by extrasReadWriteProperty("isAccessedByKotlinSourceSetConventionAt")
        private set
}




