/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    `kotlin-dsl`
}

description = "Conventions for use in settings.gradle.kts scripts"

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.gradlePlugin.gradle.develocity)
    implementation(libs.gradlePlugin.gradle.customUserData)
    implementation(libs.gradlePlugin.gradle.foojayToolchains)
}


//region check consistent develocity version

val checkBuildSettingsLogicPluginConsistency by tasks.registering {
    description = "Check consistency of plugin versions in settings.gradle.kts with version catalog."
    group = LifecycleBasePlugin.VERIFICATION_GROUP

    val customUserDataVersion = libs.versions.gradlePlugin.gradle.customUserData
    val develocityVersion = libs.versions.gradlePlugin.gradle.develocity
    val foojayToolchainsVersion = libs.versions.gradlePlugin.gradle.foojayToolchains

    inputs.property("customUserDataVersionCatalogVersion", customUserDataVersion)
    inputs.property("develocityVersionCatalogVersion", develocityVersion)
    inputs.property("foojayToolchainsVersionCatalogVersion", foojayToolchainsVersion)

    val settingsGradleKtsInput = layout.projectDirectory.file("settings.gradle.kts")
    inputs.file(settingsGradleKtsInput)
        .withPropertyName("settingsGradleKtsInput")
        .normalizeLineEndings()
        .withPathSensitivity(PathSensitivity.RELATIVE)

    val projectName = project.displayName

    doLast {
        val pluginVersionsMap =
            settingsGradleKtsInput.asFile.useLines { lines ->
                lines
                    .dropWhile { it != "plugins {" }
                    .takeWhile { it != "}" }
                    .filter { it.startsWith("    id(") }
                    .associate {
                        val id = it.substringAfter("id(\"")
                            .substringBefore("\")")
                        val version = it.substringAfter("version \"")
                            .substringBefore("\"")
                        id to version
                    }
            }

        val result = buildString {
            fun checkVersion(id: String, version: String) {
                val actualVersion = pluginVersionsMap[id]
                when {
                    actualVersion == null ->
                        appendLine("plugin $id: missing")

                    actualVersion != version ->
                        appendLine("plugin $id: expected $version, actual $actualVersion")
                }
            }

            checkVersion("com.gradle.develocity", develocityVersion.get())
            checkVersion("com.gradle.common-custom-user-data-gradle-plugin", customUserDataVersion.get())
            checkVersion("org.gradle.toolchains.foojay-resolver-convention", foojayToolchainsVersion.get())
        }

        check(result.isEmpty()) {
            """
            $projectName - inconsistent plugin versions:
            ${result.prependIndent()}
            """.trimIndent()
        }
    }
}

tasks.compileKotlin {
    // pick a task that always runs to depend on the check, to make sure it always runs
    dependsOn(checkBuildSettingsLogicPluginConsistency)
}

//endregion
