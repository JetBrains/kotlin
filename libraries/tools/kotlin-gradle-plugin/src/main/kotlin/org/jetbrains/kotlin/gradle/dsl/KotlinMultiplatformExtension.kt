/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import groovy.lang.Closure
import org.gradle.api.Named
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformProjectConfigurator
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.executeClosure
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import java.io.Serializable

open class KotlinMultiplatformExtension : KotlinProjectExtension() {
    internal lateinit var projectConfigurator: KotlinMultiplatformProjectConfigurator

    private inline fun <reified T : KotlinTarget> getOrCreateTarget(
        classifier: String,
        crossinline createExtensionIfAbsent: () -> T
    ): T {
        this as ExtensionAware
        val result = extensions.findByName(classifier) ?: createExtensionIfAbsent()
        return result as T
    }

    val common: KotlinOnlyTarget
        get() = getOrCreateTarget("common") { projectConfigurator.createCommonExtension() }

    fun common(configure: KotlinOnlyTarget.() -> Unit) {
        common.apply(configure)
    }

    fun common(configure: Closure<*>) = common { executeClosure(configure) }

    val jvm: KotlinMppTarget
        get() = getOrCreateTarget("jvm") { projectConfigurator.createJvmExtension() }

    fun jvm(configure: KotlinMppTarget.() -> Unit) {
        jvm.apply(configure)
    }

    fun jvm(configure: Closure<*>) = jvm { executeClosure(configure) }

    val jvmWithJava: KotlinWithJavaTarget
        get() = getOrCreateTarget("jvmWithJava") { projectConfigurator.createJvmWithJavaExtension() }

    fun jvmWithJava(configure: KotlinWithJavaTarget.() -> Unit) {
        jvmWithJava.apply(configure)
    }

    fun jvmWithJava(configure: Closure<*>) = jvmWithJava { executeClosure(configure) }

    val js: KotlinMppTarget
        get() = getOrCreateTarget("js") { projectConfigurator.createJsPlatformExtension() }

    fun js(configure: KotlinMppTarget.() -> Unit) {
        js.apply(configure)
    }

    fun js(configure: Closure<*>) = js { executeClosure(configure) }

    val android: KotlinAndroidTarget
        get() = getOrCreateTarget("android") { projectConfigurator.createAndroidPlatformExtension() }

    fun android(configure: KotlinAndroidTarget.() -> Unit) {
        android.apply(configure)
    }

    fun android(configure: Closure<*>) = android { executeClosure(configure) }
}

open class KotlinMppTarget(
    project: Project,
    projectExtension: KotlinProjectExtension
) : KotlinOnlyTarget(project, projectExtension) {
    internal lateinit var projectConfigurator: KotlinMultiplatformProjectConfigurator

    fun expectedBy(modulePath: String) {
        projectConfigurator.addExternalExpectedByModule(this, modulePath)
    }
}