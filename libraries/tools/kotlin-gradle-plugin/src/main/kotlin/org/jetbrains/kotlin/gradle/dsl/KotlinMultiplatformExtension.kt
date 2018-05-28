/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import groovy.lang.Closure
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformProjectConfigurator
import org.jetbrains.kotlin.gradle.plugin.executeClosure

open class KotlinMultiplatformExtension : KotlinProjectExtension() {
    internal lateinit var projectConfigurator: KotlinMultiplatformProjectConfigurator

    fun common(configure: Closure<*>) = common { executeClosure(configure) }

    private inline fun <reified T : KotlinPlatformExtension> getOrCreatePlatformExtension(
        classifier: String,
        crossinline createExtensionIfAbsent: () -> T
    ): T {
        this as ExtensionAware
        val result = extensions.findByName(classifier) ?: createExtensionIfAbsent()
        return result as T
    }

    fun common(configure: KotlinOnlyPlatformExtension.() -> Unit) {
        getOrCreatePlatformExtension("common") { projectConfigurator.createCommonExtension() }.apply { configure() }
    }

    fun withJava(configure: KotlinWithJavaPlatformExtension.() -> Unit) {

    }

    fun jvm(configure: Closure<*>) = jvm { executeClosure(configure) }

    fun jvm(configure: KotlinOnlyPlatformExtension.() -> Unit) {
        getOrCreatePlatformExtension("jvm") { projectConfigurator.createJvmExtension() }.apply { configure() }
    }

    fun js(configure: Closure<*>) = js { executeClosure(configure) }

    fun js(configure: KotlinOnlyPlatformExtension.() -> Unit) {
        getOrCreatePlatformExtension("js") { projectConfigurator.createJsPlatformExtension() }.apply { configure() }
    }
}