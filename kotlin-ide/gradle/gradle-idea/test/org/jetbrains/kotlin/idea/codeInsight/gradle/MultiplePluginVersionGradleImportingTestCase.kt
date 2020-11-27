/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * This TestCase implements possibility to test import with different versions of gradle and different
 * versions of gradle kotlin plugin
 */
package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.openapi.vfs.VirtualFile
import org.junit.Rule
import org.junit.runners.Parameterized


abstract class MultiplePluginVersionGradleImportingTestCase : KotlinGradleImportingTestCase() {
    @Rule
    @JvmField
    var pluginVersionMatchingRule = PluginTargetVersionsRule()

    @JvmField
    @Parameterized.Parameter(1)
    var gradleKotlinPluginVersion: String = ""


    companion object {

        @JvmStatic
        @Suppress("ACCIDENTAL_OVERRIDE")
        @Parameterized.Parameters(name = "{index}: Gradle-{0}, KotlinGradlePlugin-{1}")
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf("4.9", "1.3.30"),
                arrayOf("4.9", "1.3.72"),
                arrayOf("5.6.4", "1.3.72"),
                arrayOf("6.7.1", "1.4.20")
            )
        }
    }

    private fun repositories(useKts: Boolean): String {
        val repositories = mutableListOf(
            "mavenCentral()",
            "mavenLocal()",
            "google()",
            "jcenter()"
        )

        fun addCustomRepository(url: String) {
            repositories += if (useKts) "maven(\"$url\")" else "maven { url '$url' }"
        }

        addCustomRepository("https://dl.bintray.com/kotlin/kotlin-dev")
        addCustomRepository("http://dl.bintray.com/kotlin/kotlin-eap")

        return repositories.joinToString("\n")
    }

    override fun configureByFiles(properties: Map<String, String>?): List<VirtualFile> {
        val unitedProperties = HashMap(properties ?: emptyMap())
        unitedProperties["kotlin_plugin_version"] = gradleKotlinPluginVersion

        unitedProperties["kotlin_plugin_repositories"] = repositories(false)
        unitedProperties["kts_kotlin_plugin_repositories"] = repositories(true)
        return super.configureByFiles(unitedProperties)
    }
}

