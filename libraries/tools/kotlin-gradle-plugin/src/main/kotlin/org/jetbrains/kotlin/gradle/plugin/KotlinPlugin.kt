package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.builder.model.SourceProvider
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil.compareVersionNumbers
import com.intellij.util.ReflectionUtil
import groovy.lang.Closure
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.InvalidPluginException
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.internal.*
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.getKaptGeneratedClassesDir
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.scripting.gradle.ScriptingGradleSubplugin
import java.io.File
import java.net.URL
import java.util.*
import java.util.concurrent.Callable
import java.util.jar.Manifest

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

    protected val isSeparateClassesDirSupported: Boolean by lazy {
        !CopyClassesToJavaOutputStatus.isEnabled(project) &&
                sourceSet.output.javaClass.methods.any { it.name == "getClassesDirs" }
    }

    protected val sourceSetName: String = sourceSet.name
    protected val sourceRootDir: String = "src/$sourceSetName/kotlin"
    protected val kotlinSourceSet: KotlinSourceSet = createKotlinSourceSet()
    protected val kotlinTask: T = createKotlinCompileTask()

    protected open val defaultKotlinDestinationDir: File
        get() = if (isSeparateClassesDirSupported)
            File(project.buildDir, "classes/kotlin/${sourceSet.name}") else
            sourceSet.output.classesDir

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

        // Try to avoid duplicate Java sources in allSource:
        val kotlinSrcDirsToAdd = filterOutJavaSrcDirsIfPossible(kotlinDirSet)

        sourceSet.allJava.srcDirs(kotlinSrcDirsToAdd)
        sourceSet.allSource.srcDirs(kotlinSrcDirsToAdd)
        sourceSet.resources.filter.exclude { it.file in kotlinDirSet }
    }

    private fun filterOutJavaSrcDirsIfPossible(sourceDirectorySet: SourceDirectorySet): FileCollection {
        // If the API used below is not available, fall back to not filtering the Java sources.
        if (SourceDirectorySet::class.java.methods.none { it.name == "getSourceDirectories" }) {
            return sourceDirectorySet
        }

        fun getSourceDirectories(sourceDirectorySet: SourceDirectorySet): FileCollection {
            val method = SourceDirectorySet::class.java.getMethod("getSourceDirectories")
            return method(sourceDirectorySet) as FileCollection
        }

        // Build a lazily-resolved file collection that filters out Java sources from sources of this sourceDirectorySet
        return getSourceDirectories(sourceDirectorySet).minus(getSourceDirectories(sourceSet.java))
    }

    private fun createKotlinCompileTask(): T {
        val name = sourceSet.getCompileTaskName(compileTaskNameSuffix)
        logger.kotlinDebug("Creating kotlin compile task $name")
        val kotlinCompile = doCreateTask(project, name)
        kotlinCompile.description = taskDescription
        kotlinCompile.mapClasspath { sourceSet.compileClasspath }
        kotlinCompile.setDestinationDir { defaultKotlinDestinationDir }
        sourceSet.output.tryAddClassesDir { project.files(kotlinTask.destinationDir).builtBy(kotlinTask) }
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
        private val kotlinPluginVersion: String
) : KotlinSourceSetProcessor<KotlinCompile>(
        project, javaBasePlugin, sourceSet, tasksProvider, kotlinSourceSetProvider,
        dslExtensionName = KOTLIN_DSL_NAME,
        compileTaskNameSuffix = "kotlin",
        taskDescription = "Compiles the $sourceSet.kotlin."
) {
    override val defaultKotlinDestinationDir: File
        get() = if (!isSeparateClassesDirSupported)
            File(project.buildDir, "kotlin-classes/$sourceSetName") else
            super.defaultKotlinDestinationDir

    override fun doCreateTask(project: Project, taskName: String): KotlinCompile =
            tasksProvider.createKotlinJVMTask(project, taskName, sourceSet.name)

    override fun doTargetSpecificProcessing() {
        kotlinSourceSet.kotlin.source(sourceSet.java)

        project.afterEvaluate { project ->
            if (project != null) {
                val javaTask = project.tasks.findByName(sourceSet.compileJavaTaskName)

                val subpluginEnvironment = loadSubplugins(project)
                val appliedPlugins = subpluginEnvironment.addSubpluginOptions(
                        project, kotlinTask, javaTask as JavaCompile, null, null, sourceSet)

                removeAnnotationProcessingPluginClasspathEntry(kotlinTask)

                // KotlinCompile.source(kotlinDirSet) should be called only after all java roots are added to kotlinDirSet
                // otherwise some java roots can be ignored
                kotlinTask.source(kotlinSourceSet.kotlin)
                appliedPlugins
                        .flatMap { it.getSubpluginKotlinTasks(project, kotlinTask) }
                        .forEach { it.source(kotlinSourceSet.kotlin) }

                configureJavaTask(kotlinTask, javaTask, logger)

                var syncOutputTask: SyncOutputTask? = null

                if (!isSeparateClassesDirSupported) {
                    syncOutputTask = createSyncOutputTask(project, kotlinTask, javaTask, sourceSetName)
                }

                if (project.pluginManager.hasPlugin("java-library") && sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME) {
                    val (classesProviderTask, classesDirectory) = when {
                        isSeparateClassesDirSupported -> kotlinTask.let { it to it.destinationDir }
                        else -> syncOutputTask!!.let { it to it.javaOutputDir }
                    }

                    registerKotlinOutputForJavaLibrary(classesDirectory, classesProviderTask)
                }

            }
        }
    }

    private fun registerKotlinOutputForJavaLibrary(outputDir: File, taskDependency: Task): Boolean {
        val configuration = project.configurations.getByName("apiElements")

        checkedReflection({
            val getOutgoing = configuration.javaClass.getMethod("getOutgoing")
            val outgoing = getOutgoing(configuration)

            val getVariants = outgoing.javaClass.getMethod("getVariants")
            val variants = getVariants(outgoing) as NamedDomainObjectCollection<*>

            val variant = variants.getByName("classes")

            val artifactMethod = variant.javaClass.getMethod("artifact", Any::class.java)

            val artifactMap = mapOf(
                    "file" to outputDir,
                    "type" to "java-classes-directory",
                    "builtBy" to taskDependency
            )

            artifactMethod(variant, artifactMap)

            return true

        }, { reflectException ->
            logger.kotlinWarn("Could not register Kotlin output of source set $sourceSetName for java-library: $reflectException")
            return false
        })
    }
}

