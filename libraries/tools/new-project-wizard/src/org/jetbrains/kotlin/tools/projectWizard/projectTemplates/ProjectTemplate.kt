/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.projectTemplates

import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.*
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleSubType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.templates.*
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.JvmSinglePlatformModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.templates.compose.ComposeAndroidTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.compose.ComposeJvmDesktopTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.compose.ComposeMppModuleTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.mpp.MobileMppTemplate

sealed class ProjectTemplate : DisplayableSettingItem {
    abstract val title: String
    override val text: String get() = title
    abstract val description: String
    abstract val suggestedProjectName: String
    abstract val projectKind: ProjectKind
    abstract val id: String

    private val setsDefaultValues: List<SettingWithValue<*, *>>
        get() = listOf(KotlinPlugin.projectKind.reference withValue projectKind)

    protected open val setsPluginSettings: List<SettingWithValue<*, *>> = emptyList()
    protected open val setsModules: List<Module> = emptyList()
    private val setsAdditionalSettingValues = mutableListOf<SettingWithValue<*, *>>()

    val setsValues: List<SettingWithValue<*, *>>
        get() = buildList {
            setsModules.takeIf { it.isNotEmpty() }?.let { modules ->
                +(KotlinPlugin.modules.reference withValue modules)
            }
            +setsDefaultValues
            +setsPluginSettings
            +setsAdditionalSettingValues
        }


    protected fun <T : Template> Module.withTemplate(
        template: T,
        createSettings: TemplateSettingsBuilder<T>.() -> Unit = {}
    ) = apply {
        this.template = template
        with(TemplateSettingsBuilder(this, template)) {
            createSettings()
            setsAdditionalSettingValues += setsSettings
        }
    }

    protected fun <C : ModuleConfigurator> Module.withConfiguratorSettings(
        configurator: C,
        createSettings: ConfiguratorSettingsBuilder<C>.() -> Unit = {}
    ) = apply {
        assert(this.configurator === configurator)
        with(ConfiguratorSettingsBuilder(this, configurator)) {
            createSettings()
            setsAdditionalSettingValues += setsSettings
        }
    }


    companion object {
        val ALL = listOf(
            BackendApplicationProjectTemplate,
            ConsoleApplicationProjectTemplate,
            MultiplatformMobileApplicationProjectTemplate,
            MultiplatformMobileLibraryProjectTemplate,
            MultiplatformApplicationProjectTemplate,
            MultiplatformLibraryProjectTemplate,
            NativeApplicationProjectTemplate,
            FrontendApplicationProjectTemplate,
            ReactApplicationProjectTemplate,
            FullStackWebApplicationProjectTemplate,
            NodeJsApplicationProjectTemplate,
            ComposeDesktopApplicationProjectTemplate,
            ComposeMultiplatformApplicationProjectTemplate,
        )

        fun byId(id: String): ProjectTemplate? = ALL.firstOrNull {
            it.id.equals(id, ignoreCase = true)
        }
    }
}

class TemplateSettingsBuilder<Q : Template>(
    val module: Module,
    val template: Q
) : TemplateEnvironment by ModuleBasedTemplateEnvironment(template, module) {
    private val settings = mutableListOf<SettingWithValue<*, *>>()
    val setsSettings: List<SettingWithValue<*, *>>
        get() = settings

    infix fun <V : Any, T : SettingType<V>> TemplateSetting<V, T>.withValue(value: V) {
        settings += SettingWithValue(reference, value)
    }
}

class ConfiguratorSettingsBuilder<C : ModuleConfigurator>(
    val module: Module,
    val configurator: C
) : ModuleConfiguratorContext by ModuleBasedConfiguratorContext(configurator, module) {
    init {
        assert(module.configurator === configurator)
    }

    private val settings = mutableListOf<SettingWithValue<*, *>>()
    val setsSettings: List<SettingWithValue<*, *>>
        get() = settings

    infix fun <V : Any, T : SettingType<V>> ModuleConfiguratorSetting<V, T>.withValue(value: V) {
        settings += SettingWithValue(reference, value)
    }

}

data class SettingWithValue<V : Any, T : SettingType<V>>(val setting: SettingReference<V, T>, val value: V)

infix fun <V : Any, T : SettingType<V>> PluginSettingReference<V, T>.withValue(value: V): SettingWithValue<V, T> =
    SettingWithValue(this, value)

private fun createDefaultSourcesets() =
    SourcesetType.values().map { sourcesetType ->
        Sourceset(
            sourcesetType,
            dependencies = emptyList()
        )
    }

