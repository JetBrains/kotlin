package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.build.gradle.internal.variant.BaseVariantOutputData
import com.android.builder.model.SourceProvider
import groovy.lang.Closure
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.internal.AnnotationProcessingManager
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.internal.Kapt3KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.internal.initKapt
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.incremental.configureMultiProjectIncrementalCompilation
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifferenceRegistryProviderAndroidWrapper
import java.io.File
import java.net.URL
import java.util.*
import java.util.jar.Manifest

val KOTLIN_AFTER_JAVA_TASK_SUFFIX = "AfterJava"
val KOTLIN_DSL_NAME = "kotlin"
val KOTLIN_JS_DSL_NAME = "kotlin2js"
val KOTLIN_OPTIONS_DSL_NAME = "kotlinOptions"

internal abstract class KotlinSourceSetProcessor<T : AbstractKotlinCompile<*>>(
        val project: Project,
        val javaBasePlugin: JavaBasePlugin,
        val sourceSet: SourceSet,
        val tasksProvider: KotlinTasksProvider,
        val kotlinSourceSetProvider: KotlinSourceSetProvider,
        val dslExtensionName: String,
        val compileTaskNameSuffix: String,
        val taskDescription: String
) {
    abstract protected fun doTargetSpecificProcessing()
    protected val logger = Logging.getLogger(this.javaClass)!!

    protected val sourceSetName: String = sourceSet.name
    protected val sourceRootDir: String = "src/$sourceSetName/kotlin"
    protected val kotlinSourceSet: KotlinSourceSet = createKotlinSourceSet()
    protected val kotlinTask: T = createKotlinCompileTask()
    protected open val defaultKotlinDestinationDir: File
            get() = sourceSet.output.classesDir

    fun run() {
        addKotlinDirSetToSources()
        doTargetSpecificProcessing()
    }

    private fun createKotlinSourceSet(): KotlinSourceSet {
        logger.kotlinDebug("Creating KotlinSourceSet for $sourceSet")
        val kotlinSourceSet = kotlinSourceSetProvider.create(sourceSet.name)
        kotlinSourceSet.kotlin.srcDir(project.file(sourceRootDir))
        sourceSet.addConvention(dslExtensionName, kotlinSourceSet)
        return kotlinSourceSet
    }

    private fun addKotlinDirSetToSources() {
        val kotlinDirSet = kotlinSourceSet.kotlin
        sourceSet.allJava.source(kotlinDirSet)
        sourceSet.allSource.source(kotlinDirSet)
        sourceSet.resources.filter.exclude { it.file in kotlinDirSet }
    }

    private fun createKotlinCompileTask(): T {
        val name = sourceSet.getCompileTaskName(compileTaskNameSuffix)
        logger.kotlinDebug("Creating kotlin compile task $name")
        val kotlinCompile = doCreateTask(project, name)
        kotlinCompile.description = taskDescription
        kotlinCompile.mapClasspath { sourceSet.compileClasspath }
        kotlinCompile.destinationDir = defaultKotlinDestinationDir
        return kotlinCompile
    }

    protected abstract fun doCreateTask(project: Project, taskName: String): T
}

