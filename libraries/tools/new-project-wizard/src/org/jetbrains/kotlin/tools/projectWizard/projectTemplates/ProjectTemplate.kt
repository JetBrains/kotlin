package org.jetbrains.kotlin.tools.projectWizard.projectTemplates

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.*
import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.*
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.withAllSubModules
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.templates.*

sealed class ProjectTemplate : DisplayableSettingItem {
    abstract val title: String
    override val text: String get() = title
    abstract val htmlDescription: String
    abstract val suggestedProjectName: String
    abstract val projectKind: ProjectKind
    open val id: String
        get() = this::class.simpleName.toString().removeSuffix("Template").removeSuffix("Project")

    private val setsDefaultValues: List<SettingWithValue<*, *>>
        get() = listOf(KotlinPlugin::projectKind.reference withValue projectKind)

    protected abstract val setsPluginSettings: List<SettingWithValue<*, *>>
    private val setsAdditionalSettingValues = mutableListOf<SettingWithValue<*, *>>()

    val setsValues: List<SettingWithValue<*, *>>
        get() = buildList {
            +setsDefaultValues
            +setsPluginSettings
            +setsAdditionalSettingValues
        }


    fun <T : Template> Module.withTemplate(
        template: T,
        createSettings: TemplateSettingsBuilder<T>.() -> Unit = {}
    ) = apply {
        this.template = template
        with(TemplateSettingsBuilder(this, template)) {
            createSettings()
            setsAdditionalSettingValues += setsSettings
        }
    }


    companion object {
        val ALL = listOf(
            EmptySingleplatformProjectTemplate,
            EmptyMultiplatformProjectTemplate,
            JvmConsoleApplication,
            JvmServerJsClient,
            MultiplatformLibrary,
            AndroidApplication,
            IOSApplication,
            NativeConsoleApplication,
            JsBrowserApplication
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

data class SettingWithValue<V : Any, T : SettingType<V>>(val setting: SettingReference<V, T>, val value: V)

infix fun <V : Any, T : SettingType<V>> PluginSettingReference<V, T>.withValue(value: V): SettingWithValue<V, T> =
    SettingWithValue(this, value)

inline infix fun <V : Any, reified T : SettingType<V>> PluginSettingPropertyReference<V, T>.withValue(
    value: V
): SettingWithValue<V, T> = reference.withValue(value)

private fun ModuleType.createDefaultSourcesets() =
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
    override val title = "Empty Kotlin/JVM Project"
    override val htmlDescription = title
    override val suggestedProjectName = "myKotlinJvmProject"
    override val projectKind = ProjectKind.Singleplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin::modules withValue listOf(
                SingleplatformModule("mainModule", ModuleType.jvm.createDefaultSourcesets())
            )
        )
}

object EmptyMultiplatformProjectTemplate : ProjectTemplate() {
    override val title = "Empty Kotlin Multiplatform Project"
    override val htmlDescription = "Multiplatform Gradle project without predefined targets"
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
    override val title = "Kotlin/JVM Console Application"
    override val htmlDescription = title
    override val suggestedProjectName = "myConsoleApplication"
    override val projectKind = ProjectKind.Singleplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin::modules withValue listOf(
                SingleplatformModule(
                    "consoleApp",
                    ModuleType.jvm.createDefaultSourcesets()
                ).apply {
                    withTemplate(ConsoleJvmApplicationTemplate())
                }
            )
        )
}

object MultiplatformLibrary : ProjectTemplate() {
    override val title = "Kotlin Multiplatform Library"

    @Language("HTML")
    override val htmlDescription = """
        Multiplatform Gradle project allowing reuse of the same Kotlin code between all three main platforms 
        (<b>JVM</b>, <b>JS</b>, and <b>Native</b>)
        """.trimIndent()
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
    override val title: String = "Web Application with Kotlin/JS Client and Kotlin/JVM Server"
    override val htmlDescription: String =
        "Multiplatform Gradle project with client module running on Kotlin/JS and server module on Kotlin/JVM"
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

object AndroidApplication : ProjectTemplate() {
    override val title = "Android Application"

    @Language("HTML")
    override val htmlDescription = """
       Simple <b>Android</b> application with single activity 
        """.trimIndent()
    override val suggestedProjectName = "myAndroidApplication"
    override val projectKind = ProjectKind.Android

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin::modules withValue listOf(
                Module(
                    "app",
                    AndroidSinglePlatformModuleConfigurator,
                    template = null,
                    sourcesets = SourcesetType.ALL.map { type ->
                        Sourceset(type, dependencies = emptyList())
                    },
                    subModules = emptyList()
                )
            )
        )
}

object NativeConsoleApplication : ProjectTemplate() {
    override val title = "Kotlin/Native Console Application"
    override val htmlDescription = title
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
    override val title = "Kotlin/JS Frontend Application"
    override val htmlDescription = "Gradle project for a Kotlin/JS frontend web application"
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

object IOSApplication : ProjectTemplate() {
    override val title = "IOS Application"

    @Language("HTML")
    override val htmlDescription = """
       Simple <b>IOS</b> application with single screen 
        """.trimIndent()
    override val suggestedProjectName = "myIOSApplication"
    override val projectKind = ProjectKind.Multiplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin::modules withValue listOf(
                Module(
                    "iosApp",
                    IOSSinglePlatformModuleConfigurator,
                    template = null,
                    sourcesets = SourcesetType.ALL.map { type ->
                        Sourceset(type, dependencies = emptyList())
                    },
                    subModules = emptyList()
                )
            )
        )
}