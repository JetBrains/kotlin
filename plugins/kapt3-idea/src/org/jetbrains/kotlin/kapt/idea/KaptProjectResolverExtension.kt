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

import com.android.tools.idea.gradle.project.model.AndroidModuleModel
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.*
import org.gradle.api.Project
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder
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
    override fun getExtraProjectModelClasses() = setOf(KaptGradleModel::class.java)
    override fun getToolingExtensionsClasses() = setOf(KaptModelBuilderService::class.java)

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val kaptModel = resolverCtx.getExtraProject(gradleModule, KaptGradleModel::class.java) ?: return

        if (kaptModel.isEnabled) {
            for (sourceSet in kaptModel.sourceSets) {
                populateAndroidModuleModelIfNeeded(ideModule, sourceSet)

                val sourceSetDataNode = ideModule.findGradleSourceSet(sourceSet.sourceSetName) ?: continue

                fun addSourceSet(path: String, type: ExternalSystemSourceType) {
                    val contentRootData = ContentRootData(GRADLE_SYSTEM_ID, path)
                    contentRootData.storePath(type, path)
                    val child = sourceSetDataNode.createChild(ProjectKeys.CONTENT_ROOT, contentRootData)
                    sourceSetDataNode.addChild(child)
                }

                val sourceType = if (sourceSet.isTest) ExternalSystemSourceType.TEST_GENERATED else ExternalSystemSourceType.SOURCE_GENERATED
                sourceSet.generatedSourcesDirFile?.let { addSourceSet(it.absolutePath, sourceType) }
                sourceSet.generatedKotlinSourcesDirFile?.let { addSourceSet(it.absolutePath, sourceType) }

                sourceSet.generatedClassesDirFile?.let { generatedClassesDir ->
                    val libraryData = LibraryData(GRADLE_SYSTEM_ID, "Kapt generated classes")
                    libraryData.addPath(LibraryPathType.BINARY, generatedClassesDir.absolutePath)
                    val libraryDependencyData = LibraryDependencyData(ideModule.data, libraryData, LibraryLevel.MODULE)
                    val child = sourceSetDataNode.createChild(ProjectKeys.LIBRARY_DEPENDENCY, libraryDependencyData)
                    sourceSetDataNode.addChild(child)
                }
            }
        }

        super.populateModuleExtraModels(gradleModule, ideModule)
    }

    private fun populateAndroidModuleModelIfNeeded(ideModule: DataNode<ModuleData>, sourceSet: KaptSourceSetModel) {
        if (true) return

        ideModule.findAndroidModuleModel()?.let { androidModelAny ->
            // We can cast to AndroidModuleModel cause we already checked in findAndroidModuleModel() that the class exists

            val androidModel = androidModelAny.data as? AndroidModuleModel
            val generatedKotlinSources = sourceSet.generatedKotlinSourcesDirFile

            if (androidModel != null && generatedKotlinSources != null) {
                // Looks like we can't add generated class root as a library dependency
                androidModel.registerExtraGeneratedSourceFolder(generatedKotlinSources)
            }
        }
    }

    private fun DataNode<ModuleData>.findAndroidModuleModel(): DataNode<*>? {
        val modelClassName = "com.android.tools.idea.gradle.project.model.AndroidModuleModel"
        val node = children.firstOrNull { it.key.dataType == modelClassName } ?: return null
        return if (!hasClassInClasspath(modelClassName)) null else node
    }

    private fun hasClassInClasspath(name: String): Boolean {
        return try {
            Class.forName(name) != null
        } catch (thr: Throwable) {
            false
        }
    }

    private fun DataNode<ModuleData>.findGradleSourceSet(sourceSetName: String): DataNode<GradleSourceSetData>? {
        val moduleName = data.id
        for (child in children) {
            val gradleSourceSetData = child.data as? GradleSourceSetData ?: continue
            if (gradleSourceSetData.id == "$moduleName:$sourceSetName") {
                @Suppress("UNCHECKED_CAST")
                return child as DataNode<GradleSourceSetData>?
            }
        }

        return null
    }
}

class KaptModelBuilderService : AbstractKotlinGradleModelBuilder() {
    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(project, e, "Gradle import errors")
                .withDescription("Unable to build kotlin-kapt plugin configuration")
    }

    override fun canBuild(modelName: String?): Boolean = modelName == KaptGradleModel::class.java.name

    override fun buildAll(modelName: String?, project: Project): Any {
        val kaptPlugin = project.plugins.findPlugin("kotlin-kapt")
        val kaptIsEnabled = kaptPlugin != null

        val sourceSets = mutableListOf<KaptSourceSetModel>()

        if (kaptIsEnabled) {
            project.getAllTasks(false)[project]?.forEach { compileTask ->
                if (compileTask.javaClass.name !in kotlinCompileTaskClasses) return@forEach

                val sourceSetName = compileTask.getSourceSetName()
                val isTest = sourceSetName.toLowerCase().endsWith("test")

                val kaptGeneratedSourcesDir = getKaptDirectory("getKaptGeneratedSourcesDir", project, sourceSetName)
                val kaptGeneratedClassesDir = getKaptDirectory("getKaptGeneratedClassesDir", project, sourceSetName)
                val kaptGeneratedKotlinSourcesDir = getKaptDirectory("getKaptGeneratedKotlinSourcesDir", project, sourceSetName)
                sourceSets += KaptSourceSetModelImpl(
                        sourceSetName, isTest, kaptGeneratedSourcesDir, kaptGeneratedClassesDir, kaptGeneratedKotlinSourcesDir)
            }
        }

        return KaptGradleModelImpl(kaptIsEnabled, project.buildDir, sourceSets)
    }

    private fun getKaptDirectory(funName: String, project: Project, sourceSetName: String): String {
        val kotlinKaptPlugin = project.plugins.findPlugin("kotlin-kapt") ?: return ""

        val targetMethod = kotlinKaptPlugin::class.java.methods.firstOrNull {
            Modifier.isStatic(it.modifiers) && it.name == funName && it.parameterCount == 2
        } ?: return ""

        return (targetMethod(null, project, sourceSetName) as? File)?.absolutePath ?: ""
    }
}