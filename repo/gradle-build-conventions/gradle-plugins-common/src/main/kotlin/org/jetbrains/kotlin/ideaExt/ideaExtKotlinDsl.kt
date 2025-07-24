@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.ideaExt

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import org.gradle.plugins.ide.idea.model.IdeaProject
import org.jetbrains.gradle.ext.*

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun org.gradle.api.Project.idea(configure: org.gradle.plugins.ide.idea.model.IdeaModel.() -> Unit): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("idea", configure)

fun IdeaProject.settings(block: ProjectSettings.() -> Unit) =
    (this@settings as ExtensionAware).extensions.configure(block)

fun ProjectSettings.compiler(block: IdeaCompilerConfiguration.() -> Unit) =
    (this@compiler as ExtensionAware).extensions.configure(block)

fun ProjectSettings.delegateActions(block: ActionDelegationConfig.() -> Unit) =
    (this@delegateActions as ExtensionAware).extensions.configure(block)

fun ProjectSettings.runConfigurations(block: RunConfigurationContainer.() -> Unit) =
    (this@runConfigurations as ExtensionAware).extensions.configure("runConfigurations", block)

inline fun <reified T: RunConfiguration> RunConfigurationContainer.defaults(noinline block: T.() -> Unit) =
    defaults(T::class.java, block)

fun RunConfigurationContainer.junit(name: String, block: JUnit.() -> Unit) =
    create(name, JUnit::class.java, block)

fun RunConfigurationContainer.application(name: String, block: Application.() -> Unit) =
    create(name, Application::class.java, block)

fun ProjectSettings.ideArtifacts(block: NamedDomainObjectContainer<org.jetbrains.gradle.ext.TopLevelArtifact>.() -> Unit) =
    (this@ideArtifacts as ExtensionAware).extensions.configure("ideArtifacts", block)
