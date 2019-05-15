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

package org.jetbrains.kotlin.gradle.plugin.konan

import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.plugin.tasks.*
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.KonanTarget.WASM32
import java.io.File

abstract class KonanCompileConfig<T: KonanCompileTask>(name: String,
                                                       type: Class<T>,
                                                       project: ProjectInternal,
                                                       targets: Iterable<String>)
    : KonanBuildingConfig<T>(name, type, project, targets), KonanCompileSpec {

    protected abstract val typeForDescription: String

    override fun generateTaskDescription(task: T) =
            "Build the Kotlin/Native $typeForDescription '${task.name}' for target '${task.konanTarget}'"

    override fun generateAggregateTaskDescription(task: Task) =
            "Build the Kotlin/Native $typeForDescription '${task.name}' for all supported and declared targets"

    override fun generateTargetAliasTaskDescription(task: Task, targetName: String) =
            "Build the Kotlin/Native $typeForDescription '${task.name}' for target '$targetName'"

    override fun srcDir(dir: Any) = forEach { it.srcDir(dir) }
    override fun srcFiles(vararg files: Any) = forEach { it.srcFiles(*files) }
    override fun srcFiles(files: Collection<Any>) = forEach { it.srcFiles(files) }

    override fun nativeLibrary(lib: Any) = forEach { it.nativeLibrary(lib) }
    override fun nativeLibraries(vararg libs: Any) = forEach { it.nativeLibraries(*libs) }
    override fun nativeLibraries(libs: FileCollection) = forEach { it.nativeLibraries(libs) }

    @Deprecated("Use commonSourceSets instead", ReplaceWith("commonSourceSets(sourceSetName)"))
    override fun commonSourceSet(sourceSetName: String) = forEach { it.commonSourceSets(sourceSetName) }
    override fun commonSourceSets(vararg sourceSetNames: String) = forEach { it.commonSourceSets(*sourceSetNames) }
    override fun enableMultiplatform(flag: Boolean) = forEach { it.enableMultiplatform(flag) }

    override fun linkerOpts(values: List<String>) = forEach { it.linkerOpts(values) }
    override fun linkerOpts(vararg values: String) = forEach { it.linkerOpts(*values) }

    override fun enableDebug(flag: Boolean) = forEach { it.enableDebug(flag) }
    override fun noStdLib(flag: Boolean) = forEach { it.noStdLib(flag) }
    override fun noMain(flag: Boolean) = forEach { it.noMain(flag) }
    override fun enableOptimizations(flag: Boolean) = forEach { it.enableOptimizations(flag) }
    override fun enableAssertions(flag: Boolean) = forEach { it.enableAssertions(flag) }

    override fun entryPoint(entryPoint: String) = forEach { it.entryPoint(entryPoint) }

    override fun measureTime(flag: Boolean) = forEach { it.measureTime(flag) }

    override fun dependencies(closure: Closure<Unit>) = forEach { it.dependencies(closure) }
}

open class KonanProgram(name: String,
                        project: ProjectInternal,
                        targets: Iterable<String> = project.konanExtension.targets
) : KonanCompileConfig<KonanCompileProgramTask>(name,
        KonanCompileProgramTask::class.java,
        project,
        targets
) {
    override val typeForDescription: String
        get() = "executable"

    override val defaultBaseDir: File
        get() = project.konanBinBaseDir
}

open class KonanDynamic(name: String,
                        project: ProjectInternal,
                        targets: Iterable<String> = project.konanExtension.targets)
    : KonanCompileConfig<KonanCompileDynamicTask>(name,
        KonanCompileDynamicTask::class.java,
        project,
        targets
) {
    override val typeForDescription: String
        get() = "dynamic library"

    override val defaultBaseDir: File
        get() = project.konanBinBaseDir

    override fun targetIsSupported(target: KonanTarget): Boolean = target != WASM32
}

open class KonanFramework(name: String,
                          project: ProjectInternal,
                          targets: Iterable<String> = project.konanExtension.targets)
    : KonanCompileConfig<KonanCompileFrameworkTask>(name,
        KonanCompileFrameworkTask::class.java,
        project,
        targets
) {
    override val typeForDescription: String
        get() = "framework"

    override val defaultBaseDir: File
        get() = project.konanBinBaseDir

    override fun targetIsSupported(target: KonanTarget): Boolean =
        target.family.isAppleFamily
}

open class KonanLibrary(name: String,
                        project: ProjectInternal,
                        targets: Iterable<String> = project.konanExtension.targets)
    : KonanCompileConfig<KonanCompileLibraryTask>(name,
        KonanCompileLibraryTask::class.java,
        project,
        targets
) {
    override val typeForDescription: String
        get() = "library"

    override val defaultBaseDir: File
        get() = project.konanLibsBaseDir
}

open class KonanBitcode(name: String,
                        project: ProjectInternal,
                        targets: Iterable<String> = project.konanExtension.targets)
    : KonanCompileConfig<KonanCompileBitcodeTask>(name,
        KonanCompileBitcodeTask::class.java,
        project,
        targets
) {
    override val typeForDescription: String
        get() = "bitcode"

    override fun generateTaskDescription(task: KonanCompileBitcodeTask) =
            "Generates bitcode for the artifact '${task.name}' and target '${task.konanTarget}'"

    override fun generateAggregateTaskDescription(task: Task) =
            "Generates bitcode for the artifact '${task.name}' for all supported and declared targets'"

    override fun generateTargetAliasTaskDescription(task: Task, targetName: String) =
            "Generates bitcode for the artifact '${task.name}' for '$targetName'"

    override val defaultBaseDir: File
        get() = project.konanBitcodeBaseDir
}