private fun ModuleType.createDefaultTarget(
    name: String = this.name
) = MultiplatformTargetModule(name, defaultTarget, createDefaultSourcesets())

object BackendApplicationProjectTemplate : ProjectTemplate() {
    override val title = KotlinNewProjectWizardBundle.message("project.template.empty.singleplatform.title")
    override val description = KotlinNewProjectWizardBundle.message("project.template.empty.singleplatform.description")
    override val id = "backendApplication"

    @NonNls
    override val suggestedProjectName = "myKotlinJvmProject"
    override val projectKind = ProjectKind.Singleplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin.modules.reference withValue listOf(
                SingleplatformModule("mainModule", createDefaultSourcesets())
            )
        )
}

object MultiplatformApplicationProjectTemplate : ProjectTemplate() {
    override val title = KotlinNewProjectWizardBundle.message("project.template.empty.mpp.title")
    override val description = KotlinNewProjectWizardBundle.message("project.template.empty.mpp.description")
    override val id = "multiplatformApplication"

    @NonNls
    override val suggestedProjectName = "myKotlinMultiplatformProject"
    override val projectKind = ProjectKind.Multiplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin.modules.reference withValue listOf(
                MultiplatformModule("mainModule", targets = listOf(ModuleType.common.createDefaultTarget()))
            )
        )
}

object ConsoleApplicationProjectTemplate : ProjectTemplate() {
    override val title = KotlinNewProjectWizardBundle.message("project.template.empty.jvm.console.title")
    override val description = KotlinNewProjectWizardBundle.message("project.template.empty.jvm.console.description")
    override val id = "consoleApplication"

    @NonNls
    override val suggestedProjectName = "myConsoleApplication"
    override val projectKind = ProjectKind.Singleplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin.modules.reference withValue listOf(
                SingleplatformModule(
                    "consoleApp",
                    createDefaultSourcesets()
                ).apply {
                    withTemplate(ConsoleJvmApplicationTemplate())
                }
            )
        )
}

object MultiplatformLibraryProjectTemplate : ProjectTemplate() {
    override val title = KotlinNewProjectWizardBundle.message("project.template.mpp.lib.title")
    override val description = KotlinNewProjectWizardBundle.message("project.template.mpp.lib.description")
    override val id = "multiplatformLibrary"

    @NonNls
    override val suggestedProjectName = "myMultiplatformLibrary"
    override val projectKind = ProjectKind.Multiplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin.modules.reference withValue listOf(
                MultiplatformModule(
                    "library",
                    targets = listOf(
                        ModuleType.common.createDefaultTarget(),
                        ModuleType.jvm.createDefaultTarget(),
                        ModuleType.js.createDefaultTarget().withConfiguratorSettings(JsBrowserTargetConfigurator) {
                            JSConfigurator.kind withValue JsTargetKind.LIBRARY
                        },
                        ModuleType.native.createDefaultTarget()
                    )
                )
            )
        )
}

object FullStackWebApplicationProjectTemplate : ProjectTemplate() {
    override val title: String = KotlinNewProjectWizardBundle.message("project.template.full.stack.title")
    override val description: String = KotlinNewProjectWizardBundle.message("project.template.full.stack.description")
    override val id = "fullStackWebApplication"

    @NonNls
    override val suggestedProjectName: String = "myFullStackApplication"
    override val projectKind: ProjectKind = ProjectKind.Multiplatform
    override val setsPluginSettings: List<SettingWithValue<*, *>> = listOf(
        KotlinPlugin.modules.reference withValue listOf(
            MultiplatformModule(
                "application",
                targets = listOf(
                    ModuleType.common.createDefaultTarget(),
                    ModuleType.jvm.createDefaultTarget().apply {
                        withTemplate(KtorServerTemplate()) {
                            template.serverEngine withValue KtorServerEngine.Netty
                        }
                    },
                    ModuleType.js.createDefaultTarget().apply {
                        withTemplate(ReactJsClientTemplate())
                    }
                )
            )
        )
    )
}

object NativeApplicationProjectTemplate : ProjectTemplate() {
    override val title = KotlinNewProjectWizardBundle.message("project.template.native.console.title")
    override val description = KotlinNewProjectWizardBundle.message("project.template.native.console.description")
    override val id = "nativeApplication"

    @NonNls
    override val suggestedProjectName = "myNativeConsoleApp"
    override val projectKind = ProjectKind.Multiplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin.modules.reference withValue listOf(
                Module(
                    "app",
                    MppModuleConfigurator,
                    template = null,
                    sourcesets = emptyList(),
                    subModules = listOf(
                        ModuleType.native.createDefaultTarget("native").apply {
                            withTemplate(NativeConsoleApplicationTemplate())
                        }
                    )
                )
            )
        )
}

