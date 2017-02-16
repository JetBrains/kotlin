/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jps.build

import com.intellij.util.Consumer
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.jetbrains.jps.model.java.JpsJavaModuleType
import org.jetbrains.jps.model.library.JpsOrderRootType
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.util.JpsPathUtil
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.File
import java.util.*

object JpsJsModuleUtils {
    fun getLibraryFilesAndDependencies(target: ModuleBuildTarget): List<String> {
        val result = ArrayList<String>()
        getLibraryFiles(target, result)
        getDependencyModulesAndSources(target, result)
        return result
    }

    fun getLibraryFiles(target: ModuleBuildTarget, result: MutableList<String>) {
        val libraries = JpsUtils.getAllDependencies(target).libraries
        for (library in libraries) {
            for (root in library.getRoots(JpsOrderRootType.COMPILED)) {
                result.add(JpsPathUtil.urlToPath(root.url))
            }
        }
    }

    fun getDependencyModulesAndSources(target: ModuleBuildTarget, result: MutableList<String>) {
        JpsUtils.getAllDependencies(target).processModules(object : Consumer<JpsModule> {
            override fun consume(module: JpsModule) {
                if (module.moduleType != JpsJavaModuleType.INSTANCE) return

                if ((module != target.module || target.isTests) && module.sourceRoots.any { it.rootType == JavaSourceRootType.SOURCE}) {
                    addTarget(module, JavaModuleBuildTargetType.PRODUCTION)
                }

                if (module != target.module && target.isTests && module.sourceRoots.any { it.rootType == JavaSourceRootType.TEST_SOURCE}) {
                    addTarget(module, JavaModuleBuildTargetType.TEST)
                }
            }

            fun addTarget(module: JpsModule, targetType: JavaModuleBuildTargetType) {
                val moduleBuildTarget = ModuleBuildTarget(module, targetType)
                val outputDir = KotlinBuilderModuleScriptGenerator.getOutputDirSafe(moduleBuildTarget)
                val metaInfoFile = getOutputMetaFile(outputDir, module.name)
                if (metaInfoFile.exists()) {
                    result.add(metaInfoFile.absolutePath)
                }
            }
        })
    }

    @JvmStatic
    fun getOutputFile(outputDir: File, moduleName: String) = File(outputDir, moduleName + KotlinJavascriptMetadataUtils.JS_EXT)

    @JvmStatic
    fun getOutputMetaFile(outputDir: File, moduleName: String) = File(outputDir, moduleName + KotlinJavascriptMetadataUtils.META_JS_SUFFIX)
}
