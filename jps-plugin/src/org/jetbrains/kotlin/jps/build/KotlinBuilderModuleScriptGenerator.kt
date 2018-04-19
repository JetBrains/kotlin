/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.util.SmartList
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import com.intellij.util.io.URLUtil
import org.jetbrains.jps.ModuleChunk
import org.jetbrains.jps.builders.java.JavaModuleBuildTargetType
import org.jetbrains.jps.incremental.CompileContext
import org.jetbrains.jps.incremental.ModuleBuildTarget
import org.jetbrains.jps.incremental.ProjectBuildException
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.module.JpsSdkDependency
import org.jetbrains.kotlin.build.JvmSourceRoot
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.jps.build.JpsUtils.getAllDependencies
import org.jetbrains.kotlin.jps.productionOutputFilePath
import org.jetbrains.kotlin.jps.testOutputFilePath
import org.jetbrains.kotlin.modules.KotlinModuleXmlBuilder
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File
import java.io.IOException
import java.util.*

object KotlinBuilderModuleScriptGenerator {
    fun getRelatedProductionModule(module: JpsModule): JpsModule? =
        JpsJavaExtensionService.getInstance().getTestModuleProperties(module)?.productionModule

    fun generateModuleDescription(
        context: CompileContext,
        chunk: ModuleChunk,
        sourceFiles: MultiMap<ModuleBuildTarget, File>, // ignored for non-incremental compilation
        hasRemovedFiles: Boolean
    ): File? {
        val builder = KotlinModuleXmlBuilder()

        var noSources = true

        val outputDirs = HashSet<File>()
        for (target in chunk.targets) {
            outputDirs.add(getOutputDirSafe(target))
        }

        val logger = context.loggingManager.projectBuilderLogger
        for (target in chunk.targets) {
            val outputDir = getOutputDirSafe(target)
            val friendDirs = getAdditionalOutputDirsWhereInternalsAreVisible(target)

            val moduleSources = ArrayList(
                if (IncrementalCompilation.isEnabled())
                    sourceFiles.get(target)
                else
                    KotlinSourceFileCollector.getAllKotlinSourceFiles(target)
            )

            if (moduleSources.size > 0 || hasRemovedFiles) {
                noSources = false

                if (logger.isEnabled) {
                    logger.logCompiledFiles(moduleSources, KotlinBuilder.KOTLIN_BUILDER_NAME, "Compiling files:")
                }
            }

            val targetType = target.targetType
            assert(targetType is JavaModuleBuildTargetType)
            val targetId = TargetId(target)
            builder.addModule(
                targetId.name,
                outputDir.absolutePath,
                moduleSources,
                findSourceRoots(context, target),
                findClassPathRoots(target),
                findModularJdkRoot(target),
                targetId.type,
                (targetType as JavaModuleBuildTargetType).isTests,
                // this excludes the output directories from the class path, to be removed for true incremental compilation
                outputDirs,
                friendDirs
            )
        }

        if (noSources) return null

        val scriptFile = createTempFileForModuleDesc(chunk)
        FileUtil.writeToFile(scriptFile, builder.asText().toString())
        return scriptFile
    }

    private fun createTempFileForModuleDesc(chunk: ModuleChunk): File {
        val readableSuffix = buildString {
            append(StringUtil.sanitizeJavaIdentifier(chunk.representativeTarget().module.name))
            if (chunk.containsTests()) {
                append("-test")
            }
        }
        val dir = System.getProperty("kotlin.jps.dir.for.module.files")?.let { File(it) }?.takeIf { it.isDirectory }
        return try {
            File.createTempFile("kjps", readableSuffix + ".script.xml", dir)
        } catch (e: IOException) {
            // sometimes files cannot be created, because file name is too long (Windows, Mac OS)
            // see https://bugs.openjdk.java.net/browse/JDK-8148023
            try {
                File.createTempFile("kjps", ".script.xml", dir)
            } catch (e: IOException) {
                val message = buildString {
                    append("Could not create module file when building chunk $chunk")
                    if (dir != null) {
                        append(" in dir $dir")
                    }
                }
                throw RuntimeException(message, e)
            }
        }
    }

    fun getOutputDirSafe(target: ModuleBuildTarget): File {
        val explicitOutputPath = if (target.isTests) target.module.testOutputFilePath else target.module.productionOutputFilePath
        val explicitOutputDir = explicitOutputPath?.let { File(it).absoluteFile.parentFile }
        return explicitOutputDir ?: target.outputDir ?: throw ProjectBuildException("No output directory found for " + target)
    }

    fun getProductionModulesWhichInternalsAreVisible(from: ModuleBuildTarget): List<JpsModule> {
        if (!from.isTests) return emptyList()

        val result = SmartList<JpsModule>(from.module)
        result.addIfNotNull(getRelatedProductionModule(from.module))

        return result.filter { it.hasProductionSourceRoot }
    }

    fun getAdditionalOutputDirsWhereInternalsAreVisible(target: ModuleBuildTarget): List<File> {
        return getProductionModulesWhichInternalsAreVisible(target).mapNotNullTo(SmartList<File>()) {
            JpsJavaExtensionService.getInstance().getOutputDirectory(it, false)
        }
    }

    private fun findClassPathRoots(target: ModuleBuildTarget): Collection<File> {
        return getAllDependencies(target).classes().roots.filter { file ->
            if (!file.exists()) {
                val extension = file.extension

                // Don't filter out files, we want to report warnings about absence through the common place
                if (!(extension == "class" || extension == "jar")) {
                    return@filter false
                }
            }

            true
        }
    }

    private fun findModularJdkRoot(target: ModuleBuildTarget): File? {
        // List of paths to JRE modules in the following format:
        // jrt:///Library/Java/JavaVirtualMachines/jdk-9.jdk/Contents/Home!/java.base
        val urls = JpsJavaExtensionService.dependencies(target.module)
            .satisfying { dependency -> dependency is JpsSdkDependency }
            .classes().urls

        val url = urls.firstOrNull { it.startsWith(StandardFileSystems.JRT_PROTOCOL_PREFIX) } ?: return null

        return File(url.substringAfter(StandardFileSystems.JRT_PROTOCOL_PREFIX).substringBeforeLast(URLUtil.JAR_SEPARATOR))
    }

    private fun findSourceRoots(context: CompileContext, target: ModuleBuildTarget): List<JvmSourceRoot> {
        val roots = context.projectDescriptor.buildRootIndex.getTargetRoots(target, context)
        val result = ContainerUtil.newArrayList<JvmSourceRoot>()
        for (root in roots) {
            val file = root.rootFile
            val prefix = root.packagePrefix
            if (file.exists()) {
                result.add(JvmSourceRoot(file, if (prefix.isEmpty()) null else prefix))
            }
        }
        return result
    }
}