internal fun SourceSetOutput.tryAddClassesDir(
        classesDirProvider: () -> FileCollection
): Boolean {
    val getClassesDirs = ReflectionUtil.findMethod(
            javaClass.methods.asList(),
            "getClassesDirs"
    ) ?: return false

    val classesDirs = getClassesDirs(this) as? ConfigurableFileCollection
            ?: return false

    classesDirs.from(Callable { classesDirProvider() })
    return true
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
    override fun doCreateTask(project: Project, taskName: String): Kotlin2JsCompile =
            tasksProvider.createKotlinJSTask(project, taskName, sourceSet.name)

    override fun doTargetSpecificProcessing() {
        project.tasks.findByName(sourceSet.classesTaskName)!!.dependsOn(kotlinTask)
        kotlinTask.source(kotlinSourceSet.kotlin)
        createCleanSourceMapTask()

        sourceSet.clearJavaSrcDirs()

        // outputFile can be set later during the configuration phase, get it only after the phase:
        project.afterEvaluate { project ->
            val subpluginEnvironment: SubpluginEnvironment = loadSubplugins(project)
            val appliedPlugins = subpluginEnvironment.addSubpluginOptions(
                    project, kotlinTask, kotlinTask, null, null, sourceSet)

            kotlinTask.kotlinOptions.outputFile = kotlinTask.outputFile.absolutePath
            val outputDir = kotlinTask.outputFile.parentFile

            if (FileUtil.isAncestor(outputDir, project.rootDir, false))
                throw InvalidUserDataException(
                        "The output directory '$outputDir' (defined by outputFile of $kotlinTask) contains or " +
                        "matches the project root directory '${project.rootDir}'.\n" +
                        "Gradle will not be able to build the project because of the root directory lock.\n" +
                        "To fix this, consider using the default outputFile location instead of providing it explicitly.")

            kotlinTask.destinationDir = outputDir

            if (!isSeparateClassesDirSupported) {
                sourceSet.output.setClassesDirCompatible(kotlinTask.destinationDir)
            }

            appliedPlugins
                    .flatMap { it.getSubpluginKotlinTasks(project, kotlinTask) }
                    .forEach { it.source(kotlinSourceSet.kotlin) }
        }
    }

    private fun createCleanSourceMapTask() {
        val taskName = sourceSet.getTaskName("clean", "sourceMap")
        val task = project.tasks.create(taskName, Delete::class.java)
        task.onlyIf { kotlinTask.kotlinOptions.sourceMap }
        task.delete(object : Closure<String>(this) {
            override fun call(): String? = (kotlinTask.property("outputFile") as File).canonicalPath + ".map"
        })
        project.tasks.findByName("clean")?.dependsOn(taskName)
    }
}

