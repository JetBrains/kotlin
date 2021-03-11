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
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.util.Key
import org.gradle.tooling.model.idea.IdeaModule
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

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