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

package org.jetbrains.kotlin.annotation.plugin.ide

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.*
import com.intellij.openapi.util.Key
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.kotlin.gradle.AbstractKotlinGradleModelBuilder
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension
import org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder
import java.io.Serializable
import java.lang.Exception

interface DumpedPluginModel {
    val className: String

    // May contain primitives, Strings and collections of primitives/Strings
    val args: Array<*>

    operator fun component1() = className
    operator fun component2() = args
}

class DumpedPluginModelImpl(
    override val className: String,
    override val args: Array<*>
) : DumpedPluginModel, Serializable {
    constructor(clazz: Class<*>, vararg args: Any?) : this(clazz.canonicalName, args)
}

interface AnnotationBasedPluginModel : Serializable {
    val annotations: List<String>
    val presets: List<String>

    /*
        Objects returned from Gradle importer are implicitly wrapped in a proxy that can potentially leak internal Gradle structures.
        So we need a way to safely serialize the arbitrary annotation plugin model.
     */
    fun dump(): DumpedPluginModel

    val isEnabled get() = annotations.isNotEmpty() || presets.isNotEmpty()
}

@Suppress("unused")
abstract class AnnotationBasedPluginProjectResolverExtension<T : AnnotationBasedPluginModel> : AbstractProjectResolverExtension() {
    private companion object {
        private val LOG = Logger.getInstance(AnnotationBasedPluginProjectResolverExtension::class.java)
    }

    abstract val modelClass: Class<T>
    abstract val userDataKey: Key<T>

    override fun getExtraProjectModelClasses() = setOf(modelClass)

    override fun getToolingExtensionsClasses() = setOf(
            modelClass,
            AnnotationBasedPluginProjectResolverExtension::class.java,
            Unit::class.java)

    override fun populateModuleExtraModels(gradleModule: IdeaModule, ideModule: DataNode<ModuleData>) {
        val model = resolverCtx.getExtraProject(gradleModule, modelClass)

        if (model != null) {
            val (className, args) = model.dump()

            @Suppress("UNCHECKED_CAST")
            val refurbishedModel = Class.forName(className).constructors.single().newInstance(*args) as T

            ideModule.putCopyableUserData(userDataKey, refurbishedModel)
        }

        super.populateModuleExtraModels(gradleModule, ideModule)
    }
}

abstract class AnnotationBasedPluginModelBuilderService<T : AnnotationBasedPluginModel> : AbstractKotlinGradleModelBuilder() {
    abstract val gradlePluginNames: List<String>
    abstract val extensionName: String

    abstract val modelClass: Class<T>
    abstract fun createModel(annotations: List<String>, presets: List<String>, extension: Any?): T

    override fun getErrorMessageBuilder(project: Project, e: Exception): ErrorMessageBuilder {
        return ErrorMessageBuilder.create(project, e, "Gradle import errors")
                .withDescription("Unable to build $gradlePluginNames plugin configuration")
    }

    override fun canBuild(modelName: String?): Boolean = modelName == modelClass.name

    override fun buildAll(modelName: String?, project: Project): Any {
        val plugin: Plugin<*>? =  project.findPlugin(gradlePluginNames)
        val extension: Any? = project.extensions.findByName(extensionName)

        val annotations = mutableListOf<String>()
        val presets = mutableListOf<String>()

        if (plugin != null && extension != null) {
            annotations += extension.getList("myAnnotations")
            presets += extension.getList("myPresets")
            return createModel(annotations, presets, extension)
        }

        return createModel(emptyList(), emptyList(), null)
    }

    private fun Project.findPlugin(names: List<String>): Plugin<*>? {
        for (name in names) {
            plugins.findPlugin(name)?.let { return it }
        }

        return null
    }

    private fun Any.getList(fieldName: String): List<String> {
        @Suppress("UNCHECKED_CAST")
        return getFieldValue(fieldName) as? List<String> ?: emptyList()
    }

    protected fun Any.getFieldValue(fieldName: String, clazz: Class<*> = this.javaClass): Any? {
        val field = clazz.declaredFields.firstOrNull { it.name == fieldName }
                    ?: return getFieldValue(fieldName, clazz.superclass ?: return null)

        val oldIsAccessible = field.isAccessible
        try {
            field.isAccessible = true
            return field.get(this)
        } finally {
            field.isAccessible = oldIsAccessible
        }
    }
}