internal class KotlinCommonSourceSetProcessor(
        project: Project,
        javaBasePlugin: JavaBasePlugin,
        sourceSet: SourceSet,
        tasksProvider: KotlinTasksProvider,
        kotlinSourceSetProvider: KotlinSourceSetProvider
) : KotlinSourceSetProcessor<KotlinCompileCommon>(
        project, javaBasePlugin, sourceSet, tasksProvider, kotlinSourceSetProvider,
        dslExtensionName = KOTLIN_DSL_NAME,
        taskDescription = "Compiles the kotlin sources in $sourceSet to Metadata.",
        compileTaskNameSuffix = "kotlinCommon"
) {
    override fun doTargetSpecificProcessing() {
        sourceSet.clearJavaSrcDirs()

        project.afterEvaluate { project ->
            kotlinTask.source(kotlinSourceSet.kotlin)

            val subpluginEnvironment: SubpluginEnvironment = loadSubplugins(project)
            val appliedPlugins = subpluginEnvironment.addSubpluginOptions(
                    project, kotlinTask, kotlinTask, null, null, sourceSet)

            project.tasks.findByName(sourceSet.classesTaskName)!!.dependsOn(kotlinTask)
            // can be missing (e.g. in case of tests)
            project.tasks.findByName(sourceSet.jarTaskName)?.dependsOn(kotlinTask)

            appliedPlugins
                    .flatMap { it.getSubpluginKotlinTasks(project, kotlinTask) }
                    .forEach { it.source(kotlinSourceSet.kotlin) }
        }
    }

    override fun doCreateTask(project: Project, taskName: String): KotlinCompileCommon =
            tasksProvider.createKotlinCommonTask(project, taskName, sourceSet.name)
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
        configureDefaultVersionsResolutionStrategy(project)
        configureClassInspectionForIC(project)
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

    private fun configureDefaultVersionsResolutionStrategy(project: Project) {
        project.configurations.all { configuration ->
            if (isGradleVersionAtLeast(4, 4)) {
                // Use the API introduced in Gradle 4.4 to modify the dependencies directly before they are resolved:
                configuration.withDependencies { dependencySet ->
                    dependencySet.filterIsInstance<ExternalDependency>()
                            .filter { it.group == "org.jetbrains.kotlin" && it.version.isNullOrEmpty() }
                            .forEach { it.version { constraint -> constraint.prefer(kotlinPluginVersion) } }
                }
            }
            else {
                configuration.resolutionStrategy.eachDependency { details ->
                    val requested = details.requested
                    if (requested.group == "org.jetbrains.kotlin" && requested.version.isEmpty()) {
                        details.useVersion(kotlinPluginVersion)
                    }
                }
            }
        }
    }

    private fun configureClassInspectionForIC(project: Project) {
        val classesTask = project.tasks.findByName(JavaPlugin.CLASSES_TASK_NAME)
        val jarTask = project.tasks.findByName(JavaPlugin.JAR_TASK_NAME)

        if (classesTask == null || jarTask !is Jar) {
            project.logger.info(
                "Could not configure class inspection task " +
                        "(classes task = ${classesTask?.javaClass?.canonicalName}, " +
                        "jar task = ${classesTask?.javaClass?.canonicalName}"
            )
            return
        }
        val inspectTask = project.tasks.create("inspectClassesForKotlinIC", InspectClassesForMultiModuleIC::class.java)
        inspectTask.jarTask = jarTask
        inspectTask.dependsOn(classesTask)
        jarTask.dependsOn(inspectTask)
    }
}