object FrontendApplicationProjectTemplate : ProjectTemplate() {
    override val title = KotlinNewProjectWizardBundle.message("project.template.browser.title")
    override val description = KotlinNewProjectWizardBundle.message("project.template.browser.description")
    override val id = "frontendApplication"

    @NonNls
    override val suggestedProjectName = "myKotlinJsApplication"
    override val projectKind = ProjectKind.Js

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin.modules.reference withValue listOf(
                Module(
                    "browser",
                    BrowserJsSinglePlatformModuleConfigurator,
                    template = SimpleJsClientTemplate(),
                    sourcesets = SourcesetType.ALL.map { type ->
                        Sourceset(type, dependencies = emptyList())
                    },
                    subModules = emptyList()
                )
            )
        )
}

object ReactApplicationProjectTemplate : ProjectTemplate() {
    override val title = KotlinNewProjectWizardBundle.message("project.template.react.title")
    override val description = KotlinNewProjectWizardBundle.message("project.template.react.description")
    override val id = "reactApplication"

    @NonNls
    override val suggestedProjectName = "myKotlinJsApplication"
    override val projectKind = ProjectKind.Js

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin.modules.reference withValue listOf(
                Module(
                    "react",
                    BrowserJsSinglePlatformModuleConfigurator,
                    template = ReactJsClientTemplate(),
                    sourcesets = SourcesetType.ALL.map { type ->
                        Sourceset(type, dependencies = emptyList())
                    },
                    subModules = emptyList()
                )
            )
        )
}

object MultiplatformMobileApplicationProjectTemplate : ProjectTemplate() {
    override val title = KotlinNewProjectWizardBundle.message("project.template.mpp.mobile.title")
    override val description = KotlinNewProjectWizardBundle.message("project.template.mpp.mobile.description")
    override val id = "multiplatformMobileApplication"

    @NonNls
    override val suggestedProjectName = "myIOSApplication"
    override val projectKind = ProjectKind.Multiplatform

    override val setsModules: List<Module> = buildList {
        val shared = MultiplatformModule(
            "shared",
            template = MobileMppTemplate(),
            targets = listOf(
                ModuleType.common.createDefaultTarget(),
                Module(
                    "android",
                    AndroidTargetConfigurator,
                    null,
                    sourcesets = createDefaultSourcesets(),
                    subModules = emptyList()
                ).withConfiguratorSettings(AndroidTargetConfigurator) {
                    configurator.androidPlugin withValue AndroidGradlePlugin.LIBRARY
                },
                Module(
                    "ios",
                    RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.ios),
                    null,
                    sourcesets = createDefaultSourcesets(),
                    subModules = emptyList()
                )
            )
        )
        +Module(
            "iosApp",
            IOSSinglePlatformModuleConfigurator,
            template = null,
            sourcesets = createDefaultSourcesets(),
            subModules = emptyList(),
            dependencies = mutableListOf(ModuleReference.ByModule(shared))
        )
        +Module(
            "androidApp",
            AndroidSinglePlatformModuleConfigurator,
            template = null,
            sourcesets = createDefaultSourcesets(),
            subModules = emptyList(),
            dependencies = mutableListOf(ModuleReference.ByModule(shared))
        )
        +shared // shared module must be the last so dependent modules could create actual files
    }
}

object MultiplatformMobileLibraryProjectTemplate : ProjectTemplate() {
    override val title = KotlinNewProjectWizardBundle.message("project.template.mpp.mobile.lib.title")
    override val description = KotlinNewProjectWizardBundle.message("project.template.mpp.mobile.lib.description")
    override val id = "multiplatformMobileLibrary"

    @NonNls
    override val suggestedProjectName = "myMppMobileLibrary"
    override val projectKind = ProjectKind.Multiplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin.modules.reference withValue listOf(
                MultiplatformModule(
                    "library",
                    targets = listOf(
                        ModuleType.common.createDefaultTarget(),
                        Module(
                            "android",
                            AndroidTargetConfigurator,
                            null,
                            SourcesetType.ALL.map { type ->
                                Sourceset(type, dependencies = emptyList())
                            },
                            emptyList()
                        ).withConfiguratorSettings(AndroidTargetConfigurator) {
                            configurator.androidPlugin withValue AndroidGradlePlugin.LIBRARY
                        },
                        Module(
                            "ios",
                            RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.iosX64),
                            null,
                            SourcesetType.ALL.map { type ->
                                Sourceset(type, dependencies = emptyList())
                            },
                            emptyList()
                        )
                    )
                )
            )
        )
}