internal class Kotlin2JvmSourceSetProcessor(
        project: Project,
        javaBasePlugin: JavaBasePlugin,
        sourceSet: SourceSet,
        tasksProvider: KotlinTasksProvider,
        kotlinSourceSetProvider: KotlinSourceSetProvider,
        private val kotlinPluginVersion: String,
        private val kotlinGradleBuildServices: KotlinGradleBuildServices
) : KotlinSourceSetProcessor<KotlinCompile>(
        project, javaBasePlugin, sourceSet, tasksProvider, kotlinSourceSetProvider,
        dslExtensionName = KOTLIN_DSL_NAME,
        compileTaskNameSuffix = "kotlin",
        taskDescription = "Compiles the $sourceSet.kotlin."
) {
    override val defaultKotlinDestinationDir: File
        get() = File(project.buildDir, "kotlin-classes/$sourceSetName")

    override fun doCreateTask(project: Project, taskName: String): KotlinCompile =
            tasksProvider.createKotlinJVMTask(project, taskName, sourceSet.name)

    override fun doTargetSpecificProcessing() {
        val aptConfiguration = project.createAptConfiguration(sourceSet.name, kotlinPluginVersion)

        project.afterEvaluate { project ->
            if (project != null) {
                val javaTask = project.tasks.findByName(sourceSet.compileJavaTaskName)

                val subpluginEnvironment = loadSubplugins(project)
                val appliedPlugins = subpluginEnvironment.addSubpluginOptions(
                        project, kotlinTask, javaTask as JavaCompile, null, sourceSet)

                var kotlinAfterJavaTask: KotlinCompile? = null

                if (aptConfiguration.dependencies.size > 1 && !Kapt3GradleSubplugin.isEnabled(project)) {
                    javaTask.dependsOn(aptConfiguration.buildDependencies)

                    val (aptOutputDir, aptWorkingDir) = project.getAptDirsForSourceSet(sourceSetName)

                    val kaptManager = AnnotationProcessingManager(kotlinTask, javaTask, sourceSetName,
                            aptConfiguration.resolve(), aptOutputDir, aptWorkingDir)

                    kotlinAfterJavaTask = project.initKapt(kotlinTask, javaTask, kaptManager, sourceSetName, null, subpluginEnvironment, tasksProvider)
                }

                sourceSet.java.srcDirs.forEach { kotlinSourceSet.kotlin.srcDir(it) }

                // KotlinCompile.source(kotlinDirSet) should be called only after all java roots are added to kotlinDirSet
                // otherwise some java roots can be ignored
                kotlinTask.source(kotlinSourceSet.kotlin)
                kotlinAfterJavaTask?.source(kotlinSourceSet.kotlin)
                appliedPlugins
                        .flatMap { it.getSubpluginKotlinTasks(project, kotlinTask) }
                        .forEach { it.source(kotlinSourceSet.kotlin) }

                configureJavaTask(kotlinTask, javaTask, logger)
                createSyncOutputTask(project, kotlinTask, javaTask, kotlinAfterJavaTask, sourceSetName)
                val artifactFile = project.tryGetSingleArtifact()
                configureMultiProjectIncrementalCompilation(project, kotlinTask, javaTask, kotlinAfterJavaTask,
                        kotlinGradleBuildServices.artifactDifferenceRegistryProvider,
                        artifactFile)
            }
        }
    }

    private fun Project.tryGetSingleArtifact(): File? {
        val log = logger
        log.kotlinDebug { "Trying to determine single artifact for project $path" }

        val archives = configurations.findByName("archives")
        if (archives == null) {
            log.kotlinDebug { "Could not find 'archives' configuration for project $path" }
            return null
        }

        val artifacts = archives.artifacts.files.files
        log.kotlinDebug { "All artifacts for project $path: [${artifacts.joinToString()}]" }

        return if (artifacts.size == 1) artifacts.first() else null
    }
}

internal class Kotlin2JsSourceSetProcessor(
        project: Project,
        javaBasePlugin: JavaBasePlugin,
        sourceSet: SourceSet,
        tasksProvider: KotlinTasksProvider,
        kotlinSourceSetProvider: KotlinSourceSetProvider
) : KotlinSourceSetProcessor<Kotlin2JsCompile>(
        project, javaBasePlugin, sourceSet, tasksProvider, kotlinSourceSetProvider,
        dslExtensionName = KOTLIN_JS_DSL_NAME,
        taskDescription = "Compiles the kotlin sources in $sourceSet to JavaScript.",
        compileTaskNameSuffix = "kotlin2Js"
) {
    private val clean = project.tasks.findByName("clean")
    private val build = project.tasks.findByName("build")

    override fun doCreateTask(project: Project, taskName: String): Kotlin2JsCompile =
            tasksProvider.createKotlinJSTask(project, taskName, sourceSet.name)

    override fun doTargetSpecificProcessing() {
        val taskName = kotlinTask.name
        build?.dependsOn(taskName)
        clean?.dependsOn("clean" + taskName.capitalize())
        kotlinTask.source(kotlinSourceSet.kotlin)
        createCleanSourceMapTask()
    }

    private fun createCleanSourceMapTask() {
        val taskName = sourceSet.getTaskName("clean", "sourceMap")
        val task = project.tasks.create(taskName, Delete::class.java)
        task.onlyIf { kotlinTask.kotlinOptions.sourceMap }
        task.delete(object : Closure<String>(this) {
            override fun call(): String? = (kotlinTask.property("outputFile") as String) + ".map"
        })
        clean?.dependsOn(taskName)
    }
}


