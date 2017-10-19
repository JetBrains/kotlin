package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileBitcodeTask
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileLibraryTask
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileProgramTask
import org.jetbrains.kotlin.gradle.plugin.tasks.KonanCompileTask
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

abstract class KonanCompileConfig<T: KonanCompileTask>(name: String,
                                                       type: Class<T>,
                                                       project: ProjectInternal,
                                                       instantiator: Instantiator)
    : KonanBuildingConfig<T>(name, type, project, instantiator), KonanCompileSpec {

    protected abstract val typeForDescription: String

    override fun generateTaskDescription(task: T) =
            "Build the Kotlin/Native $typeForDescription '${task.name}' for target '${task.konanTarget}'"

    override fun generateAggregateTaskDescription(task: Task) =
            "Build the Kotlin/Native $typeForDescription '${task.name}' for all supported and declared targets"

    override fun generateHostTaskDescription(task: Task, hostTarget: KonanTarget) =
            "Build the Kotlin/Native $typeForDescription '${task.name}' for current host"

    override fun srcDir(dir: Any) = forEach { it.srcDir(dir) }
    override fun srcFiles(vararg files: Any) = forEach { it.srcFiles(*files) }
    override fun srcFiles(files: Collection<Any>) = forEach { it.srcFiles(files) }

    override fun nativeLibrary(lib: Any) = forEach { it.nativeLibrary(lib) }
    override fun nativeLibraries(vararg libs: Any) = forEach { it.nativeLibraries(*libs) }
    override fun nativeLibraries(libs: FileCollection) = forEach { it.nativeLibraries(libs) }

    override fun linkerOpts(values: List<String>) = forEach { it.linkerOpts(values) }
    override fun linkerOpts(vararg values: String) = forEach { it.linkerOpts(*values) }

    override fun enableDebug(flag: Boolean) = forEach { it.enableDebug(flag) }
    override fun noStdLib(flag: Boolean) = forEach { it.noStdLib(flag) }
    override fun noMain(flag: Boolean) = forEach { it.noMain(flag) }
    override fun enableOptimizations(flag: Boolean) = forEach { it.enableOptimizations(flag) }
    override fun enableAssertions(flag: Boolean) = forEach { it.enableAssertions(flag) }

    override fun measureTime(flag: Boolean) = forEach { it.measureTime(flag) }
}

open class KonanProgram(name: String, project: ProjectInternal, instantiator: Instantiator)
    : KonanCompileConfig<KonanCompileProgramTask>(name, KonanCompileProgramTask::class.java, project, instantiator) {

    override val typeForDescription: String
        get() = "executable"

    override val defaultBaseDir: File
        get() = project.konanBinBaseDir
}

open class KonanLibrary(name: String, project: ProjectInternal, instantiator: Instantiator)
    : KonanCompileConfig<KonanCompileLibraryTask>(name, KonanCompileLibraryTask::class.java, project, instantiator) {

    override val typeForDescription: String
        get() = "library"

    override val defaultBaseDir: File
        get() = project.konanLibsBaseDir
}

open class KonanBitcode(name: String, project: ProjectInternal, instantiator: Instantiator)
    : KonanCompileConfig<KonanCompileBitcodeTask>(name, KonanCompileBitcodeTask::class.java, project, instantiator) {
    override val typeForDescription: String
        get() = "bitcode"

    override fun generateTaskDescription(task: KonanCompileBitcodeTask) =
            "Generates bitcode for the artifact '${task.name}' and target '${task.konanTarget}'"

    override fun generateAggregateTaskDescription(task: Task) =
            "Generates bitcode for the artifact '${task.name}' for all supported and declared targets'"

    override fun generateHostTaskDescription(task: Task, hostTarget: KonanTarget) =
            "Generates bitcode for the artifact '${task.name}' for current host"

    override val defaultBaseDir: File
        get() = project.konanBitcodeBaseDir
}