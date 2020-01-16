package org.jetbrains.kotlin.tools.projectWizard.templates

import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.TaskRunningContext
import org.jetbrains.kotlin.tools.projectWizard.core.entity.TemplateSetting
import org.jetbrains.kotlin.tools.projectWizard.core.entity.TemplateSettingReference
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModuleKind
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType

class KotlinTestTemplate : Template() {
    override val id: String = "kotlinTest"
    override val title: String = "Kotlin Test framework"
    @Language("HTML")
    override val htmlDescription = """A unit test module using Kotlin Test Framework <br>
        | For JVM JUnit4/5 and TestNG can be used <br>
        | For Kotlin/JS Jasmine, Mocha, and Jest may be used<br><br>
        | More information can be found  <a href='https://kotlinlang.org/api/latest/kotlin.test/index.html'>here</a>
    """.trimMargin()
    override val moduleTypes = setOf(ModuleType.common, ModuleType.js, ModuleType.jvm)

    override fun TaskRunningContext.getRequiredLibraries(module: ModuleIR): List<DependencyIR> =
        withSettingsOf(module.originalModule) {
            framework.reference.settingValue.dependencyNames.map { dependencyName ->
                KotlinArbitraryDependencyIR(
                    dependencyName,
                    isInMppModule = module.originalModule.kind
                        .let { it == ModuleKind.multiplatform || it == ModuleKind.target },
                    version = KotlinPlugin::version.settingValue,
                    dependencyType = DependencyType.MAIN
                )
            }
        }

    override fun TaskRunningContext.getFileTemplates(module: ModuleIR): List<FileTemplateDescriptorWithPath> =
        withSettingsOf(module.originalModule) {
            buildList {
                if (generateDummyTest.reference.settingValue) {
                    +(FileTemplateDescriptor("kotlinTestFramework/dummyTest.kt.vm", "Test.kt".asPath()) asSrcOf SourcesetType.main)
                }
            }
        }

    val framework by enumSetting<KotlinTestFramework>(
        "Test Framework",
        neededAtPhase = GenerationPhase.PROJECT_GENERATION
    ) {
        filter = filter@{ reference, kotlinTestFramework ->
            if (reference !is TemplateSettingReference) return@filter true

            val moduleType = reference.module?.configurator?.moduleType
            kotlinTestFramework.moduleType == moduleType
        }
    }

    val generateDummyTest by booleanSetting(
        "Generate Simple test case",
        neededAtPhase = GenerationPhase.PROJECT_GENERATION
    ) {
        defaultValue = false
    }
    override val settings: List<TemplateSetting<*, *>> =
        listOf(framework, generateDummyTest)
}

enum class KotlinTestFramework(
    override val text: String,
    val moduleType: ModuleType,
    val dependencyNames: List<String>
) : DisplayableSettingItem {
    JUNIT4("JUnit 4 Test Framework", ModuleType.jvm, listOf("test-junit")),
    JUNIT5("JUnit 5 Test Framework", ModuleType.jvm, listOf("test-junit5")),
    TEST_NG("Test NG Test Framework", ModuleType.jvm, listOf("test-testng")),
    JS("JavaScript Test Framework", ModuleType.js, listOf("test-js")),
    COMMON("Common Test Framework", ModuleType.common, listOf("test-common", "test-annotations-common")),
}