internal abstract class AbstractKotlinPlugin(
        val tasksProvider: KotlinTasksProvider,
        val kotlinSourceSetProvider: KotlinSourceSetProvider,
        protected val kotlinPluginVersion: String
) : Plugin<Project> {
    internal abstract fun buildSourceSetProcessor(project: Project, javaBasePlugin: JavaBasePlugin, sourceSet: SourceSet, kotlinPluginVersion: String): KotlinSourceSetProcessor<*>

    override fun apply(project: Project) {
        val javaBasePlugin = project.plugins.apply(JavaBasePlugin::class.java)
        val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)

        project.plugins.apply(JavaPlugin::class.java)

        configureSourceSetDefaults(project, javaBasePlugin, javaPluginConvention)
    }

    open protected fun configureSourceSetDefaults(
            project: Project,
            javaBasePlugin: JavaBasePlugin,
            javaPluginConvention: JavaPluginConvention
    ) {
        javaPluginConvention.sourceSets?.all { sourceSet ->
            buildSourceSetProcessor(project, javaBasePlugin, sourceSet, kotlinPluginVersion).run()
        }
    }
}

internal open class KotlinPlugin(
        tasksProvider: KotlinTasksProvider,
        kotlinSourceSetProvider: KotlinSourceSetProvider,
        kotlinPluginVersion: String,
        private val kotlinGradleBuildServices: KotlinGradleBuildServices
) : AbstractKotlinPlugin(tasksProvider, kotlinSourceSetProvider, kotlinPluginVersion) {
    override fun buildSourceSetProcessor(project: Project, javaBasePlugin: JavaBasePlugin, sourceSet: SourceSet, kotlinPluginVersion: String) =
            Kotlin2JvmSourceSetProcessor(project, javaBasePlugin, sourceSet, tasksProvider, kotlinSourceSetProvider, kotlinPluginVersion, kotlinGradleBuildServices)

    override fun apply(project: Project) {
        project.createKaptExtension()
        super.apply(project)
    }
}


internal open class Kotlin2JsPlugin(
        tasksProvider: KotlinTasksProvider,
        kotlinSourceSetProvider: KotlinSourceSetProvider,
        kotlinPluginVersion: String
) : AbstractKotlinPlugin(tasksProvider, kotlinSourceSetProvider, kotlinPluginVersion) {
    override fun buildSourceSetProcessor(project: Project, javaBasePlugin: JavaBasePlugin, sourceSet: SourceSet, kotlinPluginVersion: String) =
            Kotlin2JsSourceSetProcessor(project, javaBasePlugin, sourceSet, tasksProvider, kotlinSourceSetProvider)
}

