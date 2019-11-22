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
    operator fun invoke(packageName: String, version: String = "*"): NpmDependency

    operator fun invoke(org: String, packageName: String, version: String = "*"): NpmDependency
}

fun Project.addNpmDependencyExtension() {
    val dependencies = this.dependencies as ExtensionAware

    val npmDependencyExtension: NpmDependencyExtension = object : NpmDependencyExtension, Closure<NpmDependency>(dependencies) {
        override operator fun invoke(packageName: String, version: String): NpmDependency {
            return NpmDependency(this@addNpmDependencyExtension, null, packageName, version)
        }

        override operator fun invoke(org: String, packageName: String, version: String): NpmDependency {
            return NpmDependency(this@addNpmDependencyExtension, org, packageName, version)
        }

        override fun call(vararg args: Any?): NpmDependency {
            val size = args.size
            if (size > 3) throw IllegalArgumentException(
                """
                    Unable to add NPM dependency by $args
                    - npm('packageName') -> packageName:*
                    - npm('packageName', 'version') -> packageName:version
                    - npm('org', 'packageName', 'version') -> org/packageName:version
                    """.trimIndent()
            )

            if (size == 3) {
                val (org, packageName, version) = args
                    .map { it as String }

                return invoke(
                    org = org,
                    packageName = packageName,
                    version = version
                )
            }

            val packageName = args[0] as String
            val version = if (size > 1) args[1] as String else null

            return if (version != null) {
                invoke(
                    packageName = packageName,
                    version = version
                )
            } else {
                invoke(
                    packageName = packageName
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