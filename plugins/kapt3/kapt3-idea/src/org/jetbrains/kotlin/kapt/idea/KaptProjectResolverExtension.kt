/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.kapt.idea

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModelBuilder.Companion.getCompilations
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModelBuilder.Companion.getCompileKotlinTaskName
import org.jetbrains.kotlin.gradle.KotlinMPPGradleModelBuilder.Companion.getTargets
import org.jetbrains.kotlin.idea.framework.GRADLE_SYSTEM_ID
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import java.io.File
import java.io.Serializable
import java.lang.Exception
import java.lang.reflect.Modifier

interface KaptSourceSetModel : Serializable {
    val sourceSetName: String
    val isTest: Boolean
    val generatedSourcesDir: String
    val generatedClassesDir: String
    val generatedKotlinSourcesDir: String

    val generatedSourcesDirFile get() = generatedSourcesDir.takeIf { it.isNotEmpty() }?.let(::File)
    val generatedClassesDirFile get() = generatedClassesDir.takeIf { it.isNotEmpty() }?.let(::File)
    val generatedKotlinSourcesDirFile get() = generatedKotlinSourcesDir.takeIf { it.isNotEmpty() }?.let(::File)
}

class KaptSourceSetModelImpl(
        override val sourceSetName: String,
        override val isTest: Boolean,
        override val generatedSourcesDir: String,
        override val generatedClassesDir: String,
        override val generatedKotlinSourcesDir: String
) : KaptSourceSetModel

interface KaptGradleModel : Serializable {
    val isEnabled: Boolean
    val buildDirectory: File
    val sourceSets: List<KaptSourceSetModel>
}

class KaptGradleModelImpl(
        override val isEnabled: Boolean,
        override val buildDirectory: File,
        override val sourceSets: List<KaptSourceSetModel>
) : KaptGradleModel

@Suppress("unused")
class KaptProjectResolverExtension : AbstractProjectResolverExtension() {
    private companion object {
        private val LOG = Logger.getInstance(KaptProjectResolverExtension::class.java)
    }

    override fun getExtraProjectModelClasses() = setOf(KaptGradleModel::class.java)
    override fun getToolingExtensionsClasses() = setOf(KaptModelBuilderService::class.java, Unit::class.java)

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val kaptModel = resolverCtx.getExtraProject(gradleModule, KaptGradleModel::class.java)

        if (kaptModel != null && kaptModel.isEnabled) {
            for (sourceSet in kaptModel.sourceSets) {
                val parentDataNode = ideModule.findParentForSourceSetDataNode(sourceSet.sourceSetName) ?: continue

                fun addSourceSet(path: String, type: ExternalSystemSourceType) {
                    val contentRootData = ContentRootData(GRADLE_SYSTEM_ID, path)
                    contentRootData.storePath(type, path)
                    parentDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRootData)
                }

                val sourceType =
                    if (sourceSet.isTest) ExternalSystemSourceType.TEST_GENERATED else ExternalSystemSourceType.SOURCE_GENERATED
                sourceSet.generatedSourcesDirFile?.let { addSourceSet(it.absolutePath, sourceType) }
                sourceSet.generatedKotlinSourcesDirFile?.let { addSourceSet(it.absolutePath, sourceType) }

                sourceSet.generatedClassesDirFile?.let { generatedClassesDir ->
                    val libraryData = LibraryData(GRADLE_SYSTEM_ID, "kaptGeneratedClasses")
                    val existingNode =
                        parentDataNode.children.map { (it.data as? LibraryDependencyData)?.target }
                            .firstOrNull { it?.externalName == libraryData.externalName }
                    if (existingNode != null) {
                        existingNode.addPath(LibraryPathType.BINARY, generatedClassesDir.absolutePath)
                    } else {
                        libraryData.addPath(LibraryPathType.BINARY, generatedClassesDir.absolutePath)
                        val libraryDependencyData = LibraryDependencyData(parentDataNode.data, libraryData, LibraryLevel.MODULE)
                        parentDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyData)
                    }
                }
            }
        }

        super.populateModuleExtraModels(gradleModule, ideModule)
    }

    private fun DataNode<ModuleData>.findParentForSourceSetDataNode(sourceSetName: String): DataNode<ModuleData>? {
        val moduleName = data.id
        for (child in children) {
            val gradleSourceSetData = child.data as? GradleSourceSetData ?: continue
            if (gradleSourceSetData.id == "$moduleName:$sourceSetName") {
                @Suppress("UNCHECKED_CAST")
                return child as? DataNode<ModuleData>
            }
        }

        return this
    }
}

