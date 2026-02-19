/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Project

private const val PLUGIN_VARIANT_NAME = "gradle813"

open class KotlinPluginWrapper : AbstractKotlinPluginWrapper() {

    override val pluginVariant: String = PLUGIN_VARIANT_NAME

    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

open class KotlinAndroidPluginWrapper : AbstractKotlinAndroidPluginWrapper() {

    override val pluginVariant: String = PLUGIN_VARIANT_NAME

    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

open class KotlinMultiplatformPluginWrapper : AbstractKotlinMultiplatformPluginWrapper() {

    override val pluginVariant: String = PLUGIN_VARIANT_NAME

    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

open class KotlinJsPluginWrapper : AbstractKotlinJsPluginWrapper() {

    override val pluginVariant: String = PLUGIN_VARIANT_NAME

    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

open class KotlinApiPlugin : KotlinBaseApiPlugin() {
    override fun apply(project: Project) {
        project.registerVariantImplementations()
        super.apply(project)
    }
}

@Suppress("UnusedReceiverParameter")
private fun Project.registerVariantImplementations() {

}
