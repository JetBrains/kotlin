/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.reflect.TypeOf

interface NpmDependencyExtension {
    operator fun invoke(name: String, version: String = "*"): NpmDependency
}

fun Project.addNpmDependencyExtension() {
    val dependencies = this.dependencies as ExtensionAware

    val npmDependencyExtension: NpmDependencyExtension = object : NpmDependencyExtension, Closure<NpmDependency>(dependencies) {
        override operator fun invoke(name: String, version: String): NpmDependency {
            return NpmDependency(this@addNpmDependencyExtension, name, version)
        }

        override fun call(vararg args: Any?): NpmDependency {
            val size = args.size
            if (size > 2) throw IllegalArgumentException(
                """
                    Unable to add NPM dependency by $args
                    - npm('name') -> name:*
                    - npm('name', 'version') -> name:version
                    """.trimIndent()
            )

            val name = args[0] as String
            val version = if (size > 1) args[1] as String else null

            return if (version != null) {
                invoke(
                    name = name,
                    version = version
                )
            } else {
                invoke(
                    name = name
                )
            }
        }
    }

    dependencies
        .extensions
        .add(
            TypeOf.typeOf<NpmDependencyExtension>(NpmDependencyExtension::class.java),
            "npm",
            npmDependencyExtension
        )
}