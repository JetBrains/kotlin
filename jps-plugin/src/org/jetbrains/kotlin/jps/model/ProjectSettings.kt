/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.model

import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.ex.JpsElementBase
import org.jetbrains.jps.model.ex.JpsElementChildRoleBase
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.config.CompilerSettings

var JpsProject.kotlinCompilerSettings
    get() = kotlinCompilerSettingsContainer.compilerSettings
    internal set(value) {
        getOrCreateSettings().compilerSettings = value
    }

var JpsProject.kotlinCommonCompilerArguments
    get() = kotlinCompilerSettingsContainer.commonCompilerArguments
    internal set(value) {
        getOrCreateSettings().commonCompilerArguments = value
    }

var JpsProject.k2MetadataCompilerArguments
    get() = kotlinCompilerSettingsContainer.k2MetadataCompilerArguments
    internal set(value) {
        getOrCreateSettings().k2MetadataCompilerArguments = value
    }

var JpsProject.k2JsCompilerArguments
    get() = kotlinCompilerSettingsContainer.k2JsCompilerArguments
    internal set(value) {
        getOrCreateSettings().k2JsCompilerArguments = value
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
    internal var k2MetadataCompilerArguments = K2MetadataCompilerArguments()
    internal var k2JvmCompilerArguments = K2JVMCompilerArguments()
    internal var k2JsCompilerArguments = K2JSCompilerArguments()
    internal var compilerSettings = CompilerSettings()

    @Suppress("UNCHECKED_CAST")
    internal operator fun <T : CommonCompilerArguments> get(compilerArgumentsClass: Class<T>): T = when (compilerArgumentsClass) {
        K2MetadataCompilerArguments::class.java -> k2MetadataCompilerArguments as T
        K2JVMCompilerArguments::class.java -> k2JvmCompilerArguments as T
        K2JSCompilerArguments::class.java -> k2JsCompilerArguments as T
        else -> commonCompilerArguments as T
    }

    override fun createCopy(): JpsKotlinCompilerSettings {
        val copy = JpsKotlinCompilerSettings()
        copy.commonCompilerArguments = this.commonCompilerArguments
        copy.k2MetadataCompilerArguments = this.k2MetadataCompilerArguments
        copy.k2JvmCompilerArguments = this.k2JvmCompilerArguments
        copy.k2JsCompilerArguments = this.k2JsCompilerArguments
        copy.compilerSettings = this.compilerSettings
        return copy
    }

    override fun applyChanges(modified: JpsKotlinCompilerSettings) {
        // do nothing
    }

    companion object {
        internal val ROLE = JpsElementChildRoleBase.create<JpsKotlinCompilerSettings>("Kotlin Compiler Settings")
    }
}