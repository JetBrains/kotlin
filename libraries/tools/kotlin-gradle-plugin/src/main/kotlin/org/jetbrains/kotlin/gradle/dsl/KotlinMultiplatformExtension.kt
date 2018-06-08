/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import groovy.lang.Closure
import org.gradle.api.Named
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformProjectConfigurator
import org.jetbrains.kotlin.gradle.plugin.executeClosure
import java.io.Serializable

open class KotlinMultiplatformExtension : KotlinProjectExtension() {
    internal lateinit var projectConfigurator: KotlinMultiplatformProjectConfigurator

    private inline fun <reified T : KotlinPlatformExtension> getOrCreatePlatformExtension(
        classifier: String,
        crossinline createExtensionIfAbsent: () -> T
    ): T {
        this as ExtensionAware
        val result = extensions.findByName(classifier) ?: createExtensionIfAbsent()
        return result as T
    }

    val common: KotlinOnlyPlatformExtension
        get() = getOrCreatePlatformExtension("common") { projectConfigurator.createCommonExtension() }

    fun common(configure: KotlinOnlyPlatformExtension.() -> Unit) {
        common.apply(configure)
    }

    fun common(configure: Closure<*>) = common { executeClosure(configure) }

    val jvm: KotlinMppPlatformExtension
        get() = getOrCreatePlatformExtension("jvm") { projectConfigurator.createJvmExtension() }

    fun jvm(configure: KotlinMppPlatformExtension.() -> Unit) {
        jvm.apply(configure)
    }

    fun jvm(configure: Closure<*>) = jvm { executeClosure(configure) }

    val jvmWithJava: KotlinWithJavaPlatformExtension
        get() = getOrCreatePlatformExtension("jvmWithJava") { projectConfigurator.createJvmWithJavaExtension() }

    fun jvmWithJava(configure: KotlinWithJavaPlatformExtension.() -> Unit) {
        jvmWithJava.apply(configure)
    }

    fun jvmWithJava(configure: Closure<*>) = jvmWithJava { executeClosure(configure) }

    val js: KotlinMppPlatformExtension
        get() = getOrCreatePlatformExtension("js") { projectConfigurator.createJsPlatformExtension() }

    fun js(configure: KotlinMppPlatformExtension.() -> Unit) {
        js.apply(configure)
    }

    fun js(configure: Closure<*>) = js { executeClosure(configure) }
}

open class KotlinMppPlatformExtension : KotlinOnlyPlatformExtension() {
    internal lateinit var projectConfigurator: KotlinMultiplatformProjectConfigurator

    fun expectedBy(modulePath: String) {
        projectConfigurator.addExternalExpectedByModule(this, modulePath)
    }
}

enum class KotlinPlatformType(val displayName: String): Named, Serializable {
    COMMON("common"), JVM("jvm"), JS("js"), NATIVE("native");

    override fun toString(): String = displayName
    override fun getName(): String = displayName
}