internal open class KotlinPlugin(
        tasksProvider: KotlinTasksProvider,
        kotlinSourceSetProvider: KotlinSourceSetProvider,
        kotlinPluginVersion: String
) : AbstractKotlinPlugin(tasksProvider, kotlinSourceSetProvider, kotlinPluginVersion) {
    override fun buildSourceSetProcessor(project: Project, javaBasePlugin: JavaBasePlugin, sourceSet: SourceSet, kotlinPluginVersion: String) =
            Kotlin2JvmSourceSetProcessor(project, javaBasePlugin, sourceSet, tasksProvider, kotlinSourceSetProvider, kotlinPluginVersion)

    override fun apply(project: Project) {
        project.pluginManager.apply(ScriptingGradleSubplugin::class.java)
        super.apply(project)
    }
}

internal open class KotlinCommonPlugin(
        tasksProvider: KotlinTasksProvider,
        kotlinSourceSetProvider: KotlinSourceSetProvider,
        kotlinPluginVersion: String
) : AbstractKotlinPlugin(tasksProvider, kotlinSourceSetProvider, kotlinPluginVersion) {
    override fun buildSourceSetProcessor(project: Project, javaBasePlugin: JavaBasePlugin, sourceSet: SourceSet, kotlinPluginVersion: String) =
            KotlinCommonSourceSetProcessor(project, javaBasePlugin, sourceSet, tasksProvider, kotlinSourceSetProvider)
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
        private val kotlinPluginVersion: String
) : Plugin<Project> {

    override fun apply(project: Project) {
        val version = loadAndroidPluginVersion()
        if (version != null) {
            val minimalVersion = "1.1.0"
            if (compareVersionNumbers(version, minimalVersion) < 0) {
                throw IllegalStateException("Kotlin: Unsupported version of com.android.tools.build:gradle plugin: version $minimalVersion or higher should be used with kotlin-android plugin")
            }
        }

        val kotlinTools = KotlinConfigurationTools(
                kotlinSourceSetProvider,
                tasksProvider,
                kotlinPluginVersion)

        val legacyVersionThreshold = "2.5.0"

        val variantProcessor = if (compareVersionNumbers(version, legacyVersionThreshold) < 0) {
            LegacyAndroidAndroidProjectHandler(kotlinTools)
        }
        else {
            val android25ProjectHandlerClass = Class.forName("org.jetbrains.kotlin.gradle.plugin.Android25ProjectHandler")
            val ctor = android25ProjectHandlerClass.constructors.single {
                it.parameterTypes.contentEquals(arrayOf(kotlinTools.javaClass))
            }
            ctor.newInstance(kotlinTools) as AbstractAndroidProjectHandler<*>
        }

        variantProcessor.handleProject(project)
    }
}

class KotlinConfigurationTools internal constructor(val kotlinSourceSetProvider: KotlinSourceSetProvider,
                                                    val kotlinTasksProvider: KotlinTasksProvider,
                                                    val kotlinPluginVersion: String)

/** Part of Android configuration, that works only with the old public API.
 * @see [LegacyAndroidAndroidProjectHandler] that is implemented with the old internal API and [AndroidGradle25VariantProcessor] that works
 *       with the new public API */
abstract class AbstractAndroidProjectHandler<V>(private val kotlinConfigurationTools: KotlinConfigurationTools) {
    protected val logger = Logging.getLogger(this.javaClass)

    abstract fun forEachVariant(project: Project, action: (V) -> Unit): Unit
    abstract fun getTestedVariantData(variantData: V): V?
    abstract fun getResDirectories(variantData: V): List<File>

    protected abstract fun getSourceProviders(variantData: V): Iterable<SourceProvider>
    protected abstract fun getAllJavaSources(variantData: V): Iterable<File>
    protected abstract fun getVariantName(variant: V): String
    protected abstract fun getJavaTask(variantData: V): AbstractCompile?
    protected abstract fun addJavaSourceDirectoryToVariantModel(variantData: V, javaSourceDirectory: File): Unit

