/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.model

import com.intellij.util.xmlb.XmlSerializer
import org.jdom.Element
import org.jetbrains.jps.model.JpsElement
import org.jetbrains.jps.model.JpsProject
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.serialization.JpsProjectExtensionSerializer
import org.jetbrains.jps.model.serialization.facet.JpsFacetConfigurationSerializer
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.setApiVersionToLanguageVersionIfNeeded
import org.jetbrains.kotlin.config.*
import java.util.*

class KotlinModelSerializerService : KotlinCommonJpsModelSerializerExtension() {
    override fun getProjectExtensionSerializers() = listOf(
        KotlinCommonCompilerArgumentsSerializer(),
        Kotlin2JvmCompilerArgumentsSerializer(),
        Kotlin2JsCompilerArgumentsSerializer(),
        KotlinCompilerSettingsSerializer()
    )

    override fun getFacetConfigurationSerializers() = listOf(JpsKotlinFacetConfigurationSerializer)
}

object JpsKotlinFacetConfigurationSerializer : JpsFacetConfigurationSerializer<JpsKotlinFacetModuleExtension>(
    JpsKotlinFacetModuleExtension.KIND,
    JpsKotlinFacetModuleExtension.FACET_TYPE_ID,
    JpsKotlinFacetModuleExtension.FACET_NAME
) {
    override fun loadExtension(
        facetConfigurationElement: Element,
        name: String,
        parent: JpsElement?,
        module: JpsModule
    ): JpsKotlinFacetModuleExtension {
        return JpsKotlinFacetModuleExtension(deserializeFacetSettings(facetConfigurationElement))
    }

    override fun saveExtension(
        extension: JpsKotlinFacetModuleExtension?,
        facetConfigurationTag: Element,
        module: JpsModule
    ) {
        (extension as JpsKotlinFacetModuleExtension).settings.serializeFacetSettings(facetConfigurationTag)
    }
}

abstract class BaseJpsCompilerSettingsSerializer<in T : Any>(
    componentName: String,
    private val settingsFactory: () -> T
) : JpsProjectExtensionSerializer(SettingConstants.KOTLIN_COMPILER_SETTINGS_FILE, componentName) {
    protected abstract fun onLoad(project: JpsProject, settings: T)

    override fun loadExtension(project: JpsProject, componentTag: Element) {
        val settings = settingsFactory().apply {
            if (this is CommonCompilerArguments) {
                freeArgs = ArrayList()
            }
        }
        XmlSerializer.deserializeInto(settings, componentTag)
        onLoad(project, settings)
    }

    override fun saveExtension(project: JpsProject, componentTag: Element) {
    }
}

internal class KotlinCompilerSettingsSerializer : BaseJpsCompilerSettingsSerializer<CompilerSettings>(
    SettingConstants.KOTLIN_COMPILER_SETTINGS_SECTION, ::CompilerSettings
) {
    override fun onLoad(project: JpsProject, settings: CompilerSettings) {
        project.kotlinCompilerSettings = settings
    }
}

internal class KotlinCommonCompilerArgumentsSerializer : BaseJpsCompilerSettingsSerializer<CommonCompilerArguments.DummyImpl>(
    SettingConstants.KOTLIN_COMMON_COMPILER_ARGUMENTS_SECTION, CommonCompilerArguments::DummyImpl
) {
    override fun onLoad(project: JpsProject, settings: CommonCompilerArguments.DummyImpl) {
        settings.setApiVersionToLanguageVersionIfNeeded()
        project.kotlinCommonCompilerArguments = settings
    }
}

internal class Kotlin2JsCompilerArgumentsSerializer : BaseJpsCompilerSettingsSerializer<K2JSCompilerArguments>(
    SettingConstants.KOTLIN_TO_JS_COMPILER_ARGUMENTS_SECTION, ::K2JSCompilerArguments
) {
    override fun onLoad(project: JpsProject, settings: K2JSCompilerArguments) {
        project.k2JsCompilerArguments = settings
    }
}

internal class Kotlin2JvmCompilerArgumentsSerializer : BaseJpsCompilerSettingsSerializer<K2JVMCompilerArguments>(
    SettingConstants.KOTLIN_TO_JVM_COMPILER_ARGUMENTS_SECTION, ::K2JVMCompilerArguments
) {
    override fun onLoad(project: JpsProject, settings: K2JVMCompilerArguments) {
        project.k2JvmCompilerArguments = settings
    }
}