internal open class KotlinAndroidPlugin(
        val tasksProvider: KotlinTasksProvider,
        private val kotlinSourceSetProvider: KotlinSourceSetProvider,
        private val kotlinPluginVersion: String,
        private val kotlinGradleBuildServices: KotlinGradleBuildServices
) : Plugin<Project> {

    private val log = Logging.getLogger(this.javaClass)

    override fun apply(project: Project) {
        val ext = project.extensions.getByName("android") as BaseExtension

        val version = loadAndroidPluginVersion()
        if (version != null) {
            val minimalVersion = "1.1.0"
            if (compareVersionNumbers(version, minimalVersion) < 0) {
                throw IllegalStateException("Kotlin: Unsupported version of com.android.tools.build:gradle plugin: version $minimalVersion or higher should be used with kotlin-android plugin")
            }
        }

        val aptConfigurations = hashMapOf<String, Configuration>()

        ext.sourceSets.all { sourceSet ->
            log.kotlinDebug("Creating KotlinSourceSet for source set $sourceSet")
            val kotlinSourceSet = kotlinSourceSetProvider.create(sourceSet.name)
            kotlinSourceSet.kotlin.srcDir(project.file(project.file("src/${sourceSet.name}/kotlin")))
            sourceSet.addConvention(KOTLIN_DSL_NAME, kotlinSourceSet)

            aptConfigurations.put(sourceSet.name, project.createAptConfiguration(sourceSet.name, kotlinPluginVersion))
        }

        val kotlinOptions = KotlinJvmOptionsImpl()
        kotlinOptions.noJdk = true
        ext.addExtension(KOTLIN_OPTIONS_DSL_NAME, kotlinOptions)

        project.createKaptExtension()

        project.afterEvaluate { project ->
            if (project != null) {
                val plugin = (project.plugins.findPlugin("android")
                        ?: project.plugins.findPlugin("android-library")
                        ?: project.plugins.findPlugin("com.android.test")) as BasePlugin

                val variantManager = AndroidGradleWrapper.getVariantDataManager(plugin)
                processVariantData(variantManager.variantDataList, project,
                        ext, plugin, aptConfigurations, kotlinOptions)
            }
        }
    }

    private fun processVariantData(
            variantDataList: List<BaseVariantData<out BaseVariantOutputData>>,
            project: Project,
            androidExt: BaseExtension,
            androidPlugin: BasePlugin,
            aptConfigurations: Map<String, Configuration>,
            rootKotlinOptions: KotlinJvmOptionsImpl
    ) {
        val logger = project.logger
        val subpluginEnvironment = loadSubplugins(project)

        for (variantData in variantDataList) {
            val variantDataName = variantData.name
            logger.kotlinDebug("Process variant [$variantDataName]")

            val javaTask = AndroidGradleWrapper.getJavaCompile(variantData)
            if (javaTask == null) {
                logger.info("KOTLIN: javaTask is missing for $variantDataName, so Kotlin files won't be compiled for it")
                continue
            }

            val kotlinTaskName = "compile${variantDataName.capitalize()}Kotlin"
            // todo: Investigate possibility of creating and configuring kotlinTask before evaluation
            val kotlinTask = tasksProvider.createKotlinJVMTask(project, kotlinTaskName, variantData.name)
            kotlinTask.parentKotlinOptionsImpl = rootKotlinOptions

            // store kotlin classes in separate directory. They will serve as class-path to java compiler
            kotlinTask.destinationDir = File(project.buildDir, "tmp/kotlin-classes/$variantDataName")
            kotlinTask.description = "Compiles the $variantDataName kotlin."
            kotlinTask.setDependsOn(javaTask.dependsOn)

            val isKapt2Enabled = Kapt3GradleSubplugin.isEnabled(project)

            val aptFiles = arrayListOf<File>()

            if (!isKapt2Enabled) {
                for (provider in variantData.sourceProviders) {
                    val aptConfiguration = aptConfigurations[(provider as AndroidSourceSet).name]
                    // Ignore if there's only an annotation processor wrapper in dependencies (added by default)
                    if (aptConfiguration != null && aptConfiguration.dependencies.size > 1) {
                        javaTask.dependsOn(aptConfiguration.buildDependencies)
                        aptFiles.addAll(aptConfiguration.resolve())
                    }
                }
            }

            val appliedPlugins = subpluginEnvironment.addSubpluginOptions(
                    project, kotlinTask, javaTask, variantData, null)

            kotlinTask.mapClasspath {
                javaTask.classpath + project.files(AndroidGradleWrapper.getRuntimeJars(androidPlugin, androidExt))
            }

            var kotlinAfterJavaTask: KotlinCompile? = null

            if (javaTask is JavaCompile && aptFiles.isNotEmpty() && !isKapt2Enabled) {
                val (aptOutputDir, aptWorkingDir) = project.getAptDirsForSourceSet(variantDataName)

                variantData.addJavaSourceFoldersToModel(aptOutputDir)

                val kaptManager = AnnotationProcessingManager(kotlinTask, javaTask, variantDataName,
                        aptFiles.toSet(), aptOutputDir, aptWorkingDir, variantData)

                kotlinAfterJavaTask = project.initKapt(kotlinTask, javaTask, kaptManager,
                        variantDataName, rootKotlinOptions, subpluginEnvironment, tasksProvider)
            }

            configureSources(kotlinTask, variantData)
            if (kotlinAfterJavaTask != null) {
                configureSources(kotlinAfterJavaTask, variantData)
            }
            appliedPlugins
                    .flatMap { it.getSubpluginKotlinTasks(project, kotlinTask) }
                    .forEach { configureSources(it, variantData) }

            configureJavaTask(kotlinTask, javaTask, logger)
            createSyncOutputTask(project, kotlinTask, javaTask, kotlinAfterJavaTask, variantDataName)

            if ((kotlinAfterJavaTask ?: kotlinTask).incremental) {
                val jarToAarMapping = AndroidGradleWrapper.getJarToAarMapping(variantData)
                val artifactFile = project.tryGetSingleArtifact(variantData)
                val artifactDifferenceRegistryProvider = ArtifactDifferenceRegistryProviderAndroidWrapper(kotlinGradleBuildServices.artifactDifferenceRegistryProvider, jarToAarMapping)
                configureMultiProjectIncrementalCompilation(project, kotlinTask, javaTask, kotlinAfterJavaTask,
                        artifactDifferenceRegistryProvider, artifactFile)
            }
        }
    }

    private fun configureSources(compileTask: AbstractCompile, variantData: BaseVariantData<out BaseVariantOutputData>) {
        val logger = compileTask.project.logger

        for (provider in variantData.sourceProviders) {
            val kotlinSourceSet = provider.getConvention(KOTLIN_DSL_NAME) as? KotlinSourceSet ?: continue
            compileTask.source(kotlinSourceSet.kotlin)
        }

        for (javaSrcDir in AndroidGradleWrapper.getJavaSources(variantData)) {
            compileTask.source(javaSrcDir)
            logger.kotlinDebug("Source directory $javaSrcDir was added to kotlin source for ${compileTask.name}")
        }
    }

    private fun Project.tryGetSingleArtifact(variantData: BaseVariantData<*>): File? {
        val log = logger
        log.kotlinDebug { "Trying to determine single artifact for project $path" }

        val outputs = variantData.outputs
        if (outputs.size != 1) {
            log.kotlinDebug { "Output count != 1 for variant: ${outputs.map { it.outputFile.relativeTo(rootDir).path }.joinToString() }" }
            return null
        }

        return variantData.outputs.first().outputFile
    }

    private val BaseVariantData<*>.sourceProviders: List<SourceProvider>
        get() = variantConfiguration.sortedSourceProviders
}

