/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import groovy.lang.Closure
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformProjectConfigurator
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.executeClosure
import org.jetbrains.kotlin.gradle.plugin.mpp.*

open class KotlinMultiplatformExtension : KotlinProjectExtension() {
    val targets: KotlinTargetContainer
        get() = DslObject(this).extensions.getByType(KotlinTargetContainer::class.java)

    fun targets(configure: KotlinTargetContainer.() -> Unit) = targets.run(configure)

    fun targets(closure: Closure<*>) = targets.executeClosure(closure)

    internal lateinit var projectConfigurator: KotlinMultiplatformProjectConfigurator

    private inline fun <reified T : KotlinTarget> getOrCreateTarget(
        classifier: String,
        crossinline createExtensionIfAbsent: () -> T
    ): T {
        this as ExtensionAware
        val result = extensions.findByName(classifier) ?: createExtensionIfAbsent()
        return result as T
    }

    val common: KotlinOnlyTarget<KotlinCommonCompilation>
        get() = getOrCreateTarget("common") { projectConfigurator.createCommonExtension() }

    fun common(configure: KotlinOnlyTarget<KotlinCommonCompilation>.() -> Unit) {
        common.apply(configure)
    }

    fun common(configure: Closure<*>) = common { executeClosure(configure) }

    val jvm: KotlinOnlyTarget<KotlinJvmCompilation>
        get() = getOrCreateTarget("jvm") { projectConfigurator.createJvmExtension() }

    fun jvm(configure: KotlinOnlyTarget<KotlinJvmCompilation>.() -> Unit) {
        jvm.apply(configure)
    }

    fun jvm(configure: Closure<*>) = jvm { executeClosure(configure) }

    val jvmWithJava: KotlinWithJavaTarget
        get() = getOrCreateTarget("jvmWithJava") { projectConfigurator.createJvmWithJavaExtension() }

    fun jvmWithJava(configure: KotlinWithJavaTarget.() -> Unit) {
        jvmWithJava.apply(configure)
    }

    fun jvmWithJava(configure: Closure<*>) = jvmWithJava { executeClosure(configure) }

    val js: KotlinOnlyTarget<KotlinJsCompilation>
        get() = getOrCreateTarget("js") { projectConfigurator.createJsPlatformExtension() }

    fun js(configure: KotlinOnlyTarget<KotlinJsCompilation>.() -> Unit) {
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