class KaptModelBuilderService : AbstractKotlinGradleModelBuilder() {
    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(project, e, "Gradle import errors")
                .withDescription("Unable to build kotlin-kapt plugin configuration")
    }

    override fun canBuild(modelName: String?): Boolean = modelName == KaptGradleModel::class.java.name

    override fun buildAll(modelName: String?, project: Project): Any {
        val kaptPlugin: Plugin<*>? = project.plugins.findPlugin("kotlin-kapt")
        val kaptIsEnabled = kaptPlugin != null

        val sourceSets = mutableListOf<KaptSourceSetModel>()

        if (kaptIsEnabled) {
            val targets = project.getTargets()

            fun handleCompileTask(moduleName: String, compileTask: Task) {
                if (compileTask.javaClass.name !in kotlinCompileJvmTaskClasses) {
                    return
                }

                val sourceSetName = compileTask.getSourceSetName()
                val isTest = sourceSetName.toLowerCase().endsWith("test")

                val kaptGeneratedSourcesDir = getKaptDirectory("getKaptGeneratedSourcesDir", project, sourceSetName)
                val kaptGeneratedClassesDir = getKaptDirectory("getKaptGeneratedClassesDir", project, sourceSetName)
                val kaptGeneratedKotlinSourcesDir = getKaptDirectory("getKaptGeneratedKotlinSourcesDir", project, sourceSetName)
                sourceSets += KaptSourceSetModelImpl(
                    moduleName, isTest, kaptGeneratedSourcesDir, kaptGeneratedClassesDir, kaptGeneratedKotlinSourcesDir
                )
            }

            if (targets != null && targets.isNotEmpty()) {
                for (target in targets) {
                    if (!isWithJavaEnabled(target)) {
                        continue
                    }

                    val compilations = getCompilations(target) ?: continue
                    for (compilation in compilations) {
                        val compileTask = getCompileKotlinTaskName(project, compilation) ?: continue
                        val moduleName = target.name + compilation.name.capitalize()
                        handleCompileTask(moduleName, compileTask)
                    }
                }
            } else {
                project.getAllTasks(false)[project]?.forEach { compileTask ->
                    val sourceSetName = compileTask.getSourceSetName()
                    handleCompileTask(sourceSetName, compileTask)
                }
            }
        }

        return KaptGradleModelImpl(kaptIsEnabled, project.buildDir, sourceSets)
    }

    private fun isWithJavaEnabled(target: Named): Boolean {
        val getWithJavaEnabledMethod = target.javaClass.methods
            .firstOrNull { it.name == "getWithJavaEnabled" && it.parameterCount == 0 } ?: return false

        return getWithJavaEnabledMethod.invoke(target) == true
    }

    private fun getKaptDirectory(funName: String, project: Project, sourceSetName: String): String {
        val kotlinKaptPlugin = project.plugins.findPlugin("kotlin-kapt") ?: return ""

        val targetMethod = kotlinKaptPlugin::class.java.methods.firstOrNull {
            Modifier.isStatic(it.modifiers) && it.name == funName && it.parameterCount == 2
        } ?: return ""

        return (targetMethod(null, project, sourceSetName) as? File)?.absolutePath ?: ""
    }
}