private fun configureJavaTask(kotlinTask: KotlinCompile, javaTask: AbstractCompile, logger: Logger) {
    // Since we cannot update classpath statically, java not able to detect changes in the classpath after kotlin compiler.
    // Therefore this (probably inefficient since java cannot decide "uptodateness" by the list of changed class files, but told
    // explicitly being out of date whenever any kotlin files are compiled
    kotlinTask.anyClassesCompiled = false

    javaTask.outputs.upToDateWhen { task ->
        val kotlinClassesCompiled = kotlinTask.anyClassesCompiled
        if (kotlinClassesCompiled) {
            logger.info("Marking $task out of date, because kotlin classes are changed")
        }
        !kotlinClassesCompiled
    }

    javaTask.dependsOn(kotlinTask.name)
    /*
     * It's important to modify javaTask.classpath only in doFirst,
     * because Android plugin uses ConventionMapping to modify it too (see JavaCompileConfigAction.execute),
     * and setting classpath explicitly prevents usage of Android mappings.
     * Also classpath setted by Android can be modified after excecution of some tasks (see VarianConfiguration.getCompileClasspath)
     * ex. it adds some support libraries jars after execution of prepareComAndroidSupportSupportV42311Library task,
     * so it's only safe to modify javaTask.classpath right before its usage
     */
    javaTask.appendClasspathDynamically(kotlinTask.destinationDir!!)
}

