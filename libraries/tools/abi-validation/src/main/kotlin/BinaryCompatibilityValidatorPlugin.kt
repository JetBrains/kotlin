/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.klib.KlibTarget
import kotlinx.validation.api.klib.konanTargetNameMapping
import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.LibraryAbiReader
import java.io.*

@OptIn(ExperimentalBCVApi::class, ExperimentalLibraryAbiReader::class)
public class BinaryCompatibilityValidatorPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        val extension = extensions.create("apiValidation", ApiValidationExtension::class.java)
        validateExtension(extension)
        allprojects {
            configureProject(it, extension)
        }
    }

    private fun Project.validateExtension(extension: ApiValidationExtension) {
        afterEvaluate {
            val ignored = extension.ignoredProjects
            val all = allprojects.map { it.name }
            for (project in ignored) {
                require(project in all) { "Cannot find excluded project $project in all projects: $all" }
            }
            if (extension.klib.enabled) {
                try {
                    LibraryAbiReader.javaClass
                } catch (e: NoClassDefFoundError) {
                    throw IllegalStateException(
                        "KLib validation is not available. " +
                                "Make sure the project uses at least Kotlin 1.9.20 or disable KLib validation " +
                                "by setting apiValidation.klib.enabled to false", e
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalBCVApi::class)
    private fun configureProject(project: Project, extension: ApiValidationExtension) {
        configureKotlinPlugin(project, extension)
        configureAndroidPlugin(project, extension)
        configureMultiplatformPlugin(project, extension)
    }

    private fun configurePlugin(
        name: String,
        project: Project,
        extension: ApiValidationExtension,
        action: Action<AppliedPlugin>
    ) = project.pluginManager.withPlugin(name) {
        if (project.name in extension.ignoredProjects) return@withPlugin
        action.execute(it)
    }

    private fun configureMultiplatformPlugin(
        project: Project,
        extension: ApiValidationExtension
    ) = configurePlugin("kotlin-multiplatform", project, extension) {
        if (project.name in extension.ignoredProjects) return@configurePlugin
        val kotlin = project.kotlinMultiplatform

        // Create common tasks for multiplatform
        val commonApiDump = project.tasks.register("apiDump") {
            it.group = "other"
            it.description = "Task that collects all target specific dump tasks"
        }

        val commonApiCheck: TaskProvider<Task> = project.tasks.register("apiCheck") {
            it.group = "verification"
            it.description = "Shortcut task that depends on all specific check tasks"
        }.apply { project.tasks.named("check") { it.dependsOn(this) } }

        val jvmTargetCountProvider = project.provider {
            kotlin.targets.count {
                it.platformType in arrayOf(
                    KotlinPlatformType.jvm,
                    KotlinPlatformType.androidJvm
                )
            }
        }

        val jvmDirConfig = jvmTargetCountProvider.map {
            if (it == 1) DirConfig.COMMON else DirConfig.TARGET_DIR
        }
        val klibDirConfig = project.provider { DirConfig.COMMON }

        kotlin.targets.matching { it.jvmBased }.all { target ->
            val targetConfig = TargetConfig(project, extension, target.name, jvmDirConfig)
            if (target.platformType == KotlinPlatformType.jvm) {
                target.mainCompilationOrNull?.also {
                    project.configureKotlinCompilation(it, extension, targetConfig, commonApiDump, commonApiCheck)
                }
            } else if (target.platformType == KotlinPlatformType.androidJvm) {
                target.compilations.matching { it.name == "release" }.all {
                    project.configureKotlinCompilation(
                        it,
                        extension,
                        targetConfig,
                        commonApiDump,
                        commonApiCheck,
                        useOutput = true
                    )
                }
            }
        }
        KlibValidationPipelineBuilder(klibDirConfig, extension).configureTasks(project, commonApiDump, commonApiCheck)
    }

    private fun configureAndroidPlugin(
        project: Project,
        extension: ApiValidationExtension
    ) {
        configureAndroidPluginForKotlinLibrary(project, extension)

    }

    private fun configureAndroidPluginForKotlinLibrary(
        project: Project,
        extension: ApiValidationExtension
    ) = configurePlugin("kotlin-android", project, extension) {
        val androidExtension = project.extensions
            .getByName("kotlin") as KotlinAndroidProjectExtension
        androidExtension.target.compilations.matching {
            it.compilationName == "release"
        }.all {
            project.configureKotlinCompilation(it, extension, useOutput = true)
        }
    }

    private fun configureKotlinPlugin(
        project: Project,
        extension: ApiValidationExtension
    ) = configurePlugin("kotlin", project, extension) {
        project.configureApiTasks(extension, TargetConfig(project, extension))
    }
}

private class TargetConfig constructor(
    project: Project,
    extension: ApiValidationExtension,
    val targetName: String? = null,
    dirConfig: Provider<DirConfig>? = null,
) {
    private val apiDirProvider = project.provider {
        val dir = extension.apiDumpDirectory

        val root = project.layout.projectDirectory.asFile.toPath().toAbsolutePath().normalize()
        val resolvedDir = root.resolve(dir).normalize()
        if (!resolvedDir.startsWith(root)) {
            throw IllegalArgumentException(
                "apiDumpDirectory (\"$dir\") should be inside the project directory, " +
                        "but it resolves to a path outside the project root.\n" +
                        "Project's root path: $root\nResolved apiDumpDirectory: $resolvedDir"
            )
        }

        dir
    }

    val apiDir = dirConfig?.map { dirConfig ->
        when (dirConfig) {
            DirConfig.COMMON -> apiDirProvider.get()
            else -> "${apiDirProvider.get()}/$targetName"
        }
    } ?: apiDirProvider

    fun apiTaskName(suffix: String) = when (targetName) {
        null, "" -> "api$suffix"
        else -> "${targetName}Api$suffix"
    }
}

private enum class DirConfig {
    /**
     * `api` directory for .api files.
     * Used in single target projects
     */
    COMMON,

    /**
     * Target-based directory, used in multitarget setups.
     * E.g. for the project with targets jvm and android,
     * the resulting paths will be
     * `/api/jvm/project.api` and `/api/android/project.api`
     */
    TARGET_DIR
}

private fun Project.configureKotlinCompilation(
    compilation: KotlinCompilation<KotlinCommonOptions>,
    extension: ApiValidationExtension,
    targetConfig: TargetConfig = TargetConfig(this, extension),
    commonApiDump: TaskProvider<Task>? = null,
    commonApiCheck: TaskProvider<Task>? = null,
    useOutput: Boolean = false,
) {
    val projectName = project.name
    val dumpFileName = project.jvmDumpFileName
    val apiDirProvider = targetConfig.apiDir
    val apiBuildDir = apiDirProvider.map { layout.buildDirectory.asFile.get().resolve(it) }

    val apiBuild = task<KotlinApiBuildTask>(targetConfig.apiTaskName("Build")) {
        // Do not enable task for empty umbrella modules
        isEnabled = apiCheckEnabled(projectName, extension)
        val hasSourcesPredicate = compilation.hasAnySourcesPredicate()
        onlyIf { hasSourcesPredicate.get() }
        // 'group' is not specified deliberately, so it will be hidden from ./gradlew tasks
        description =
            "Builds Kotlin API for 'main' compilations of $projectName. Complementary task and shouldn't be called manually"
        if (useOutput) {
            // Workaround for #4
            inputClassesDirs =
                files(provider<Any> { if (isEnabled) compilation.output.classesDirs else emptyList<Any>() })
            inputDependencies =
                files(provider<Any> { if (isEnabled) compilation.output.classesDirs else emptyList<Any>() })
        } else {
            inputClassesDirs =
                files(provider<Any> { if (isEnabled) compilation.output.classesDirs else emptyList<Any>() })
            inputDependencies =
                files(provider<Any> { if (isEnabled) compilation.compileDependencyFiles else emptyList<Any>() })
        }
        outputApiFile = apiBuildDir.get().resolve(dumpFileName)
    }
    configureCheckTasks(apiBuildDir, apiBuild, extension, targetConfig, commonApiDump, commonApiCheck)
}

internal val Project.sourceSets: SourceSetContainer
    get() = extensions.getByName("sourceSets") as SourceSetContainer

internal val Project.apiValidationExtensionOrNull: ApiValidationExtension?
    get() =
        generateSequence(this) { it.parent }
            .map { it.extensions.findByType(ApiValidationExtension::class.java) }
            .firstOrNull { it != null }

private fun apiCheckEnabled(projectName: String, extension: ApiValidationExtension): Boolean =
    projectName !in extension.ignoredProjects && !extension.validationDisabled

@OptIn(ExperimentalBCVApi::class)
private fun klibAbiCheckEnabled(projectName: String, extension: ApiValidationExtension): Boolean =
    projectName !in extension.ignoredProjects && !extension.validationDisabled && extension.klib.enabled

private fun Project.configureApiTasks(
    extension: ApiValidationExtension,
    targetConfig: TargetConfig = TargetConfig(this, extension),
) {
    val projectName = project.name
    val dumpFileName = project.jvmDumpFileName
    val apiBuildDir = targetConfig.apiDir.map { layout.buildDirectory.asFile.get().resolve(it) }
    val sourceSetsOutputsProvider = project.provider {
        sourceSets
            .filter { it.name == SourceSet.MAIN_SOURCE_SET_NAME || it.name in extension.additionalSourceSets }
            .map { it.output.classesDirs }
    }

    val apiBuild = task<KotlinApiBuildTask>(targetConfig.apiTaskName("Build")) {
        isEnabled = apiCheckEnabled(projectName, extension)
        // 'group' is not specified deliberately, so it will be hidden from ./gradlew tasks
        description =
            "Builds Kotlin API for 'main' compilations of $projectName. Complementary task and shouldn't be called manually"
        inputClassesDirs = files(provider<Any> { if (isEnabled) sourceSetsOutputsProvider.get() else emptyList<Any>() })
        inputDependencies =
            files(provider<Any> { if (isEnabled) sourceSetsOutputsProvider.get() else emptyList<Any>() })
        outputApiFile = apiBuildDir.get().resolve(dumpFileName)
    }

    configureCheckTasks(apiBuildDir, apiBuild, extension, targetConfig)
}

private fun Project.configureCheckTasks(
    apiBuildDir: Provider<File>,
    apiBuild: TaskProvider<*>,
    extension: ApiValidationExtension,
    targetConfig: TargetConfig,
    commonApiDump: TaskProvider<Task>? = null,
    commonApiCheck: TaskProvider<Task>? = null,
) {
    val projectName = project.name
    val apiCheckDir = targetConfig.apiDir.map {
        projectDir.resolve(it).also { r ->
            logger.debug("Configuring api for ${targetConfig.targetName ?: "jvm"} to $r")
        }
    }
    val apiCheck = task<KotlinApiCompareTask>(targetConfig.apiTaskName("Check")) {
        isEnabled = apiCheckEnabled(projectName, extension) && apiBuild.map { it.enabled }.getOrElse(true)
        group = "verification"
        description = "Checks signatures of public API against the golden value in API folder for $projectName"
        projectApiFile = apiCheckDir.get().resolve(jvmDumpFileName)
        generatedApiFile = apiBuildDir.get().resolve(jvmDumpFileName)
        dependsOn(apiBuild)
    }

    val dumpFileName = project.jvmDumpFileName
    val apiDump = task<CopyFile>(targetConfig.apiTaskName("Dump")) {
        isEnabled = apiCheckEnabled(projectName, extension) && apiBuild.map { it.enabled }.getOrElse(true)
        group = "other"
        description = "Syncs API from build dir to ${targetConfig.apiDir} dir for $projectName"
        from = apiBuildDir.get().resolve(dumpFileName)
        to = apiCheckDir.get().resolve(dumpFileName)
        dependsOn(apiBuild)
    }

    commonApiDump?.configure { it.dependsOn(apiDump) }

    when (commonApiCheck) {
        null -> project.tasks.named("check").configure { it.dependsOn(apiCheck) }
        else -> commonApiCheck.configure { it.dependsOn(apiCheck) }
    }
}

private inline fun <reified T : Task> Project.task(
    name: String,
    noinline configuration: T.() -> Unit,
): TaskProvider<T> = tasks.register(name, T::class.java, Action(configuration))

private const val BANNED_TARGETS_PROPERTY_NAME = "binary.compatibility.validator.klib.targets.disabled.for.testing"
private const val KLIB_DUMPS_DIRECTORY = "klib"
private const val KLIB_INFERRED_DUMPS_DIRECTORY = "klib-all"

/**
 * KLib ABI dump validation and dump extraction consists of multiple steps that extracts and transforms dumps for klibs.
 * The main entry point for validation is the `klibApiCheck` task, which is a dependency for `apiCheck` task, and the
 * main entry point for dump extraction is the `klibApiDump` task, which is a dependency for `apiDump` task.
 *
 * Both `klibApiCheck` and `klibApiDump` depends on multiple other tasks that extracts dumps for compiled klibs,
 * generate (in case of dumping) dumps for targets that are not supported by the host compiler and don't have compiled
 * klibs, and, finally, merges individual dumps into a single merged KLib ABI dump file that is then either stored
 * inside a project's api dir (in case of dumping), or compared against a golden value (in case of validation).
 *
 * Here's how different tasks depend on each other:
 * - `klibApiCheck` ([KotlinApiCompareTask]) depends on `klibApiMerge` and `klibApiExtractForValidation` tasks;
 * this task itself does not perform anything except comparing the result of a merge, with a preprocessed golden value;
 * - `klibApiDump` ([CopyFile]) depends on `klibApiMergeInferred` and simply moves the merged ABI dump into a configured
 * api directory within a project;
 * - `klibApiMerge` and `klibApiMergeInferred` are both [KotlinKlibMergeAbiTask] instances merging multiple individual
 * KLib ABI dumps into a single merged dump file; these tasks differs only by their dependencies and input dump files
 * to merge: `klibApiMerge` uses only dump files extracted from compiled klibs, these dumps are extracted using
 * multiple `<targetName>ApiBuild` tasks ([KotlinKlibAbiBuildTask]); `klibApiMergeInferred` depends on the same tasks
 * as `klibApiMerge`, but also have additional dependencies responsible for inferring KLib ABI dumps for targets not
 * supported by the host compiler (`<targetName>ApiInfer` tasks
 * instantiating [KotlinKlibInferAbiForUnsupportedTargetTask]);
 * - `klibApiExtractForValidation` ([KotlinKlibExtractSupportedTargetsAbiTask]) is responsible for filtering out all
 * currently unsupported targets from the golden image, so that it could be compared with a merged dump;
 * - each `<targetName>ApiInfer` task depends on all regular `<targetName>ApiBuild` tasks; it searches for targets
 * that are suitable to ABI dump inference, merges them and then mixes in all declarations specific to the unsupported
 * target that were extracted from the golden image.
 */
@ExperimentalBCVApi
private class KlibValidationPipelineBuilder(
    val dirConfig: Provider<DirConfig>?,
    val extension: ApiValidationExtension
) {
    lateinit var intermediateFilesConfig: Provider<DirConfig>

    fun configureTasks(project: Project, commonApiDump: TaskProvider<Task>, commonApiCheck: TaskProvider<Task>) {
        // In the intermediate phase of KLib dump generation, there are always multiple targets; thus we need
        // a target-based directory tree.
        intermediateFilesConfig = project.provider { DirConfig.TARGET_DIR }
        val klibApiDirConfig = dirConfig?.map { TargetConfig(project, extension, KLIB_DUMPS_DIRECTORY, dirConfig) }
        val klibDumpConfig = TargetConfig(project, extension, KLIB_DUMPS_DIRECTORY, intermediateFilesConfig)
        val klibInferDumpConfig =
            TargetConfig(project, extension, KLIB_INFERRED_DUMPS_DIRECTORY, intermediateFilesConfig)

        val projectDir = project.projectDir
        val klibApiDir = klibApiDirConfig?.map {
            projectDir.resolve(it.apiDir.get())
        }!!
        val projectBuildDir = project.layout.buildDirectory.asFile.get()
        val klibMergeDir = projectBuildDir.resolve(klibDumpConfig.apiDir.get())
        val klibMergeInferredDir = projectBuildDir.resolve(klibInferDumpConfig.apiDir.get())
        val klibExtractedFileDir = klibMergeInferredDir.resolve("extracted")

        val klibMerge = project.mergeKlibsUmbrellaTask(klibDumpConfig, klibMergeDir)
        val klibMergeInferred = project.mergeInferredKlibsUmbrellaTask(klibDumpConfig, klibMergeInferredDir)
        val klibDump = project.dumpKlibsTask(klibDumpConfig, klibApiDir, klibMergeInferredDir)
        val klibExtractAbiForSupportedTargets = project.extractAbi(klibDumpConfig, klibApiDir, klibExtractedFileDir)
        val klibCheck = project.checkKlibsTask(klibDumpConfig, project.provider { klibExtractedFileDir }, klibMergeDir)

        commonApiDump.configure { it.dependsOn(klibDump) }
        commonApiCheck.configure { it.dependsOn(klibCheck) }

        klibDump.configure { it.dependsOn(klibMergeInferred) }
        klibCheck.configure {
            it.dependsOn(klibExtractAbiForSupportedTargets)
            it.dependsOn(klibMerge)
        }

        project.configureTargets(klibApiDir, klibMerge, klibMergeInferred)
    }

    private fun Project.checkKlibsTask(
        klibDumpConfig: TargetConfig,
        klibApiDir: Provider<File>,
        klibMergeDir: File
    ) = project.task<KotlinApiCompareTask>(klibDumpConfig.apiTaskName("Check")) {
        isEnabled = klibAbiCheckEnabled(project.name, extension)
        group = "verification"
        description = "Checks signatures of a public KLib ABI against the golden value in ABI folder for " +
                project.name
        projectApiFile = klibApiDir.get().resolve(klibDumpFileName)
        generatedApiFile = klibMergeDir.resolve(klibDumpFileName)
        val hasCompilableTargets = project.hasCompilableTargetsPredicate()
        onlyIf("There are no klibs compiled for the project") { hasCompilableTargets.get() }
    }

    private fun Project.dumpKlibsTask(
        klibDumpConfig: TargetConfig,
        klibApiDir: Provider<File>,
        klibMergeDir: File
    ) = project.task<CopyFile>(klibDumpConfig.apiTaskName("Dump")) {
        isEnabled = klibAbiCheckEnabled(project.name, extension)
        description = "Syncs a KLib ABI dump from a build dir to the ${klibDumpConfig.apiDir} dir for ${project.name}"
        group = "other"
        from = klibMergeDir.resolve(klibDumpFileName)
        to = klibApiDir.get().resolve(klibDumpFileName)
        val hasCompilableTargets = project.hasCompilableTargetsPredicate()
        onlyIf("There are no klibs compiled for the project") { hasCompilableTargets.get() }
    }

    private fun Project.extractAbi(
        klibDumpConfig: TargetConfig,
        klibApiDir: Provider<File>,
        klibOutputDir: File
    ) = project.task<KotlinKlibExtractSupportedTargetsAbiTask>(
        klibDumpConfig.apiTaskName("ExtractForValidation")
    )
    {
        isEnabled = klibAbiCheckEnabled(project.name, extension)
        description = "Prepare a reference KLib ABI file by removing all unsupported targets from " +
                "the golden file stored in the project"
        group = "other"
        strictValidation = extension.klib.strictValidation
        supportedTargets = supportedTargets()
        inputAbiFile = klibApiDir.get().resolve(klibDumpFileName)
        outputAbiFile = klibOutputDir.resolve(klibDumpFileName)
        val hasCompilableTargets = project.hasCompilableTargetsPredicate()
        onlyIf("There are no klibs compiled for the project") { hasCompilableTargets.get() }
    }

    private fun Project.mergeInferredKlibsUmbrellaTask(
        klibDumpConfig: TargetConfig,
        klibMergeDir: File,
    ) = project.task<KotlinKlibMergeAbiTask>(
        klibDumpConfig.apiTaskName("MergeInferred")
    )
    {
        isEnabled = klibAbiCheckEnabled(project.name, extension)
        description = "Merges multiple KLib ABI dump files generated for " +
                "different targets (including inferred dumps for unsupported targets) " +
                "into a single merged KLib ABI dump"
        dumpFileName = klibDumpFileName
        mergedFile = klibMergeDir.resolve(klibDumpFileName)
        val hasCompilableTargets = project.hasCompilableTargetsPredicate()
        onlyIf("There are no dumps to merge") { hasCompilableTargets.get() }
    }

    private fun Project.mergeKlibsUmbrellaTask(
        klibDumpConfig: TargetConfig,
        klibMergeDir: File
    ) = project.task<KotlinKlibMergeAbiTask>(klibDumpConfig.apiTaskName("Merge")) {
        isEnabled = klibAbiCheckEnabled(project.name, extension)
        description = "Merges multiple KLib ABI dump files generated for " +
                "different targets into a single merged KLib ABI dump"
        dumpFileName = klibDumpFileName
        mergedFile = klibMergeDir.resolve(klibDumpFileName)
        val hasCompilableTargets = project.hasCompilableTargetsPredicate()
        onlyIf("There are no dumps to merge") { hasCompilableTargets.get() }
    }

    fun Project.bannedTargets(): Set<String> {
        val prop = project.properties[BANNED_TARGETS_PROPERTY_NAME] as String?
        prop ?: return emptySet()
        return prop.split(",").map { it.trim() }.toSet().also {
            if (it.isNotEmpty()) {
                project.logger.warn(
                    "WARNING: Following property is not empty: $BANNED_TARGETS_PROPERTY_NAME. " +
                            "If you're don't know what it means, please make sure that its value is empty."
                )
            }
        }
    }

    fun Project.configureTargets(
        klibApiDir: Provider<File>,
        mergeTask: TaskProvider<KotlinKlibMergeAbiTask>,
        mergeInferredTask: TaskProvider<KotlinKlibMergeAbiTask>
    ) {
        val kotlin = project.kotlinMultiplatform

        val supportedTargetsProvider = supportedTargets()
        kotlin.targets.matching { it.emitsKlib }.configureEach { currentTarget ->
            val mainCompilation = currentTarget.mainCompilationOrNull ?: return@configureEach

            val targetName = currentTarget.targetName
            val targetConfig = TargetConfig(project, extension, targetName, intermediateFilesConfig)
            val apiBuildDir = targetConfig.apiDir.map { project.layout.buildDirectory.asFile.get().resolve(it) }.get()
            val targetSupported = targetIsSupported(currentTarget)
            // If a target is supported, the workflow is simple: create a dump, then merge it along with other dumps.
            if (targetSupported) {
                val buildTargetAbi = configureKlibCompilation(mainCompilation, extension, targetConfig, apiBuildDir)
                mergeTask.configure {
                    it.addInput(targetName, apiBuildDir)
                    it.dependsOn(buildTargetAbi)
                }
                mergeInferredTask.configure {
                    it.addInput(targetName, apiBuildDir)
                    it.dependsOn(buildTargetAbi)
                }
                return@configureEach
            }
            // If the target is unsupported, the regular merge task will only depend on a task complaining about
            // the target being unsupported.
            val unsupportedTargetStub = mergeDependencyForUnsupportedTarget(targetConfig)
            mergeTask.configure {
                it.dependsOn(unsupportedTargetStub)
            }
            // The actual merge will happen here, where we'll try to infer a dump for the unsupported target and merge
            // it with other supported target dumps.
            val proxy = unsupportedTargetDumpProxy(
                mainCompilation,
                klibApiDir, targetConfig,
                extractUnderlyingTarget(currentTarget),
                apiBuildDir, supportedTargetsProvider
            )
            mergeInferredTask.configure {
                it.addInput(targetName, apiBuildDir)
                it.dependsOn(proxy)
            }
        }
        mergeTask.configure {
            it.doFirst {
                if (supportedTargetsProvider.get().isEmpty()) {
                    throw IllegalStateException(
                        "KLib ABI dump/validation requires at least one enabled klib target, but none were found."
                    )
                }
            }
        }
    }

    private fun Project.targetIsSupported(target: KotlinTarget): Boolean {
        if (bannedTargets().contains(target.targetName)) return false
        return when (target) {
            is KotlinNativeTarget -> HostManager().isEnabled(target.konanTarget)
            else -> true
        }
    }

    // Compilable targets supported by the host compiler
    private fun Project.supportedTargets(): Provider<Set<String>> {
        val banned = bannedTargets() // for testing only
        return project.provider {
            val hm = HostManager()
            project.kotlinMultiplatform.targets.matching { it.emitsKlib }
                .asSequence()
                .filter { it.mainCompilationOrNull?.hasAnySources() == true }
                .filter {
                    if (it is KotlinNativeTarget) {
                        hm.isEnabled(it.konanTarget) && it.targetName !in banned
                    } else {
                        true
                    }
                }
                .map { KlibTarget(extractUnderlyingTarget(it), it.targetName).toString() }
                .toSet()
        }
    }

    // Returns a predicate that checks if there are any compilable targets
    private fun Project.hasCompilableTargetsPredicate(): Provider<Boolean> {
        return project.provider {
            project.kotlinMultiplatform.targets.matching { it.emitsKlib }
                .asSequence()
                .any { it.mainCompilationOrNull?.hasAnySources() == true }
        }
    }

    private fun Project.configureKlibCompilation(
        compilation: KotlinCompilation<KotlinCommonOptions>,
        extension: ApiValidationExtension,
        targetConfig: TargetConfig,
        apiBuildDir: File
    ): TaskProvider<KotlinKlibAbiBuildTask> {
        val projectName = project.name
        val buildTask = project.task<KotlinKlibAbiBuildTask>(targetConfig.apiTaskName("Build")) {
            target = targetConfig.targetName!!
            // Do not enable task for empty umbrella modules
            isEnabled = klibAbiCheckEnabled(projectName, extension)
            val hasSourcesPredicate = compilation.hasAnySourcesPredicate()
            onlyIf { hasSourcesPredicate.get() }
            // 'group' is not specified deliberately, so it will be hidden from ./gradlew tasks
            description = "Builds Kotlin KLib ABI dump for 'main' compilations of $projectName. " +
                    "Complementary task and shouldn't be called manually"
            klibFile = project.files(project.provider { compilation.output.classesDirs })
            compilationDependencies = project.files(project.provider { compilation.compileDependencyFiles })
            signatureVersion = SerializableSignatureVersion(extension.klib.signatureVersion)
            outputApiFile = apiBuildDir.resolve(klibDumpFileName)
        }
        return buildTask
    }

    private fun Project.mergeDependencyForUnsupportedTarget(targetConfig: TargetConfig): TaskProvider<DefaultTask> {
        return project.task<DefaultTask>(targetConfig.apiTaskName("Build")) {
            isEnabled = apiCheckEnabled(project.name, extension)

            doLast {
                logger.warn(
                    "Target ${targetConfig.targetName} is not supported by the host compiler and a " +
                            "KLib ABI dump could not be directly generated for it."
                )
            }
        }
    }

    private fun Project.unsupportedTargetDumpProxy(
        compilation: KotlinCompilation<KotlinCommonOptions>,
        klibApiDir: Provider<File>,
        targetConfig: TargetConfig,
        underlyingTarget: String,
        apiBuildDir: File,
        supportedTargets: Provider<Set<String>>
    ): TaskProvider<KotlinKlibInferAbiForUnsupportedTargetTask> {
        val targetName = targetConfig.targetName!!
        return project.task<KotlinKlibInferAbiForUnsupportedTargetTask>(targetConfig.apiTaskName("Infer")) {
            isEnabled = klibAbiCheckEnabled(project.name, extension)
            val hasSourcesPredicate = compilation.hasAnySourcesPredicate()
            onlyIf { hasSourcesPredicate.get() }
            description = "Try to infer the dump for unsupported target $targetName using dumps " +
                    "generated for supported targets."
            group = "other"
            this.supportedTargets = supportedTargets
            inputImageFile = klibApiDir.get().resolve(klibDumpFileName)
            outputApiDir = apiBuildDir.toString()
            outputFile = apiBuildDir.resolve(klibDumpFileName)
            unsupportedTargetName = targetConfig.targetName
            unsupportedTargetCanonicalName = underlyingTarget
            dumpFileName = klibDumpFileName
            dependsOn(project.tasks.withType(KotlinKlibAbiBuildTask::class.java))
        }
    }
}

private val KotlinTarget.emitsKlib: Boolean
    get() {
        val platformType = this.platformType
        return platformType == KotlinPlatformType.native ||
                platformType == KotlinPlatformType.wasm ||
                platformType == KotlinPlatformType.js
    }

private val KotlinTarget.jvmBased: Boolean
    get() {
        val platformType = this.platformType
        return platformType == KotlinPlatformType.jvm || platformType == KotlinPlatformType.androidJvm
    }

private fun extractUnderlyingTarget(target: KotlinTarget): String {
    if (target is KotlinNativeTarget) {
        return konanTargetNameMapping[target.konanTarget.name]!!
    }
    return when (target.platformType) {
        KotlinPlatformType.js -> "js"
        KotlinPlatformType.wasm -> when ((target as KotlinJsIrTarget).wasmTargetType) {
            KotlinWasmTargetType.WASI -> "wasmWasi"
            KotlinWasmTargetType.JS -> "wasmJs"
            else -> throw IllegalStateException("Unreachable")
        }
        else -> throw IllegalArgumentException("Unsupported platform type: ${target.platformType}")
    }
}

private val Project.kotlinMultiplatform
    get() = extensions.getByName("kotlin") as KotlinMultiplatformExtension

private val KotlinTarget.mainCompilationOrNull: KotlinCompilation<KotlinCommonOptions>?
    get() = compilations.firstOrNull { it.name == KotlinCompilation.MAIN_COMPILATION_NAME }

private val Project.jvmDumpFileName: String
    get() = "$name.api"
private val Project.klibDumpFileName: String
    get() = "$name.klib.api"

private fun KotlinCompilation<KotlinCommonOptions>.hasAnySources(): Boolean = allKotlinSourceSets.any {
    it.kotlin.srcDirs.any(File::exists)
}

private fun KotlinCompilation<KotlinCommonOptions>.hasAnySourcesPredicate(): Provider<Boolean> = project.provider {
    this.hasAnySources()
}