    protected open fun checkVariantIsValid(variant: V) = Unit

    protected abstract fun wireKotlinTasks(project: Project,
                                           androidPlugin: BasePlugin,
                                           androidExt: BaseExtension,
                                           variantData: V,
                                           javaTask: AbstractCompile,
                                           kotlinTask: KotlinCompile)

    protected abstract fun wrapVariantDataForKapt(variantData: V): KaptVariantData<V>

    fun handleProject(project: Project) {
        val ext = project.extensions.getByName("android") as BaseExtension
        val aptConfigurations = hashMapOf<String, Configuration>()

        ext.sourceSets.all { sourceSet ->
            logger.kotlinDebug("Creating KotlinSourceSet for source set $sourceSet")
            val kotlinSourceSet = kotlinConfigurationTools.kotlinSourceSetProvider.create(sourceSet.name)
            kotlinSourceSet.kotlin.srcDir(project.file(project.file("src/${sourceSet.name}/kotlin")))
            sourceSet.addConvention(KOTLIN_DSL_NAME, kotlinSourceSet)
        }

        val kotlinOptions = KotlinJvmOptionsImpl()
        kotlinOptions.noJdk = true
        ext.addExtension(KOTLIN_OPTIONS_DSL_NAME, kotlinOptions)

        project.afterEvaluate { project ->
            if (project != null) {
                val androidPluginIds = listOf("android", "com.android.application", "android-library", "com.android.library",
                        "com.android.test", "com.android.feature", "com.android.dynamic-feature")
                val plugin = androidPluginIds.asSequence()
                                     .mapNotNull { project.plugins.findPlugin(it) as? BasePlugin }
                                     .firstOrNull()
                             ?: throw InvalidPluginException("'kotlin-android' expects one of the Android Gradle " +
                                                             "plugins to be applied to the project:\n\t" +
                                                             androidPluginIds.joinToString("\n\t") { "* $it" })

                val subpluginEnvironment = loadSubplugins(project)

                checkAndroidAnnotationProcessorDependencyUsage(project)

                forEachVariant(project) {
                    processVariant(it, project, ext, plugin, aptConfigurations, kotlinOptions,
                            kotlinConfigurationTools.kotlinTasksProvider, subpluginEnvironment)
                }
            }
        }
    }

    private fun processVariant(variantData: V,
                               project: Project,
                               androidExt: BaseExtension,
                               androidPlugin: BasePlugin,
                               aptConfigurations: Map<String, Configuration>,
                               rootKotlinOptions: KotlinJvmOptionsImpl,
                               tasksProvider: KotlinTasksProvider, subpluginEnvironment: SubpluginEnvironment) {

        checkVariantIsValid(variantData)

        val variantDataName = getVariantName(variantData)
        logger.kotlinDebug("Process variant [$variantDataName]")

        val javaTask = getJavaTask(variantData)

        if (javaTask == null) {
            logger.info("KOTLIN: javaTask is missing for $variantDataName, so Kotlin files won't be compiled for it")
            return
        }

        val kotlinTaskName = "compile${variantDataName.capitalize()}Kotlin"
        // todo: Investigate possibility of creating and configuring kotlinTask before evaluation
        val kotlinTask = tasksProvider.createKotlinJVMTask(project, kotlinTaskName, variantDataName)
        kotlinTask.parentKotlinOptionsImpl = rootKotlinOptions

        // store kotlin classes in separate directory. They will serve as class-path to java compiler
        kotlinTask.destinationDir = File(project.buildDir, "tmp/kotlin-classes/$variantDataName")
        kotlinTask.description = "Compiles the $variantDataName kotlin."

        removeAnnotationProcessingPluginClasspathEntry(kotlinTask)

        configureSources(kotlinTask, variantData)
        wireKotlinTasks(project, androidPlugin, androidExt, variantData, javaTask, kotlinTask)

        val appliedPlugins = subpluginEnvironment.addSubpluginOptions(
                project, kotlinTask, javaTask, wrapVariantDataForKapt(variantData), this, null)

        appliedPlugins.flatMap { it.getSubpluginKotlinTasks(project, kotlinTask) }
                .forEach { configureSources(it, variantData) }
    }

