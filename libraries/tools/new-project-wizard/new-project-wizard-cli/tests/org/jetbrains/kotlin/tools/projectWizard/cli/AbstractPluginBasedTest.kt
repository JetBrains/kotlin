/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.cli

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.tools.projectWizard.core.Context
import org.jetbrains.kotlin.tools.projectWizard.core.Plugin
import org.jetbrains.kotlin.tools.projectWizard.core.PluginReference
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import java.nio.file.Path
import kotlin.reflect.full.primaryConstructor

abstract class AbstractPluginBasedTest : UsefulTestCase() {
    open val defaultPlugins: List<PluginReference> = listOf(
        StructurePlugin::class,
        TemplatesPlugin::class
    )

    protected fun init(directory: Path): WizardTestData {
        val pluginNames = directory.resolve("plugins.txt").toFile().readLines().mapNotNull { name ->
            name.trim().takeIf { it.isNotBlank() }
        }.distinct()

        @Suppress("UNCHECKED_CAST")
        val pluginClasses = defaultPlugins + pluginNames.map { pluginName ->
            Class.forName(pluginName).kotlin as PluginReference
        }

        val createPlugins = { context: Context ->
            pluginClasses.map { pluginClass ->
                pluginClass.primaryConstructor!!.call(context)
            }
        }
        return WizardTestData(pluginClasses, createPlugins)
    }
}

data class WizardTestData(
    val pluginClasses: List<PluginReference>,
    val createPlugins: (Context) -> List<Plugin>
)