object NodeJsApplicationProjectTemplate : ProjectTemplate() {
    override val title = KotlinNewProjectWizardBundle.message("project.template.nodejs.title")
    override val description = KotlinNewProjectWizardBundle.message("project.template.nodejs.description")
    override val id = "nodejsApplication"

    @NonNls
    override val suggestedProjectName = "myKotlinJsApplication"
    override val projectKind = ProjectKind.Js

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin.modules.reference withValue listOf(
                Module(
                    "nodejs",
                    NodeJsSinglePlatformModuleConfigurator,
                    template = SimpleNodeJsTemplate(),
                    sourcesets = SourcesetType.ALL.map { type ->
                        Sourceset(type, dependencies = emptyList())
                    },
                    subModules = emptyList()
                )
            )
        )
}

object ComposeDesktopApplicationProjectTemplate : ProjectTemplate() {
    override val title = KotlinNewProjectWizardBundle.message("project.template.compose.desktop.title")
    override val description = KotlinNewProjectWizardBundle.message("project.template.compose.desktop.description")
    override val id = "composeDesktopApplication"

    @NonNls
    override val suggestedProjectName = "myComposeDesktopApplication"
    override val projectKind = ProjectKind.COMPOSE

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin.modules.reference withValue listOf(
                Module(
                    "compose",
                    JvmSinglePlatformModuleConfigurator,
                    template = ComposeJvmDesktopTemplate(),
                    sourcesets = SourcesetType.ALL.map { type ->
                        Sourceset(type, dependencies = emptyList())
                    },
                    subModules = emptyList()
                ).withConfiguratorSettings(JvmSinglePlatformModuleConfigurator) {
                    ModuleConfiguratorWithTests.testFramework withValue KotlinTestFramework.NONE
                }
            )
        )
}

object ComposeMultiplatformApplicationProjectTemplate : ProjectTemplate() {
    override val title = KotlinNewProjectWizardBundle.message("project.template.compose.multiplatform.title")
    override val description = KotlinNewProjectWizardBundle.message("project.template.compose.multiplatform.description")
    override val id = "composeMultiplatformApplication"

    @NonNls
    override val suggestedProjectName = "myComposeMultiplatformApplication"
    override val projectKind = ProjectKind.COMPOSE

    override val setsModules: List<Module>
        get() = buildList {
            val common = MultiplatformModule(
                "common",
                template = ComposeMppModuleTemplate(),
                listOf(
                    ModuleType.common.createDefaultTarget().withConfiguratorSettings(CommonTargetConfigurator) {
                        ModuleConfiguratorWithTests.testFramework withValue KotlinTestFramework.NONE
                    },
                    Module(
                        "android",
                        AndroidTargetConfigurator,
                        template = null,
                        sourcesets = createDefaultSourcesets(),
                        subModules = emptyList()
                    ).withConfiguratorSettings(AndroidTargetConfigurator) {
                        configurator.androidPlugin withValue AndroidGradlePlugin.LIBRARY
                        ModuleConfiguratorWithTests.testFramework withValue KotlinTestFramework.NONE
                    },
                    Module(
                        "desktop",
                        JvmTargetConfigurator,
                        template = null,
                        sourcesets = createDefaultSourcesets(),
                        subModules = emptyList()
                    ).withConfiguratorSettings(JvmTargetConfigurator) {
                        ModuleConfiguratorWithTests.testFramework withValue KotlinTestFramework.NONE
                    }
                )
            )
            +Module(
                "android",
                AndroidSinglePlatformModuleConfigurator,
                template = ComposeAndroidTemplate(),
                sourcesets = createDefaultSourcesets(),
                subModules = emptyList(),
                dependencies = mutableListOf(ModuleReference.ByModule(common))
            ).withConfiguratorSettings(AndroidSinglePlatformModuleConfigurator) {
                ModuleConfiguratorWithTests.testFramework withValue KotlinTestFramework.NONE
            }
            +Module(
                "desktop",
                JvmSinglePlatformModuleConfigurator,
                template = ComposeJvmDesktopTemplate(),
                sourcesets = createDefaultSourcesets(),
                subModules = emptyList(),
                dependencies = mutableListOf(ModuleReference.ByModule(common))
            ).withConfiguratorSettings(JvmSinglePlatformModuleConfigurator) {
                ModuleConfiguratorWithTests.testFramework withValue KotlinTestFramework.NONE
            }
            +common
        }
}