private fun createSyncOutputTask(
        project: Project,
        kotlinTask: KotlinCompile,
        javaTask: AbstractCompile,
        kotlinAfterJavaTask: KotlinCompile?,
        variantName: String
) {
    // if kotlinAfterJavaTask is not null then kotlinTask compiles stubs, so don't sync them
    val kotlinCompile = kotlinAfterJavaTask ?: kotlinTask
    val kotlinDir = kotlinCompile.destinationDir
    val javaDir = javaTask.destinationDir
    val taskName = "copy${variantName.capitalize()}KotlinClasses"

    val syncTask = project.tasks.create(taskName, SyncOutputTask::class.java)
    syncTask.kotlinOutputDir = kotlinDir
    syncTask.javaOutputDir = javaDir
    syncTask.kotlinTask = kotlinCompile
    kotlinTask.javaOutputDir = javaDir
    kotlinAfterJavaTask?.javaOutputDir = javaDir

    // copying should be executed after a latter task
    val previousTask = kotlinAfterJavaTask ?: javaTask
    previousTask.finalizedByIfNotFailed(syncTask)

    project.logger.kotlinDebug { "Created task ${syncTask.path} to copy kotlin classes from $kotlinDir to $javaDir" }
}

private fun loadSubplugins(project: Project): SubpluginEnvironment {
    try {
        val subplugins = ServiceLoader.load(KotlinGradleSubplugin::class.java, project.buildscript.classLoader)
                .map { @Suppress("UNCHECKED_CAST") (it as KotlinGradleSubplugin<KotlinCompile>) }

        return SubpluginEnvironment(project.resolveSubpluginArtifacts(subplugins), subplugins)
    } catch (e: NoClassDefFoundError) {
        // Skip plugin loading if KotlinGradleSubplugin is not defined.
        // It is true now for tests in kotlin-gradle-plugin-core.
        return SubpluginEnvironment(mapOf(), listOf())
    }
}

internal fun Project.resolveSubpluginArtifacts(
        subplugins: List<KotlinGradleSubplugin<KotlinCompile>>
): Map<KotlinGradleSubplugin<KotlinCompile>, List<File>> {
    fun Project.getResolvedArtifacts() = buildscript.configurations.getByName("classpath")
            .resolvedConfiguration.resolvedArtifacts

    val resolvedClasspathArtifacts = getResolvedArtifacts().toMutableList()
    val rootProject = rootProject
    if (rootProject != this) {
        resolvedClasspathArtifacts += rootProject.getResolvedArtifacts()
    }

    val subpluginClasspaths = hashMapOf<KotlinGradleSubplugin<KotlinCompile>, List<File>>()

    for (subplugin in subplugins) {
        val file = resolvedClasspathArtifacts
                .firstOrNull {
                    val id = it.moduleVersion.id
                    subplugin.getGroupName() == id.group && subplugin.getArtifactName() == id.name
                }?.file
        if (file != null) {
            subpluginClasspaths.put(subplugin, listOf(file))
        }
    }

    return subpluginClasspaths
}

internal class SubpluginEnvironment(
        val subpluginClasspaths: Map<KotlinGradleSubplugin<KotlinCompile>, List<File>>,
        val subplugins: List<KotlinGradleSubplugin<KotlinCompile>>
) {

    fun addSubpluginOptions(
            project: Project,
            kotlinTask: KotlinCompile,
            javaTask: AbstractCompile,
            variantData: Any?,
            javaSourceSet: SourceSet?
    ): List<KotlinGradleSubplugin<KotlinCompile>> {
        val pluginOptions = kotlinTask.pluginOptions

        val appliedSubplugins = subplugins.filter { it.isApplicable(project, kotlinTask) }
        for (subplugin in appliedSubplugins) {
            if (!subplugin.isApplicable(project, kotlinTask)) continue

            with(subplugin) {
                project.logger.kotlinDebug("Subplugin ${getCompilerPluginId()} (${getGroupName()}:${getArtifactName()}) loaded.")
            }

            val subpluginClasspath = subpluginClasspaths[subplugin] ?: continue
            subpluginClasspath.forEach { pluginOptions.addClasspathEntry(it) }

            for (option in subplugin.apply(project, kotlinTask, javaTask, variantData, javaSourceSet)) {
                pluginOptions.addPluginArgument(subplugin.getCompilerPluginId(), option.key, option.value)
            }
        }

        return appliedSubplugins
    }
}

