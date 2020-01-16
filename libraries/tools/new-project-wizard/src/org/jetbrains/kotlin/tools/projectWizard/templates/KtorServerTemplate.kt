/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tools.projectWizard.templates

import org.jetbrains.kotlin.tools.projectWizard.core.TaskRunningContext
import org.jetbrains.kotlin.tools.projectWizard.core.asPath
import org.jetbrains.kotlin.tools.projectWizard.core.buildList
import org.jetbrains.kotlin.tools.projectWizard.core.entity.TemplateSetting
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.*
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.TargetConfigurationIR
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.multiplatform.addWithJavaIntoJvmTarget
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.phases.GenerationPhase
import org.jetbrains.kotlin.tools.projectWizard.plugins.kotlin.ModuleType
import org.jetbrains.kotlin.tools.projectWizard.settings.DisplayableSettingItem
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repositories
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version
import org.jetbrains.kotlin.tools.projectWizard.transformers.interceptors.InterceptionPoint

class KtorServerTemplate : Template() {
    override val title: String = "Ktor-based Server"
    override val htmlDescription: String = title
    override val moduleTypes: Set<ModuleType> = setOf(ModuleType.jvm)
    override val id: String = "ktorServer"

    override fun TaskRunningContext.getRequiredLibraries(module: ModuleIR): List<DependencyIR> =
        withSettingsOf(module.originalModule) {
            buildList {
                +ktorArtifactDependency(serverEngine.reference.settingValue.dependencyName)
                +ktorArtifactDependency("ktor-html-builder")
                +ArtifactBasedLibraryDependencyIR(
                    MavenArtifact(DefaultRepository.JCENTER, "org.jetbrains.kotlinx", "kotlinx-html-jvm"),
                    Version.fromString("0.6.12"),
                    DependencyType.MAIN
                )
            }
        }

    override fun TaskRunningContext.getIrsToAddToBuildFile(module: ModuleIR): List<BuildSystemIR> = buildList {
        +RepositoryIR(Repositories.KTOR_BINTRAY)
        +RepositoryIR(DefaultRepository.JCENTER)
        +runTaskIrs(mainClass = "ServerKt")
    }

    override fun updateTargetIr(module: ModuleIR, targetConfigurationIR: TargetConfigurationIR): TargetConfigurationIR =
        targetConfigurationIR.addWithJavaIntoJvmTarget()

    override fun TaskRunningContext.getFileTemplates(module: ModuleIR): List<FileTemplateDescriptorWithPath> = listOf(
        FileTemplateDescriptor("$id/server.kt.vm", "server.kt".asPath()) asSrcOf SourcesetType.main
    )

    val serverEngine by enumSetting<KtorServerEngine>("Ktor Server", GenerationPhase.PROJECT_GENERATION)

    val imports = InterceptionPoint("imports", emptyList<String>())
    val routes = InterceptionPoint("routes", emptyList<String>())
    val elements = InterceptionPoint("elements", emptyList<String>())

    override val interceptionPoints: List<InterceptionPoint<Any>> = listOf(imports, routes, elements)
    override val settings: List<TemplateSetting<*, *>> = listOf(serverEngine)
}

private fun ktorArtifactDependency(name: String) = ArtifactBasedLibraryDependencyIR(
    MavenArtifact(Repositories.KTOR_BINTRAY, "io.ktor", name),
    Version.fromString("1.2.6"),
    DependencyType.MAIN
)


@Suppress("MemberVisibilityCanBePrivate")
enum class KtorServerEngine(val engineName: String, val dependencyName: String) : DisplayableSettingItem {
    Netty("Netty", dependencyName = "ktor-server-netty"),
    Tomcat("Tomcat", dependencyName = "ktor-server-tomcat"),
    Jetty("Jetty", dependencyName = "ktor-server-jetty");

    override val text: String
        get() = engineName.capitalize()

    val import: String
        get() = "io.ktor.server.${engineName.decapitalize()}.${engineName.capitalize()}"
}