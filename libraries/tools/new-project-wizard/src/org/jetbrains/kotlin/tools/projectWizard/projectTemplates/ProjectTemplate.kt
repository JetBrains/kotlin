package org.jetbrains.kotlin.tools.projectWizard.projectTemplates

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.entity.*
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.AndroidSinglePlatformModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.MppModuleConfigurator
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.NativeForCurrentSystemTarget
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.defaultTarget
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.projectTemplates.MultiplatformLibrary.withTemplate
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.templates.*

sealed class ProjectTemplate : DisplayableSettingItem {
    abstract val title: String
    override val text: String get() = title
    abstract val htmlDescription: String
    abstract val suggestedProjectName: String
    abstract val projectKind: ProjectKind

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


    fun <T : Template> Sourceset.withTemplate(
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
            CustomSingleplatformProjectTemplate,
            CustomMultiplatformProjectTemplate,
            JvmConsoleApplication,
            JvmServerJsClient,
            MultiplatformLibrary,
            AndroidApplication,
            NativeConsoleApplication
        )
    }
}

class TemplateSettingsBuilder<Q : Template>(
    val sourceset: Sourceset,
    val template: Q
) : TemplateEnvironment by SourcesetBasedTemplateEnvironment(template, sourceset) {
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
            this,
            template = null,
            dependencies = emptyList()
        )
    }

private fun ModuleType.createDefaultTarget(
    name: String = this.name
) = MultiplatformTargetModule(name, defaultTarget, createDefaultSourcesets())

object CustomSingleplatformProjectTemplate : ProjectTemplate() {
    override val title = "Empty JVM Project"
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

object CustomMultiplatformProjectTemplate : ProjectTemplate() {
    override val title = "Empty MultiPlatform Project"
    override val htmlDescription = title
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
    override val title = "Kotlin Console JVM Application"
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
                    mainSourceset?.withTemplate(ConsoleJvmApplicationTemplate())
                }
            )
        )
}

object MultiplatformLibrary : ProjectTemplate() {
    override val title = "Multiplatform Library"
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
                        ModuleType.common.createDefaultTarget().apply {
                            testSourceset?.withTemplate(KotlinTestTemplate()) {
                                template.framework withValue KotlinTestFramework.COMMON
                                template.generateDummyTest withValue false
                            }
                        },
                        ModuleType.jvm.createDefaultTarget().apply {
                            testSourceset?.withTemplate(KotlinTestTemplate()) {
                                template.framework withValue KotlinTestFramework.JUNIT4
                                template.generateDummyTest withValue false
                            }
                        },
                        ModuleType.js.createDefaultTarget().apply {
                            testSourceset?.withTemplate(KotlinTestTemplate()) {
                                template.framework withValue KotlinTestFramework.JS
                                template.generateDummyTest withValue false
                            }
                        },
                        ModuleType.native.createDefaultTarget()
                    )
                )
            )
        )
}

object JvmServerJsClient : ProjectTemplate() {
    override val title: String = "JS client and JVM server"
    override val htmlDescription: String =
        "Multiplatform Gradle project allowing reuse of the same Kotlin code between JS Client and JVM Server"
    override val suggestedProjectName: String = "myFullStackApplication"
    override val projectKind: ProjectKind = ProjectKind.Multiplatform
    override val setsPluginSettings: List<SettingWithValue<*, *>> = listOf(
        KotlinPlugin::modules withValue listOf(
            MultiplatformModule(
                "application",
                listOf(
                    ModuleType.jvm.createDefaultTarget().apply {
                        mainSourceset?.withTemplate(KtorServerTemplate()) {
                            template.serverEngine withValue KtorServerEngine.Netty
                        }
                    },
                    ModuleType.js.createDefaultTarget().apply {
                        mainSourceset?.withTemplate(SimpleJsClientTemplate())
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
    override val projectKind = ProjectKind.Multiplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin::modules withValue listOf(
                Module(
                    "app",
                    ModuleKind.singleplatform,
                    AndroidSinglePlatformModuleConfigurator,
                    SourcesetType.ALL.map { type ->
                        Sourceset(type, ModuleType.jvm, template = null, dependencies = emptyList())
                    },
                    subModules = emptyList()
                )
            )
        )
}

object NativeConsoleApplication : ProjectTemplate() {
    override val title = "Native Console Application"
    override val htmlDescription = title
    override val suggestedProjectName = "myNativeConsoleApp"
    override val projectKind = ProjectKind.Multiplatform

    override val setsPluginSettings: List<SettingWithValue<*, *>>
        get() = listOf(
            KotlinPlugin::modules withValue listOf(
                Module(
                    "app",
                    ModuleKind.multiplatform,
                    MppModuleConfigurator,
                    emptyList(),
                    subModules = listOf(
                        ModuleType.native.createDefaultTarget("native").apply {
                            mainSourceset?.withTemplate(NativeConsoleApplicationTemplate())
                        }
                    )
                )
            )
        )
}