/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp.publication

import org.jetbrains.kotlin.gradle.testbase.GradleProject
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import kotlin.io.path.appendText

fun GradleProject.prepareProjectForPublication(
    scenarioProject: Scenario.Project,
) {
    buildGradleKts.appendText("\n" + """group = "${scenarioProject.packageName}" """)
    buildGradleKts.appendText("\n" + """version = "1.0" """)
    settingsGradleKts.appendText("\n" + """rootProject.name = "${scenarioProject.artifactName}" """)

    when (scenarioProject.variant) {
        ProjectVariant.AndroidOnly -> prepareAndroidOnlyProjectForPublication(scenarioProject)
        ProjectVariant.JavaOnly -> Unit
        is ProjectVariant.Kmp -> prepareKmpProjectForPublication(scenarioProject)
    }
}

private fun GradleProject.prepareAndroidOnlyProjectForPublication(
    scenarioProject: Scenario.Project
) {
    generateCode(scenarioProject, "main")
}

private fun GradleProject.prepareKmpProjectForPublication(
    scenarioProject: Scenario.Project,
) {
    val projectVariant = scenarioProject.variant
    check(projectVariant is ProjectVariant.Kmp)
    val kotlinVersion = checkNotNull(scenarioProject.kotlinVersion)

    generateCode(scenarioProject, "commonMain")
    generateCode(scenarioProject, "nativeMain")

    if (projectVariant.withJvm) {
        buildGradleKts.replaceText("// jvm() // JVM", "jvm() // JVM")
        generateCode(scenarioProject, "jvmMain")
    }

    if (projectVariant.withAndroid) {
        if (kotlinVersion < KotlinToolingVersion("1.9.20")) {
            buildGradleKts.replaceText("androidTarget", "android")
        }
        buildGradleKts.replaceText("// id(\"com.android.library\") // AGP", "id(\"com.android.library\") // AGP")
        buildGradleKts.replaceText("/* Begin AGP", "// /* Begin AGP")
        buildGradleKts.replaceText("End AGP */", "// End AGP */")

        generateCode(scenarioProject, "androidMain")
    }
}


private fun GradleProject.generateCode(scenarioProject: Scenario.Project, name: String) {
    val file = projectPath.resolve("src/$name/kotlin/$name.kt").toFile()
    file.parentFile.mkdirs()
    file.writeText(
        """
        package ${scenarioProject.packageName}.${scenarioProject.artifactName}
        
        fun $name() = "$name"
    """.trimIndent())
}
