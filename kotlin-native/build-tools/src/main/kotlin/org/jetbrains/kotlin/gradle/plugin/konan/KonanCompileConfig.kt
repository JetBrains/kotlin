/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

    override fun srcDir(dir: Any) = tasks().forEach { it.configure { srcDir(dir) } }
    override fun srcFiles(vararg files: Any) = tasks().forEach { it.configure { srcFiles(*files) } }
    override fun srcFiles(files: Collection<Any>) = tasks().forEach { it.configure { srcFiles(files) } }

    override fun nativeLibrary(lib: Any) = tasks().forEach { it.configure { nativeLibrary(lib) } }
    override fun nativeLibraries(vararg libs: Any) = tasks().forEach { it.configure { nativeLibraries(*libs) } }
    override fun nativeLibraries(libs: FileCollection) = tasks().forEach { it.configure { nativeLibraries(libs) } }

    @Deprecated("Use commonSourceSets instead", ReplaceWith("commonSourceSets(sourceSetName)"))
    override fun commonSourceSet(sourceSetName: String) = tasks().forEach { it.configure { commonSourceSets(sourceSetName) } }
    override fun commonSourceSets(vararg sourceSetNames: String) = tasks().forEach { it.configure { commonSourceSets(*sourceSetNames) } }
    override fun enableMultiplatform(flag: Boolean) = tasks().forEach { it.configure { enableMultiplatform(flag) } }

    override fun commonSrcDir(dir: Any) = tasks().forEach { it.configure { commonSrcDir(dir) } }
    override fun commonSrcFiles(vararg files: Any) = tasks().forEach { it.configure { commonSrcFiles(*files) } }
    override fun commonSrcFiles(files: Collection<Any>) = tasks().forEach { it.configure { commonSrcFiles(files) } }

    override fun linkerOpts(values: List<String>) = tasks().forEach { it.configure { linkerOpts(values) } }
    override fun linkerOpts(vararg values: String) = tasks().forEach { it.configure { linkerOpts(*values) } }

    override fun enableDebug(flag: Boolean) = tasks().forEach { it.configure { enableDebug(flag) } }
    override fun noStdLib(flag: Boolean) = tasks().forEach { it.configure { noStdLib(flag) } }
    override fun noMain(flag: Boolean) = tasks().forEach { it.configure { noMain(flag) } }
    override fun noPack(flag: Boolean) = tasks().forEach { it.configure { noPack(flag) } }
    override fun enableOptimizations(flag: Boolean) = tasks().forEach { it.configure { enableOptimizations(flag) } }
    override fun enableAssertions(flag: Boolean) = tasks().forEach { it.configure { enableAssertions(flag) } }

    override fun entryPoint(entryPoint: String) = tasks().forEach { it.configure { entryPoint(entryPoint) } }

    override fun measureTime(flag: Boolean) = tasks().forEach { it.configure { measureTime(flag) } }

    override fun dependencies(closure: Closure<Unit>) = tasks().forEach { it.configure { dependencies(closure) } }
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