/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates


import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.tools.projectWizard.core.Writer
import org.jetbrains.kotlin.tools.projectWizard.core.asPath
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.entity.settings.TemplateSetting
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.addWithJavaIntoJvmTarget
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.InterceptionPoint
import org.jetbrains.kotlin.tools.projectWizard.KotlinNewProjectWizardBundle
import org.jetbrains.kotlin.tools.projectWizard.Versions
import org.jetbrains.kotlin.tools.projectWizard.WizardGradleRunConfiguration
import org.jetbrains.kotlin.tools.projectWizard.WizardRunConfiguration
import org.jetbrains.kotlin.tools.projectWizard.core.Reader
import org.jetbrains.kotlin.tools.projectWizard.moduleConfigurators.moduleType
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.KotlinPlugin
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ProjectKind
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

class KtorServerTemplate : Template() {
    override val title: String = KotlinNewProjectWizardBundle.message("module.template.ktor.server.title")
    override val description: String = KotlinNewProjectWizardBundle.message("module.template.ktor.server.description")

    override fun isSupportedByModuleType(module: Module, projectKind: ProjectKind): Boolean =
        module.configurator.moduleType == ModuleType.jvm

    @NonNls
    override val id: String = "ktorServer"

    override fun Writer.getRequiredLibraries(module: ModuleIR): List<DependencyIR> =
        withSettingsOf(module.originalModule) {
            val kotlinVersion = KotlinPlugin.version.propertyValue.version
            buildList {
                +ktorArtifactDependency(serverEngine.reference.settingValue.dependencyName, kotlinVersion)
                +ktorArtifactDependency("ktor-html-builder", kotlinVersion)
                +ArtifactBasedLibraryDependencyIR(
                    MavenArtifact(Repositories.KOTLINX, "org.jetbrains.kotlinx", "kotlinx-html-jvm"),
                    Versions.KOTLINX.KOTLINX_HTML,
                    DependencyType.MAIN
                )
            }
        }

    override fun Writer.getIrsToAddToBuildFile(module: ModuleIR): List<BuildSystemIR> = buildList {
        +RepositoryIR(Repositories.KTOR_BINTRAY)
        +RepositoryIR(DefaultRepository.JCENTER)
        +runTaskIrs(mainClass = "ServerKt")
    }

    override fun Reader.createRunConfigurations(module: ModuleIR): List<WizardRunConfiguration> = buildList {
        +WizardGradleRunConfiguration("Run", "run", emptyList())
    }

    override fun updateTargetIr(module: ModuleIR, targetConfigurationIR: TargetConfigurationIR): TargetConfigurationIR =
        targetConfigurationIR.addWithJavaIntoJvmTarget()

    override fun Reader.getFileTemplates(module: ModuleIR): List<FileTemplateDescriptorWithPath> = listOf(
        FileTemplateDescriptor("$id/server.kt.vm", "server.kt".asPath()) asSrcOf SourcesetType.main
    )

    val serverEngine by enumSetting<KtorServerEngine>(
        KotlinNewProjectWizardBundle.message("module.template.ktor.server.setting.engine"),
        GenerationPhase.PROJECT_GENERATION
    )

    val imports = InterceptionPoint("imports", emptyList<String>())
    val routes = InterceptionPoint("routes", emptyList<String>())
    val elements = InterceptionPoint("elements", emptyList<String>())

    override val interceptionPoints: List<InterceptionPoint<Any>> = listOf(imports, routes, elements)
    override val settings: List<TemplateSetting<*, *>> = listOf(serverEngine)
}

private fun ktorArtifactDependency(@NonNls name: String, kotlinVersion: Version) = ArtifactBasedLibraryDependencyIR(
    MavenArtifact(Repositories.KTOR_BINTRAY, "io.ktor", name),
    Versions.KTOR,
    DependencyType.MAIN
)


@Suppress("MemberVisibilityCanBePrivate")
enum class KtorServerEngine(val engineName: String, val dependencyName: String) : DisplayableSettingItem {
    Netty(
        KotlinNewProjectWizardBundle.message("module.template.ktor.server.setting.engine.netty"),
        dependencyName = "ktor-server-netty"
    ),
    Tomcat(
        KotlinNewProjectWizardBundle.message("module.template.ktor.server.setting.engine.tomcat"),
        dependencyName = "ktor-server-tomcat"
    ),
    Jetty(
        KotlinNewProjectWizardBundle.message("module.template.ktor.server.setting.engine.jetty"),
        dependencyName = "ktor-server-jetty"
    );

    override val text: String
        get() = engineName.capitalize()

    val import: String
        get() = "io.ktor.server.${engineName.decapitalize()}.${engineName.capitalize()}"
}