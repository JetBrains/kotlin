/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.reflect.TypeOf.typeOf
import org.jetbrains.kotlin.gradle.dsl.KotlinJsProjectExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.configureDefaultVersionsResolutionStrategy
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsSingleTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependency
import org.jetbrains.kotlin.gradle.utils.checkGradleCompatibility

interface Npm {
    operator fun invoke(packageName: String, version: String = "*"): NpmDependency

    operator fun invoke(org: String, packageName: String, version: String = "*"): NpmDependency
}

open class KotlinJsPlugin(
    private val kotlinPluginVersion: String
) : Plugin<Project> {

    override fun apply(project: Project) {
        // TODO get rid of this plugin, too? Use the 'base' plugin instead?
        // in fact, the attributes schema of the Java base plugin may be required to consume non-MPP Kotlin/JS libs,
        // so investigation is needed
        project.plugins.apply(JavaBasePlugin::class.java)

        checkGradleCompatibility()

        val npm = object : Npm {
            override fun invoke(packageName: String, version: String): NpmDependency {
                return NpmDependency(project, null, packageName, version)
            }

            override fun invoke(org: String, packageName: String, version: String): NpmDependency {
                return NpmDependency(project, org, packageName, version)
            }

        }

        (project.dependencies as ExtensionAware)
            .extensions
            .add(
                typeOf<Npm>(npm::class.java),
                "npm",
                npm
            )

        val kotlinExtension = project.kotlinExtension as KotlinJsProjectExtension
        configureDefaultVersionsResolutionStrategy(project, kotlinPluginVersion)

        val target = KotlinJsSingleTargetPreset(project, kotlinPluginVersion).createTarget("Js")

        kotlinExtension.target = target

        project.components.addAll(target.components)
    }
}