    private fun configureSources(compileTask: AbstractCompile, variantData: V) {
        val logger = compileTask.project.logger

        for (provider in getSourceProviders(variantData)) {
            val kotlinSourceSet = provider.getConvention(KOTLIN_DSL_NAME) as? KotlinSourceSet ?: continue
            compileTask.source(kotlinSourceSet.kotlin)
        }

        for (javaSrcDir in getAllJavaSources(variantData)) {
            compileTask.source(javaSrcDir)
            logger.kotlinDebug("Source directory $javaSrcDir was added to kotlin source for ${compileTask.name}")
        }
    }
}

internal fun configureJavaTask(kotlinTask: KotlinCompile, javaTask: AbstractCompile, logger: Logger) {
    kotlinTask.javaOutputDir = javaTask.destinationDir

    // Gradle Java IC in older Gradle versions (before 2.14) cannot check .class directories updates.
    // To make it work, reset the up-to-date status of compileJava with this flag.
    kotlinTask.anyClassesCompiled = false
    val gradleSupportsJavaIcWithClassesDirs = ParsedGradleVersion.parse(javaTask.project.gradle.gradleVersion)
                                                      ?.let { it >= ParsedGradleVersion(2, 14) } ?: false
    if (!gradleSupportsJavaIcWithClassesDirs) {
        javaTask.outputs.upToDateWhen { task ->
            if (kotlinTask.anyClassesCompiled) {
                logger.info("Marking $task out of date, because kotlin classes are changed")
                false
            } else true
        }
    }

    // Make Gradle check if the javaTask is up-to-date based on the Kotlin classes
    javaTask.inputsCompatible.run {
        if (isBuildCacheSupported()) {
            dir(kotlinTask.destinationDir)
                .withNormalizer(CompileClasspathNormalizer::class.java)
                .withPropertyName("${kotlinTask.name}OutputClasses")
        }
        else {
            dirCompatible(kotlinTask.destinationDir)
        }
    }
    // Also, use kapt1 annotations file for up-to-date check since annotation processing is done with javac
    javaTask.dependsOn(kotlinTask)
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

internal fun syncOutputTaskName(variantName: String) = "copy${variantName.capitalize()}KotlinClasses"

internal fun createSyncOutputTask(
        project: Project,
        kotlinCompile: KotlinCompile,
        javaTask: AbstractCompile,
        variantName: String
): SyncOutputTask {
    val kotlinDir = kotlinCompile.destinationDir
    val javaDir = javaTask.destinationDir
    val taskName = syncOutputTaskName(variantName)

    val syncTask = project.tasks.create(taskName, SyncOutputTask::class.java)
    syncTask.kotlinOutputDir = kotlinDir
    syncTask.javaOutputDir = javaDir
    syncTask.kotlinTask = kotlinCompile
    kotlinCompile.javaOutputDir = javaDir
    syncTask.kaptClassesDir = getKaptGeneratedClassesDir(project, variantName)

    // copying should be executed after a latter task
    javaTask.finalizedByIfNotFailed(syncTask)

    project.logger.kotlinDebug { "Created task ${syncTask.path} to copy kotlin classes from $kotlinDir to $javaDir" }

    return syncTask
}

private fun SourceSet.clearJavaSrcDirs() {
    java.setSrcDirs(emptyList<File>())
}

private val KOTLIN_ANNOTATION_PROCESSING_FILE_REGEX = "kotlin-annotation-processing-[\\-0-9A-Za-z.]+\\.jar".toRegex()

private fun removeAnnotationProcessingPluginClasspathEntry(kotlinCompile: KotlinCompile) {
    kotlinCompile.pluginOptions.classpath
        .map(::File)
        .filter { it.name.matches(KOTLIN_ANNOTATION_PROCESSING_FILE_REGEX) }
        .forEach {
            kotlinCompile.logger.kotlinDebug("Removing plugin classpath dependency $it")
            kotlinCompile.pluginOptions.removeClasspathEntry(it)
        }
}

private fun loadSubplugins(project: Project): SubpluginEnvironment {
    try {
        val subplugins = ServiceLoader.load(KotlinGradleSubplugin::class.java, project.buildscript.classLoader)
                .map { @Suppress("UNCHECKED_CAST") (it as KotlinGradleSubplugin<AbstractCompile>) }

        return SubpluginEnvironment(project.resolveSubpluginArtifacts(subplugins), subplugins)
    } catch (e: NoClassDefFoundError) {
        // Skip plugin loading if KotlinGradleSubplugin is not defined.
        // It is true now for tests in kotlin-gradle-plugin-core.
        return SubpluginEnvironment(mapOf(), listOf())
    }
}

internal fun <T: AbstractCompile> Project.resolveSubpluginArtifacts(
        subplugins: List<KotlinGradleSubplugin<T>>
): Map<KotlinGradleSubplugin<T>, List<File>> {
    fun Project.getResolvedArtifacts() = buildscript.configurations.getByName("classpath")
            .resolvedConfiguration.resolvedArtifacts

    val resolvedClasspathArtifacts = getResolvedArtifacts().toMutableList()
    val rootProject = rootProject
    if (rootProject != this) {
        resolvedClasspathArtifacts += rootProject.getResolvedArtifacts()
    }

    val subpluginClasspaths = hashMapOf<KotlinGradleSubplugin<T>, List<File>>()

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
        val subpluginClasspaths: Map<KotlinGradleSubplugin<AbstractCompile>, List<File>>,
        val subplugins: List<KotlinGradleSubplugin<AbstractCompile>>
) {

    fun <C: CommonCompilerArguments> addSubpluginOptions(
            project: Project,
            kotlinTask: AbstractKotlinCompile<C>,
            javaTask: AbstractCompile,
            variantData: Any?,
            androidProjectHandler: AbstractAndroidProjectHandler<out Any?>?,
            javaSourceSet: SourceSet?
    ): List<KotlinGradleSubplugin<AbstractKotlinCompile<C>>> {
        val pluginOptions = kotlinTask.pluginOptions

        val appliedSubplugins = subplugins.filter { it.isApplicable(project, kotlinTask) }
        for (subplugin in appliedSubplugins) {
            if (!subplugin.isApplicable(project, kotlinTask)) continue

            with(subplugin) {
                project.logger.kotlinDebug("Subplugin ${getCompilerPluginId()} (${getGroupName()}:${getArtifactName()}) loaded.")
            }

            val subpluginClasspath = subpluginClasspaths[subplugin] ?: continue
            subpluginClasspath.forEach { pluginOptions.addClasspathEntry(it) }

            val subpluginOptions = subplugin.apply(project, kotlinTask, javaTask, variantData, androidProjectHandler, javaSourceSet)
            val subpluginId = subplugin.getCompilerPluginId()
            kotlinTask.registerSubpluginOptionsAsInputs(subpluginId, subpluginOptions)

            for (option in subpluginOptions) {
                pluginOptions.addPluginArgument(subpluginId, option)
            }
        }

        return appliedSubplugins
    }
}

internal fun Task.registerSubpluginOptionsAsInputs(subpluginId: String, subpluginOptions: List<SubpluginOption>) {
    // There might be several options with the same key. We group them together
    // and add an index to the Gradle input property name to resolve possible duplication:
    val pluginOptionsGrouped = subpluginOptions.groupBy { it.key }
    for ((optionKey, optionsGroup) in pluginOptionsGrouped) {
        optionsGroup.forEachIndexed { index, option ->
            val indexSuffix = if (optionsGroup.size > 1) ".$index" else ""
            when (option) {
                is CompositeSubpluginOption -> {
                    val subpluginIdWithWrapperKey = "$subpluginId.${optionKey}$indexSuffix"
                    registerSubpluginOptionsAsInputs(subpluginIdWithWrapperKey, option.originalOptions)
                }

                is FilesSubpluginOption -> when (option.kind) {
                    FilesOptionKind.INTERNAL -> Unit
                }.run { /* exhaustive when */ }

                else -> {
                    inputsCompatible.propertyCompatible("$subpluginId." + option.key + indexSuffix, option.value)
                }
            }
        }
    }
}

//copied from BasePlugin.getLocalVersion
internal fun loadAndroidPluginVersion(): String? {
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
internal fun compareVersionNumbers(v1: String?, v2: String?): Int {
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