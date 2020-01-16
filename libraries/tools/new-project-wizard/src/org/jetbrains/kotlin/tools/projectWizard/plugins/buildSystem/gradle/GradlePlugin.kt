package org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.gradle

import org.jetbrains.kotlin.tools.projectWizard.core.*
import org.jetbrains.kotlin.tools.projectWizard.core.entity.reference
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.StructurePlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.BuildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.allModulesPaths
import org.jetbrains.kotlin.tools.projectWizard.plugins.buildSystem.buildSystemType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.templates.TemplatesPlugin
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplate
import org.jetbrains.kotlin.tools.projectWizard.templates.FileTemplateDescriptor


abstract class GradlePlugin(context: Context) : BuildSystemPlugin(context) {
    val createGradleWrapper by booleanSetting("Create Gradle Wrapper", GenerationPhase.FIRST_STEP) {
        defaultValue = true
        checker = isGradle
    }


    val version by versionSetting("Gradle Version", GenerationPhase.FIRST_STEP) {
        defaultValue = defaultVersions.first()
        checker = isGradle
    }

    val gradleVersions by property<List<Version>>(emptyList())

    val gradleProperties by listProperty<Pair<String, String>>()

    val createGradlePropertiesFile by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runAfter(KotlinPlugin::createModules)
        runBefore(TemplatesPlugin::renderFileTemplates)
        activityChecker = isGradle
        withAction {
            TemplatesPlugin::addFileTemplate.execute(
                FileTemplate(
                    FileTemplateDescriptor(
                        "gradle/gradle.properties.vm",
                        "gradle.properties".asPath()
                    ),
                    StructurePlugin::projectPath.settingValue,
                    mapOf(
                        "properties" to GradlePlugin::gradleProperties.propertyValue
                    )
                )
            )
        }
    }

    val localProperties by listProperty(
        "kotlin.code.style" to "official"
    )

    val createLocalPropertiesFile by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runAfter(KotlinPlugin::createModules)
        runBefore(TemplatesPlugin::renderFileTemplates)
        activityChecker = isGradle
        withAction {
            TemplatesPlugin::addFileTemplate.execute(
                FileTemplate(
                    FileTemplateDescriptor(
                        "gradle/local.properties.vm",
                        "local.properties".asPath()
                    ),
                    StructurePlugin::projectPath.settingValue,
                    mapOf(
                        "properties" to GradlePlugin::localProperties.propertyValue
                    )
                )
            )
        }
    }

    private val isGradle = checker {
        rule(
            (BuildSystemPlugin::type.reference shouldBeEqual BuildSystemType.GradleKotlinDsl) or
                    (BuildSystemPlugin::type.reference shouldBeEqual BuildSystemType.GradleGroovyDsl)
        )
    }


    val initGradleWrapperTask by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runBefore(TemplatesPlugin::renderFileTemplates)
        activityChecker = isGradle
        withAction {
            if (!GradlePlugin::createGradleWrapper.reference.settingValue) return@withAction UNIT_SUCCESS
            TemplatesPlugin::addFileTemplate.execute(
                FileTemplate(
                    FileTemplateDescriptor(
                        "gradle/gradle-wrapper.properties.vm",
                        "gradle" / "wrapper" / "gradle-wrapper.properties"
                    ),
                    StructurePlugin::projectPath.reference.settingValue,
                    mapOf(
                        "version" to GradlePlugin::version.reference.settingValue.toString()
                    )
                )
            )
        }
    }

    val createSettingsFileTask by pipelineTask(GenerationPhase.PROJECT_GENERATION) {
        runBefore(TemplatesPlugin::addTemplatesToModules)
        activityChecker = isGradle
        withAction {
            val templateDescriptor = when (buildSystemType) {
                BuildSystemType.GradleKotlinDsl -> FileTemplateDescriptor(
                    "gradle/settings.gradle.kts.vm",
                    "settings.gradle.kts".asPath()
                )
                BuildSystemType.GradleGroovyDsl -> FileTemplateDescriptor(
                    "gradle/settings.gradle.vm",
                    "settings.gradle".asPath()
                )
                else -> return@withAction UNIT_SUCCESS
            }
            TemplatesPlugin::addFileTemplate.execute(
                FileTemplate(
                    templateDescriptor,
                    StructurePlugin::projectPath.settingValue,
                    mapOf(
                        "projectName" to StructurePlugin::name.settingValue,
                        "subProjects" to allModulesPaths
                            .map { path ->
                                path.joinToString(separator = "") { ":$it" }
                            }
                    )
                )
            )
        }
    }

    companion object {
        // TODO update default versions
        private val defaultVersions = listOf("5.5.1").map(Version.Companion::fromString)
    }
}
