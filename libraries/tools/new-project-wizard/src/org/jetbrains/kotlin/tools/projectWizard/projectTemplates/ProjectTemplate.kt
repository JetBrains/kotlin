package org.jetbrains.kotlin.tools.projectWizard.projectTemplates

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

sealed class ProjectTemplate : DisplayableSettingItem {
    abstract val title: String
    override val text: String get() = title
    abstract val description: String
    abstract val suggestedProjectName: String
    abstract val projectKind: ProjectKind
    open val id: String
        get() = this::class.simpleName.toString().removeSuffix("Template").removeSuffix("Project")

    private val setsDefaultValues: List<SettingWithValue<*, *>>
        get() = listOf(KotlinPlugin::projectKind.reference withValue projectKind)

    protected open val setsPluginSettings: List<SettingWithValue<*, *>> = emptyList()
    protected open val setsModules: List<Module> = emptyList()
    private val setsAdditionalSettingValues = mutableListOf<SettingWithValue<*, *>>()

    val setsValues: List<SettingWithValue<*, *>>
        get() = buildList {
            setsModules.takeIf { it.isNotEmpty() }?.let { modules ->
                +(KotlinPlugin::modules withValue modules)
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
            EmptySingleplatformProjectTemplate,
            JvmConsoleApplication,
            MPPMobileApplication,
            MultiplatformMobileLibrary,
            EmptyMultiplatformProjectTemplate,
            MultiplatformLibrary,
            NativeConsoleApplication,
            JsBrowserApplication,
            JvmServerJsClient
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
) : ModuleConfiguratorSettingsEnvironment by ModuleBasedConfiguratorSettingsEnvironment(configurator, module) {
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

inline infix fun <V : Any, reified T : SettingType<V>> PluginSettingPropertyReference<V, T>.withValue(
    value: V
): SettingWithValue<V, T> = reference.withValue(value)

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

object EmptySingleplatformProjectTemplate : ProjectTemplate() {
    override val title = "Backend Application"
    override val description = "Create a backend application with Kotlin/JVM."
    override val suggestedProjectName = "myKotlinJvmProject"
    override val projectKind = ProjectKind.Singleplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin::modules withValue listOf(
                SingleplatformModule("mainModule", createDefaultSourcesets())
            )
        )
}

object EmptyMultiplatformProjectTemplate : ProjectTemplate() {
    override val title = "Multiplatform Application"
    override val description = "Create applications for different platforms that support sharing common code."
    override val suggestedProjectName = "myKotlinMultiplatformProject"
    override val projectKind = ProjectKind.Multiplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin::modules withValue listOf(
                MultiplatformModule("mainModule", listOf(ModuleType.common.createDefaultTarget()))
            )
        )
}

object JvmConsoleApplication : ProjectTemplate() {
    override val title = "Console Application"
    override val description = "Create a console application with Kotlin/JVM. Use it for prototyping or testing purposes."
    override val suggestedProjectName = "myConsoleApplication"
    override val projectKind = ProjectKind.Singleplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin::modules withValue listOf(
                SingleplatformModule(
                    "consoleApp",
                    createDefaultSourcesets()
                ).apply {
                    withTemplate(ConsoleJvmApplicationTemplate())
                }
            )
        )
}

object MultiplatformLibrary : ProjectTemplate() {
    override val title = "Multiplatform Library"
    override val description = "Create a library for sharing common code among different platforms."
    override val suggestedProjectName = "myMultiplatformLibrary"
    override val projectKind = ProjectKind.Multiplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin::modules withValue listOf(
                MultiplatformModule(
                    "library",
                    listOf(
                        ModuleType.common.createDefaultTarget(),
                        ModuleType.jvm.createDefaultTarget(),
                        ModuleType.js.createDefaultTarget(),
                        ModuleType.native.createDefaultTarget()
                    )
                )
            )
        )
}

object JvmServerJsClient : ProjectTemplate() {
    override val title: String = "Full-Stack Web Application"
    override val description: String = "" +
            "Create a fully-functional web application using Kotlin/JS for the frontend and Kotlin/JVM for the backend."
    override val suggestedProjectName: String = "myFullStackApplication"
    override val projectKind: ProjectKind = ProjectKind.Multiplatform
    override val setsPluginSettings: List<SettingWithValue<*, *>> = listOf(
        KotlinPlugin::modules withValue listOf(
            MultiplatformModule(
                "application",
                listOf(
                    ModuleType.jvm.createDefaultTarget().apply {
                        withTemplate(KtorServerTemplate()) {
                            template.serverEngine withValue KtorServerEngine.Netty
                        }
                    },
                    ModuleType.js.createDefaultTarget().apply {
                        withTemplate(SimpleJsClientTemplate())
                    }
                )
            )
        )
    )
}

object NativeConsoleApplication : ProjectTemplate() {
    override val title = "Native Application"
    override val description =
        "Create an application with Kotlin/Native that works as a standalone application under a specific platform."
    override val suggestedProjectName = "myNativeConsoleApp"
    override val projectKind = ProjectKind.Multiplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin::modules withValue listOf(
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

object JsBrowserApplication : ProjectTemplate() {
    override val title = "Frontend Application"
    override val description = "Create a frontend application with Kotlin/JS if you already have a backend."
    override val suggestedProjectName = "myKotlinJsApplication"
    override val projectKind = ProjectKind.Js

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin::modules withValue listOf(
                Module(
                    "frontend",
                    JsSingleplatformModuleConfigurator,
                    template = SimpleJsClientTemplate(),
                    sourcesets = SourcesetType.ALL.map { type ->
                        Sourceset(type, dependencies = emptyList())
                    },
                    subModules = emptyList()
                )
            )
        )
}

object MPPMobileApplication : ProjectTemplate() {
    override val title = "Multiplatform Mobile Application"

    override val description =
        "Create mobile applications for iOS and Android with Kotlin Multiplatform Mobile, which supports sharing common code between platforms."
    override val suggestedProjectName = "myIOSApplication"
    override val projectKind = ProjectKind.Multiplatform

    override val setsModules: List<Module> = buildList {
        val shared = MultiplatformModule(
            "shared",
            listOf(
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
                    RealNativeTargetConfigurator.configuratorsByModuleType.getValue(ModuleSubType.iosX64),
                    null,
                    sourcesets = createDefaultSourcesets(),
                    subModules = emptyList()
                )
            )
        )
        +shared
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
    }
}

object MultiplatformMobileLibrary : ProjectTemplate() {
    override val title = "Multiplatform Mobile Library"
    override val description = "Create a library that supports sharing code between iOS and Android."
    override val suggestedProjectName = "myMppMobileLibrary"
    override val projectKind = ProjectKind.Multiplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin::modules withValue listOf(
                MultiplatformModule(
                    "library",
                    listOf(
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