/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package gradle

import add
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin

fun Project.removeGradleApiDependencyFromTestConfiguration() {
    val javaExtension = extensions.getByType<JavaPluginExtension>()
    javaExtension.sourceSets.named("test") {
        plugins.withType<JavaGradlePluginPlugin>().configureEach {
            afterEvaluate {
                // Gradle Plugins plugin adds them in afterEvaluate
                afterEvaluate {
                    configurations[implementationConfigurationName].dependencies.remove(dependencies.gradleApi())
                    configurations[implementationConfigurationName].dependencies.remove(dependencies.gradleTestKit())
                }
            }
        }
    }
}

fun DependencyCollector.addKgpGradleApiDependency() {
    add("org.gradle.experimental:gradle-public-api:${GradlePluginVariant.GRADLE_COMMON_COMPILE_API_VERSION}") {
        capabilities {
            requireCapability("org.gradle.experimental:gradle-public-api-internal")
        }
    }
}

fun DependencyHandler.addKgpGradleApiDependency(configurationName: String) {
    add(configurationName, "org.gradle.experimental:gradle-public-api:${GradlePluginVariant.GRADLE_COMMON_COMPILE_API_VERSION}") {
        capabilities {
            requireCapability("org.gradle.experimental:gradle-public-api-internal")
        }
    }
}