private fun Project.getAptDirsForSourceSet(sourceSetName: String): Pair<File, File> {
    val aptOutputDir = File(buildDir, "generated/source/kapt")
    val aptOutputDirForVariant = File(aptOutputDir, sourceSetName)

    val aptWorkingDir = File(buildDir, "tmp/kapt")
    val aptWorkingDirForVariant = File(aptWorkingDir, sourceSetName)

    return aptOutputDirForVariant to aptWorkingDirForVariant
}

private fun Project.createAptConfiguration(sourceSetName: String, kotlinPluginVersion: String): Configuration {
    val aptConfigurationName = Kapt3KotlinGradleSubplugin.getKaptConfigurationName(sourceSetName)

    val aptConfiguration = configurations.create(aptConfigurationName)

    // Add base kotlin-annotation-processing artifact for the main kapt configuration,
    // All other configurations (such as kaptTest) should extend the main one
    if (aptConfiguration.name == Kapt3KotlinGradleSubplugin.MAIN_KAPT_CONFIGURATION_NAME) {
        val kotlinAnnotationProcessingDep = "org.jetbrains.kotlin:kotlin-annotation-processing:$kotlinPluginVersion"
        aptConfiguration.dependencies.add(dependencies.create(kotlinAnnotationProcessingDep))
    } else {
        Kapt3KotlinGradleSubplugin.findMainKaptConfiguration(this)?.let { aptConfiguration.extendsFrom(it) }
    }

    return aptConfiguration
}

private fun Project.createKaptExtension() {
    extensions.create("kapt", KaptExtension::class.java)
}

//copied from BasePlugin.getLocalVersion
private fun loadAndroidPluginVersion(): String? {
    try {
        val clazz = BasePlugin::class.java
        val className = clazz.simpleName + ".class"
        val classPath = clazz.getResource(className).toString()
        if (!classPath.startsWith("jar")) {
            // Class not from JAR, unlikely
            return null
        }
        val manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF"

        val jarConnection = URL(manifestPath).openConnection()
        jarConnection.useCaches = false
        val jarInputStream = jarConnection.inputStream
        val attr = Manifest(jarInputStream).mainAttributes
        jarInputStream.close()
        return attr.getValue("Plugin-Version")
    } catch (t: Throwable) {
        return null
    }
}

//Copied from StringUtil.compareVersionNumbers
private fun compareVersionNumbers(v1: String?, v2: String?): Int {
    if (v1 == null && v2 == null) {
        return 0
    }
    if (v1 == null) {
        return -1
    }
    if (v2 == null) {
        return 1
    }

    val pattern = "[\\.\\_\\-]".toRegex()
    val digitsPattern = "\\d+".toRegex()
    val part1 = v1.split(pattern)
    val part2 = v2.split(pattern)

    var idx = 0
    while (idx < part1.size && idx < part2.size) {
        val p1 = part1[idx]
        val p2 = part2[idx]

        val cmp: Int
        if (p1.matches(digitsPattern) && p2.matches(digitsPattern)) {
            cmp = p1.toInt().compareTo(p2.toInt())
        } else {
            cmp = part1[idx].compareTo(part2[idx])
        }
        if (cmp != 0) return cmp
        idx++
    }

    if (part1.size == part2.size) {
        return 0
    } else {
        val left = part1.size > idx
        val parts = if (left) part1 else part2

        while (idx < parts.size) {
            val p = parts[idx]
            val cmp: Int
            if (p.matches(digitsPattern)) {
                cmp = Integer(p).compareTo(0)
            } else {
                cmp = 1
            }
            if (cmp != 0) return if (left) cmp else -cmp
            idx++
        }
        return 0
    }
}