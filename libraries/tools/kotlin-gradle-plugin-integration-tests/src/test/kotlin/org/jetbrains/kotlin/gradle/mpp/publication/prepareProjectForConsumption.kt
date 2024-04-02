/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.publication

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleProject
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText
import kotlin.io.path.readText
import kotlin.io.path.writeText

fun GradleProject.prepareProjectForConsumption(
    consumer: Scenario.Project,
    dependencies: List<Scenario.Project>,
    localRepoDir: Path,
) {
    // prepend with local repository prevent unnecessary network calls
    if (consumer.gradleVersion < GradleVersion.version("8.1")) {
        buildGradleKts.writeText(
            """
        repositories {
            maven("${localRepoDir.absolutePathString().replace("\\", "\\\\")}")
        }

    """.trimIndent() + buildGradleKts.readText()
        )
    } else {
        settingsGradleKts.replaceText("""dependencyResolutionManagement {""", """
            dependencyResolutionManagement {
                repositories {
                    maven("${localRepoDir.absolutePathString().replace("\\", "\\\\")}")
                }
            
        """.trimIndent())
    }

    when (consumer.variant) {
        ProjectVariant.AndroidOnly -> prepareAndroidForConsumption(consumer, dependencies)
        ProjectVariant.JavaOnly -> prepareJavaForConsumption(consumer, dependencies)
        is ProjectVariant.Kmp -> prepareKmpForConsumption(consumer, dependencies)
    }
}

private fun GradleProject.prepareAndroidForConsumption(consumer: Scenario.Project, dependencies: List<Scenario.Project>) {
    buildGradleKts.appendText(
        """
            
            dependencies {
            ${dependencies.asDependenciesBlock()}
            }
        """.trimIndent()
    )
}

private fun List<Scenario.Project>.asDependenciesBlock(): String = joinToString("\n") {
    """   api("${it.packageName}:${it.artifactName}:1.0") """
}

private fun GradleProject.prepareJavaForConsumption(consumer: Scenario.Project, dependencies: List<Scenario.Project>) {
    buildGradleKts.appendText(
        """
            
            dependencies {
            ${dependencies.asDependenciesBlock()}
            }
        """.trimIndent()
    )
}

private fun GradleProject.prepareKmpForConsumption(consumer: Scenario.Project, dependencies: List<Scenario.Project>) {
    val projectVariant = consumer.variant
    check(projectVariant is ProjectVariant.Kmp)
    val kotlinVersion = checkNotNull(consumer.kotlinVersion)

    if (projectVariant.withJvm) {
        buildGradleKts.replaceText("// jvm() // JVM", "jvm() // JVM")
    }

    if (projectVariant.withAndroid) {
        if (kotlinVersion < KotlinToolingVersion("1.9.20")) {
            buildGradleKts.replaceText("androidTarget", "android")
        }
        buildGradleKts.replaceText("// id(\"com.android.library\") // AGP", "id(\"com.android.library\") // AGP")
        buildGradleKts.replaceText("/* Begin AGP", "// /* Begin AGP")
        buildGradleKts.replaceText("End AGP */", "// End AGP */")
    }

    val (commonMainDependencies, targetSpecificDependencies) = dependencies.partition { projectVariant.isCommonMainDependableOn(it.variant) }

    val targetsDependencies = targetSpecificDependencies.flatMap { dependency ->
        when (dependency.variant) {
            ProjectVariant.AndroidOnly -> if (projectVariant.withAndroid) listOf("android" to dependency) else emptyList()
            ProjectVariant.JavaOnly -> if (projectVariant.withAndroid || projectVariant.withJvm) listOf("jvm" to dependency) else emptyList()
            is ProjectVariant.Kmp -> (dependency.variant.variants intersect projectVariant.variants)
                .map { it to dependency }
        }
    }.groupBy(keySelector = { it.first }) { it.second }

    fun List<Scenario.Project>.asSourceSetDependenciesBlock(sourceSetName: String) = """
        
        sourceSets.getByName("$sourceSetName").dependencies {
        ${this.asDependenciesBlock()}
        }
    """.trimIndent()

    val androidMainDependencies = targetsDependencies["android"]?.asSourceSetDependenciesBlock("androidMain")
    val jvmMainDependencies = targetsDependencies["jvm"]?.asSourceSetDependenciesBlock("jvmMain")
    val linuxX64MainDependencies = targetsDependencies["native"]?.asSourceSetDependenciesBlock("linuxX64Main")
    val linuxArm64MainDependencies = targetsDependencies["native"]?.asSourceSetDependenciesBlock("linuxArm64Main")

    buildGradleKts.appendText(
        """
            
            kotlin { 
              sourceSets.getByName("commonMain").dependencies {
               ${commonMainDependencies.asDependenciesBlock()}
              }
              ${androidMainDependencies.orEmpty()}
              ${jvmMainDependencies.orEmpty()}
              ${linuxX64MainDependencies.orEmpty()}
              ${linuxArm64MainDependencies.orEmpty()}
            }           
        """.trimIndent()
    )
}