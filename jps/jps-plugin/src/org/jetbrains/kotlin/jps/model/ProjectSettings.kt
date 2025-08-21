/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.model

import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.ex.JpsElementBase
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.JpsPluginSettings

var JpsProject.kotlinCompilerSettings
    get() = kotlinCompilerSettingsContainer.compilerSettings
    internal set(value) {
        getOrCreateSettings().compilerSettings = value
    }

var JpsProject.kotlinJpsPluginSettings
    get() = kotlinCompilerSettingsContainer.jpsPluginSettings
    internal set(value) {
        getOrCreateSettings().jpsPluginSettings = value
    }

var JpsProject.kotlinCommonCompilerArguments
    get() = kotlinCompilerSettingsContainer.commonCompilerArguments
    internal set(value) {
        getOrCreateSettings().commonCompilerArguments = value
    }

var JpsProject.k2JvmCompilerArguments
    get() = kotlinCompilerSettingsContainer.k2JvmCompilerArguments
    internal set(value) {
        getOrCreateSettings().k2JvmCompilerArguments = value
    }

internal val JpsProject.kotlinCompilerSettingsContainer
    get() = container.getChild(JpsKotlinCompilerSettings.ROLE) ?: JpsKotlinCompilerSettings()

private fun JpsProject.getOrCreateSettings(): JpsKotlinCompilerSettings {
    var settings = container.getChild(JpsKotlinCompilerSettings.ROLE)
    if (settings == null) {
        settings = JpsKotlinCompilerSettings()
        container.setChild(JpsKotlinCompilerSettings.ROLE, settings)
    }
    return settings
}

class JpsKotlinCompilerSettings : JpsElementBase<JpsKotlinCompilerSettings>() {
    internal var commonCompilerArguments: CommonCompilerArguments = CommonCompilerArguments.DummyImpl()
    internal var k2JvmCompilerArguments = K2JVMCompilerArguments()
    internal var compilerSettings = CompilerSettings()
    internal var jpsPluginSettings = JpsPluginSettings()

    @Suppress("UNCHECKED_CAST")
    internal operator fun <T : CommonCompilerArguments> get(compilerArgumentsClass: Class<T>): T = when (compilerArgumentsClass) {
        K2JVMCompilerArguments::class.java -> k2JvmCompilerArguments as T
        else -> commonCompilerArguments as T
    }

    @Deprecated("Deprecated by IJ platform, don't use it")
    override fun createCopy(): JpsKotlinCompilerSettings {
        val copy = JpsKotlinCompilerSettings()
        copy.commonCompilerArguments = this.commonCompilerArguments
        copy.k2JvmCompilerArguments = this.k2JvmCompilerArguments
        copy.compilerSettings = this.compilerSettings
        copy.jpsPluginSettings = this.jpsPluginSettings
        return copy
    }

    @Deprecated("Deprecated by IJ platform, don't use it")
    override fun applyChanges(modified: JpsKotlinCompilerSettings) {
        // do nothing
    }

    companion object {
        internal val ROLE = JpsElementChildRoleBase.create<JpsKotlinCompilerSettings>("Kotlin Compiler Settings")
    }
}