package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.builder.model.SourceProvider
import groovy.lang.Closure
import org.gradle.api.*
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.InvalidPluginException
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.CompileClasspathNormalizer
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetOutput
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleJavaTargetExtension
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin.Companion.getKaptGeneratedClassesDir
import org.jetbrains.kotlin.gradle.internal.Kapt3KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.internal.KaptVariantData
import org.jetbrains.kotlin.gradle.internal.checkAndroidAnnotationProcessorDependencyUsage
import org.jetbrains.kotlin.gradle.model.builder.KotlinModelBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.source.KotlinSourceSet
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.*
import java.io.File
import java.net.URL
import java.util.concurrent.Callable
import java.util.jar.Manifest

const val PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerPluginClasspath"
internal const val COMPILER_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerClasspath"
val KOTLIN_DSL_NAME = "kotlin"
val KOTLIN_JS_DSL_NAME = "kotlin2js"
val KOTLIN_OPTIONS_DSL_NAME = "kotlinOptions"

internal abstract class KotlinSourceSetProcessor<T : AbstractKotlinCompile<*>>(
    val project: Project,
    val tasksProvider: KotlinTasksProvider,
    val taskDescription: String,
    val kotlinCompilation: KotlinCompilation
) {
    protected abstract fun doTargetSpecificProcessing()
    protected val logger = Logging.getLogger(this.javaClass)!!

    protected val isSeparateClassesDirSupported: Boolean by lazy {
        !CopyClassesToJavaOutputStatus.isEnabled(project) &&
                kotlinCompilation.output.javaClass.methods.any { it.name == "getClassesDirs" }
    }

    protected val sourceSetName: String = kotlinCompilation.compilationName

    protected val kotlinTask: T = createKotlinCompileTask()

    protected val javaSourceSet: SourceSet? = (kotlinCompilation as? KotlinWithJavaCompilation)?.javaSourceSet

    protected open val defaultKotlinDestinationDir: File
        get() {
            return if (isSeparateClassesDirSupported) {
                val kotlinExt = project.kotlinExtension
                val targetSubDirectory =
                    if (kotlinExt is KotlinSingleJavaTargetExtension)
                        "" // In single-target projects, don't add the target name part to this path
                    else
                        kotlinCompilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
                File(project.buildDir, "classes/kotlin/$targetSubDirectory${kotlinCompilation.compilationName}")
            } else {
                kotlinCompilation.output.classesDir
            }
        }

    private fun createKotlinCompileTask(): T {
        val name = kotlinCompilation.compileKotlinTaskName
        logger.kotlinDebug("Creating kotlin compile task $name")
        val kotlinCompile = doCreateTask(project, name)
        kotlinCompile.description = taskDescription
        kotlinCompile.mapClasspath { kotlinCompilation.compileDependencyFiles }
        kotlinCompile.setDestinationDir { defaultKotlinDestinationDir }
        kotlinCompilation.output.tryAddClassesDir { project.files(kotlinTask.destinationDir).builtBy(kotlinTask) }
        return kotlinCompile
    }

    open fun run() {
        addKotlinDirectoriesToJavaSourceSet()
        doTargetSpecificProcessing()
    }

    private fun addKotlinDirectoriesToJavaSourceSet() {
        if (javaSourceSet == null)
            return

        // Try to avoid duplicate Java sources in allSource; run lazily to allow changing the directory set:
        val kotlinSrcDirsToAdd = Callable {
            kotlinCompilation.kotlinSourceSets.map { filterOutJavaSrcDirsIfPossible(it.kotlin) }
        }

        javaSourceSet.allJava.srcDirs(kotlinSrcDirsToAdd)
        javaSourceSet.allSource.srcDirs(kotlinSrcDirsToAdd)
    }

    private fun filterOutJavaSrcDirsIfPossible(sourceDirectorySet: SourceDirectorySet): FileCollection {
        if (javaSourceSet == null)
            return sourceDirectorySet

        // If the API used below is not available, fall back to not filtering the Java sources.
        if (SourceDirectorySet::class.java.methods.none { it.name == "getSourceDirectories" }) {
            return sourceDirectorySet
        }

        fun getSourceDirectories(sourceDirectorySet: SourceDirectorySet): FileCollection {
            val method = SourceDirectorySet::class.java.getMethod("getSourceDirectories")
            return method(sourceDirectorySet) as FileCollection
        }

        // Build a lazily-resolved file collection that filters out Java sources from sources of this sourceDirectorySet
        return getSourceDirectories(sourceDirectorySet).minus(getSourceDirectories(javaSourceSet.java))
    }

    protected abstract fun doCreateTask(project: Project, taskName: String): T
}

internal class Kotlin2JvmSourceSetProcessor(
    project: Project,
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: KotlinCompilation,
    private val kotlinPluginVersion: String
) : KotlinSourceSetProcessor<KotlinCompile>(
    project, tasksProvider, "Compiles the $kotlinCompilation.", kotlinCompilation
) {
    override val defaultKotlinDestinationDir: File
        get() = if (!isSeparateClassesDirSupported)
            File(project.buildDir, "kotlin-classes/$sourceSetName") else
            super.defaultKotlinDestinationDir

    override fun doCreateTask(project: Project, taskName: String): KotlinCompile =
            tasksProvider.createKotlinJVMTask(project, taskName, kotlinCompilation)

    override fun doTargetSpecificProcessing() {
        Kapt3KotlinGradleSubplugin.createAptConfigurationIfNeeded(project, kotlinCompilation.compilationName)

        project.afterEvaluate { project ->
            val javaTask = javaSourceSet?.let { project.tasks.findByName(it.compileJavaTaskName) as JavaCompile }

            val subpluginEnvironment = SubpluginEnvironment.loadSubplugins(project, kotlinPluginVersion)
            val appliedPlugins = subpluginEnvironment.addSubpluginOptions(
                project, kotlinTask, javaTask, null, null, kotlinCompilation
            )

            appliedPlugins
                .flatMap { it.getSubpluginKotlinTasks(project, kotlinTask) }
                .forEach { plugin -> kotlinCompilation.kotlinSourceSets.forEach { sourceSet -> plugin.source(sourceSet.kotlin) } }

            javaTask?.let { configureJavaTask(kotlinTask, it, logger) }

            var syncOutputTask: SyncOutputTask? = null

            if (!isSeparateClassesDirSupported && javaTask != null) {
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
    val getClassesDirs = javaClass.methods.firstOrNull { it.name == "getClassesDirs" && it.parameterCount == 0 }
            ?: return false

    val classesDirs = getClassesDirs(this) as? ConfigurableFileCollection
            ?: return false

    classesDirs.from(Callable { classesDirProvider() })
    return true
}

internal class Kotlin2JsSourceSetProcessor(
    project: Project,
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: KotlinCompilation,
    private val kotlinPluginVersion: String
) : KotlinSourceSetProcessor<Kotlin2JsCompile>(
    project, tasksProvider, taskDescription = "Compiles the Kotlin sources in $kotlinCompilation to JavaScript.",
    kotlinCompilation = kotlinCompilation
) {
    override fun doCreateTask(project: Project, taskName: String): Kotlin2JsCompile =
            tasksProvider.createKotlinJSTask(project, taskName, kotlinCompilation)

    override fun doTargetSpecificProcessing() {
        project.tasks.findByName(kotlinCompilation.compileAllTaskName)!!.dependsOn(kotlinTask)

        createCleanSourceMapTask()

        if (kotlinCompilation is KotlinWithJavaCompilation) {
            kotlinCompilation.javaSourceSet.clearJavaSrcDirs()
        }

        // outputFile can be set later during the configuration phase, get it only after the phase:
        project.afterEvaluate { project ->
            kotlinTask.kotlinOptions.outputFile = kotlinTask.outputFile.absolutePath
            val outputDir = kotlinTask.outputFile.parentFile

            val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project, kotlinPluginVersion)
            val appliedPlugins = subpluginEnvironment.addSubpluginOptions(
                    project, kotlinTask, null, null, null, kotlinCompilation)

            if (outputDir.isParentOf(project.rootDir))
                throw InvalidUserDataException(
                        "The output directory '$outputDir' (defined by outputFile of $kotlinTask) contains or " +
                        "matches the project root directory '${project.rootDir}'.\n" +
                        "Gradle will not be able to build the project because of the root directory lock.\n" +
                        "To fix this, consider using the default outputFile location instead of providing it explicitly.")

            kotlinTask.destinationDir = outputDir

            if (!isSeparateClassesDirSupported) {
                kotlinCompilation.output.setClassesDirCompatible(kotlinTask.destinationDir)
            }

            appliedPlugins
                    .flatMap { it.getSubpluginKotlinTasks(project, kotlinTask) }
                    .forEach { task -> kotlinCompilation.kotlinSourceSets.forEach { sourceSet -> task.source(sourceSet.kotlin) } }
        }
    }

    private fun createCleanSourceMapTask() {
        val taskName = kotlinCompilation.composeName("clean", "sourceMap")
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
    compilation: KotlinCompilation,
    tasksProvider: KotlinTasksProvider,
    private val kotlinPluginVersion: String
) : KotlinSourceSetProcessor<KotlinCompileCommon>(
    project, tasksProvider, taskDescription = "Compiles the kotlin sources in $compilation to Metadata.",
    kotlinCompilation = compilation
) {
    override fun doTargetSpecificProcessing() {
        project.tasks.findByName(kotlinCompilation.compileAllTaskName)!!.dependsOn(kotlinTask)
        // can be missing (e.g. in case of tests)
        if (kotlinCompilation.compilationName == KotlinCompilation.MAIN_COMPILATION_NAME) {
            project.tasks.findByName(kotlinCompilation.target.artifactsTaskName)?.dependsOn(kotlinTask)
        }

        project.afterEvaluate { project ->
            val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project, kotlinPluginVersion)
            val appliedPlugins = subpluginEnvironment.addSubpluginOptions(
                project, kotlinTask, null, null, null, kotlinCompilation
            )
            appliedPlugins
                .flatMap { it.getSubpluginKotlinTasks(project, kotlinTask) }
                .forEach { kotlinCompilation.kotlinSourceSets.forEach { sourceSet -> it.source(sourceSet.kotlin) } }
        }
    }

    override fun doCreateTask(project: Project, taskName: String): KotlinCompileCommon =
            tasksProvider.createKotlinCommonTask(project, taskName, kotlinCompilation)
}

internal abstract class AbstractKotlinPlugin(
    val tasksProvider: KotlinTasksProvider,
    protected val kotlinPluginVersion: String,
        val registry: ToolingModelBuilderRegistry
) : Plugin<Project> {

    internal abstract fun buildSourceSetProcessor(project: Project, compilation: KotlinCompilation, kotlinPluginVersion: String): KotlinSourceSetProcessor<*>

    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)

        val target = (project.kotlinExtension as KotlinSingleJavaTargetExtension).target

        configureTarget(
            target,
            { compilation -> buildSourceSetProcessor(project, compilation, kotlinPluginVersion) }
        )

        configureAttributes(target)
        configureProjectGlobalSettings(project, kotlinPluginVersion)
        registry.register(KotlinModelBuilder(kotlinPluginVersion))
    }

    companion object {
        fun configureProjectGlobalSettings(project: Project, kotlinPluginVersion: String) {
            configureDefaultVersionsResolutionStrategy(project, kotlinPluginVersion)
            configureClassInspectionForIC(project)
    }

        fun configureTarget(
            target: KotlinWithJavaTarget,
            buildSourceSetProcessor: (KotlinCompilation) -> KotlinSourceSetProcessor<*>
        ) {
            setUpJavaSourceSets(target)
            configureSourceSetDefaults(target, buildSourceSetProcessor)
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
            inspectTask.sourceSetName = SourceSet.MAIN_SOURCE_SET_NAME
            inspectTask.jarTask = jarTask
            inspectTask.dependsOn(classesTask)
            jarTask.dependsOn(inspectTask)
        }

        private fun setUpJavaSourceSets(
            kotlinTarget: KotlinWithJavaTarget
        ) {
            val project = kotlinTarget.project
            val javaSourceSets = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets

            val kotlinSourceSetDslName = when (kotlinTarget.platformType) {
                KotlinPlatformType.js -> KOTLIN_JS_DSL_NAME
                else -> KOTLIN_DSL_NAME
            }

            // Workaround for indirect mutual recursion between the two `all { ... }` handlers:
            val compilationsUnderConstruction = mutableMapOf<String, KotlinWithJavaCompilation>()

            javaSourceSets.all { javaSourceSet ->
                val kotlinCompilation = compilationsUnderConstruction[javaSourceSet.name] ?: kotlinTarget.compilations.maybeCreate(javaSourceSet.name)
                kotlinCompilation.javaSourceSet = javaSourceSet
                val kotlinSourceSet = project.kotlinExtension.sourceSets.maybeCreate(kotlinCompilation.name)
                javaSourceSet.addConvention(kotlinSourceSetDslName, kotlinSourceSet)
                kotlinSourceSet.kotlin.source(javaSourceSet.java)
                kotlinCompilation.source(kotlinSourceSet)
            }

            kotlinTarget.compilations.all { kotlinCompilation ->
                val sourceSetName = kotlinCompilation.name
                compilationsUnderConstruction[sourceSetName] = kotlinCompilation
                kotlinCompilation.javaSourceSet = javaSourceSets.maybeCreate(sourceSetName)

                // Another Kotlin source set following the other convention, named according to the compilation, not the Java source set:
                val kotlinSourceSet = project.kotlinExtension.sourceSets.maybeCreate(kotlinCompilation.defaultSourceSetName)
                kotlinCompilation.source(kotlinSourceSet)
            }
        }

        private fun configureAttributes(
            kotlinTarget: KotlinWithJavaTarget
        ) {
            val project = kotlinTarget.project

            // Setup the consuming configurations:
            project.dependencies.attributesSchema.attribute(KotlinPlatformType.attribute)
            kotlinTarget.compilations.all { compilation ->
                AbstractKotlinTargetConfigurator.defineConfigurationsForCompilation(compilation, kotlinTarget, project.configurations)
            }

            // Setup the published configurations:
            // Don't set the attributes for common module; otherwise their 'common' platform won't be compatible with the one in
            // platform-specific modules
            if (kotlinTarget.platformType != KotlinPlatformType.common) {
                project.configurations.getByName(kotlinTarget.apiElementsConfigurationName).usesPlatformOf(kotlinTarget)
                project.configurations.getByName(kotlinTarget.runtimeElementsConfigurationName).usesPlatformOf(kotlinTarget)
            }
        }

        private fun configureSourceSetDefaults(
            kotlinTarget: KotlinTarget,
            buildSourceSetProcessor: (KotlinCompilation) -> KotlinSourceSetProcessor<*>
        ) {
            kotlinTarget.compilations.all { compilation ->
                buildSourceSetProcessor(compilation).run()
            }
        }
    }
}

internal fun configureDefaultVersionsResolutionStrategy(project: Project, kotlinPluginVersion: String) {
    project.configurations.all { configuration ->
        if (isGradleVersionAtLeast(4, 4)) {
            // Use the API introduced in Gradle 4.4 to modify the dependencies directly before they are resolved:
            configuration.withDependencies { dependencySet ->
                dependencySet.filterIsInstance<ExternalDependency>()
                    .filter { it.group == "org.jetbrains.kotlin" && it.version.isNullOrEmpty() }
                    .forEach { it.version { constraint -> constraint.prefer(kotlinPluginVersion) } }
            }
        } else {
            configuration.resolutionStrategy.eachDependency { details ->
                val requested = details.requested
                if (requested.group == "org.jetbrains.kotlin" && requested.version.isEmpty()) {
                    details.useVersion(kotlinPluginVersion)
                }
            }
        }
    }
}

internal open class KotlinPlugin(
    kotlinPluginVersion: String,
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPlugin(KotlinTasksProvider(targetName), kotlinPluginVersion, registry) {

    companion object {
        private const val targetName = "" // use empty suffix for the task names
    }

    override fun buildSourceSetProcessor(project: Project, compilation: KotlinCompilation, kotlinPluginVersion: String) =
            Kotlin2JvmSourceSetProcessor(project, tasksProvider, compilation, kotlinPluginVersion)

    override fun apply(project: Project) {
        val target = KotlinWithJavaTarget(project, KotlinPlatformType.jvm, targetName).apply {
            disambiguationClassifier = null // don't add anything to the task names
        }
        (project.kotlinExtension as KotlinSingleJavaTargetExtension).target = target

        project.pluginManager.apply(ScriptingGradleSubplugin::class.java)
        super.apply(project)
    }
}

internal open class KotlinCommonPlugin(
    kotlinPluginVersion: String,
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPlugin(KotlinTasksProvider(targetName), kotlinPluginVersion, registry) {

    companion object {
        private const val targetName = "common"
    }

    override fun buildSourceSetProcessor(
        project: Project,
        compilation: KotlinCompilation,
        kotlinPluginVersion: String
    ): KotlinSourceSetProcessor<*> =
        KotlinCommonSourceSetProcessor(project, compilation, tasksProvider, kotlinPluginVersion)

    override fun apply(project: Project) {
        val target = KotlinWithJavaTarget(project, KotlinPlatformType.common, targetName)
        (project.kotlinExtension as KotlinSingleJavaTargetExtension).target = target

        super.apply(project)
    }
}

internal open class Kotlin2JsPlugin(
    kotlinPluginVersion: String,
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPlugin(KotlinTasksProvider(targetName), kotlinPluginVersion, registry) {

    companion object {
        private const val targetName = "2Js"
    }

    override fun buildSourceSetProcessor(
        project: Project,
        compilation: KotlinCompilation,
        kotlinPluginVersion: String
    ): KotlinSourceSetProcessor<*> =
        Kotlin2JsSourceSetProcessor(
            project, tasksProvider, compilation, kotlinPluginVersion
        )

    override fun apply(project: Project) {
        val target = KotlinWithJavaTarget(project, KotlinPlatformType.js, targetName)

        (project.kotlinExtension as KotlinSingleJavaTargetExtension).target = target
        super.apply(project)
    }
}

internal open class KotlinAndroidPlugin(
    private val kotlinPluginVersion: String
) : Plugin<Project> {

    override fun apply(project: Project) {
        val androidTarget = KotlinAndroidTarget("", project)
        val tasksProvider = AndroidTasksProvider(androidTarget.targetName)

        applyToTarget(
            project, androidTarget, tasksProvider,
            kotlinPluginVersion
        )
    }

    companion object {
        fun applyToTarget(
            project: Project,
            kotlinTarget: KotlinAndroidTarget,
            tasksProvider: KotlinTasksProvider,
            kotlinPluginVersion: String
        ) {

            val version = loadAndroidPluginVersion()
            if (version != null) {
                val minimalVersion = "1.1.0"
                if (compareVersionNumbers(version, minimalVersion) < 0) {
                    throw IllegalStateException("Kotlin: Unsupported version of com.android.tools.build:gradle plugin: version $minimalVersion or higher should be used with kotlin-android plugin")
                }
            }

            val kotlinTools = KotlinConfigurationTools(
                tasksProvider,
                kotlinPluginVersion
            )

            val legacyVersionThreshold = "2.5.0"

            val variantProcessor = if (compareVersionNumbers(version, legacyVersionThreshold) < 0) {
                LegacyAndroidAndroidProjectHandler(kotlinTools)
            } else {
                val android25ProjectHandlerClass = Class.forName("org.jetbrains.kotlin.gradle.plugin.Android25ProjectHandler")
                val ctor = android25ProjectHandlerClass.constructors.single {
                    it.parameterTypes.contentEquals(arrayOf(kotlinTools.javaClass))
                }
                ctor.newInstance(kotlinTools) as AbstractAndroidProjectHandler<*>
            }

            variantProcessor.handleProject(project, kotlinTarget)
        }
    }
}

class KotlinConfigurationTools internal constructor(
    val kotlinTasksProvider: KotlinTasksProvider,
    val kotlinPluginVersion: String
)

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

    protected open fun setUpDependencyResolution(variant: V, compilation: KotlinJvmAndroidCompilation) = Unit

    protected abstract fun wireKotlinTasks(
        project: Project,
        compilation: KotlinJvmAndroidCompilation,
        androidPlugin: BasePlugin,
        androidExt: BaseExtension,
        variantData: V,
        javaTask: AbstractCompile,
        kotlinTask: KotlinCompile
    )

    protected abstract fun wrapVariantDataForKapt(variantData: V): KaptVariantData<V>

    fun handleProject(project: Project, kotlinAndroidTarget: KotlinAndroidTarget) {
        val ext = project.extensions.getByName("android") as BaseExtension

        ext.sourceSets.all { sourceSet ->
            logger.kotlinDebug("Creating KotlinBaseSourceSet for source set $sourceSet")
            val kotlinSourceSet = project.kotlinExtension.sourceSets.maybeCreate(
                lowerCamelCaseName(kotlinAndroidTarget.disambiguationClassifier, sourceSet.name)
            ).apply {
                kotlin.srcDir(project.file(project.file("src/${sourceSet.name}/kotlin")))
                kotlin.srcDirs(sourceSet.java.srcDirs)
            }
            sourceSet.addConvention(KOTLIN_DSL_NAME, kotlinSourceSet)
            Kapt3KotlinGradleSubplugin.createAptConfigurationIfNeeded(project, sourceSet.name)
        }

        val kotlinOptions = KotlinJvmOptionsImpl()
        kotlinOptions.noJdk = true
        ext.addExtension(KOTLIN_OPTIONS_DSL_NAME, kotlinOptions)

        project.afterEvaluate { project ->
            forEachVariant(project) { variant ->
                val variantName = getVariantName(variant)
                val compilation = kotlinAndroidTarget.compilations.create(variantName)
                setUpDependencyResolution(variant, compilation)
            }

            val androidPluginIds = listOf("android", "com.android.application", "android-library", "com.android.library",
                    "com.android.test", "com.android.feature", "com.android.dynamic-feature", "com.android.instantapp")
            val plugin = androidPluginIds.asSequence()
                                 .mapNotNull { project.plugins.findPlugin(it) as? BasePlugin }
                                 .firstOrNull()
                         ?: throw InvalidPluginException("'kotlin-android' expects one of the Android Gradle " +
                                                         "plugins to be applied to the project:\n\t" +
                                                         androidPluginIds.joinToString("\n\t") { "* $it" })

            checkAndroidAnnotationProcessorDependencyUsage(project)

            forEachVariant(project) {
                processVariant(
                    it, kotlinAndroidTarget, project, ext, plugin, kotlinOptions, kotlinConfigurationTools.kotlinTasksProvider
                )
            }

            val subpluginEnvironment = SubpluginEnvironment.loadSubplugins(project, kotlinConfigurationTools.kotlinPluginVersion)

            forEachVariant(project) { variant ->
                val compilation = kotlinAndroidTarget.compilations.getByName(getVariantName(variant))
                applySubplugins(project, compilation, variant, subpluginEnvironment)
            }
        }
    }

    private fun processVariant(
        variantData: V,
        target: KotlinAndroidTarget,
        project: Project,
        androidExt: BaseExtension,
        androidPlugin: BasePlugin,
        rootKotlinOptions: KotlinJvmOptionsImpl,
        tasksProvider: KotlinTasksProvider
    ) {
        checkVariantIsValid(variantData)
        val variantDataName = getVariantName(variantData)
        logger.kotlinDebug("Process variant [$variantDataName]")

        val javaTask = getJavaTask(variantData)

        if (javaTask == null) {
            logger.info("KOTLIN: javaTask is missing for $variantDataName, so Kotlin files won't be compiled for it")
            return
        }

        val compilation = target.compilations.getByName(variantDataName)
        val defaultSourceSet = project.kotlinExtension.sourceSets.maybeCreate(compilation.defaultSourceSetName)

        val kotlinTaskName = compilation.compileKotlinTaskName
        // todo: Investigate possibility of creating and configuring kotlinTask before evaluation
        val kotlinTask = tasksProvider.createKotlinJVMTask(project, kotlinTaskName, compilation)
        kotlinTask.parentKotlinOptionsImpl = rootKotlinOptions

        // store kotlin classes in separate directory. They will serve as class-path to java compiler
        kotlinTask.destinationDir = File(project.buildDir, "tmp/kotlin-classes/$variantDataName")
        kotlinTask.description = "Compiles the $variantDataName kotlin."

        // Register the source only after the task is created, because tne task is required for that:
        compilation.source(defaultSourceSet)
        configureSources(kotlinTask, variantData, compilation)

        // In MPPs, add the common main Kotlin sources to non-test variants, the common test sources to test variants
        val commonSourceSetName = if (getTestedVariantData(variantData) == null)
            KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME else
            KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME
        project.kotlinExtension.sourceSets.findByName(commonSourceSetName)?.let {
            compilation.source(it)
        }

        wireKotlinTasks(project, compilation, androidPlugin, androidExt, variantData, javaTask, kotlinTask)
    }

    private fun applySubplugins(
        project: Project,
        compilation: KotlinCompilation,
        variantData: V,
        subpluginEnvironment: SubpluginEnvironment
    ) {
        val kotlinTask = project.tasks.getByName(compilation.compileKotlinTaskName) as KotlinCompile
        val javaTask = getJavaTask(variantData)

        val appliedPlugins = subpluginEnvironment.addSubpluginOptions(
                project, kotlinTask, javaTask, wrapVariantDataForKapt(variantData), this, null)

        appliedPlugins.flatMap { it.getSubpluginKotlinTasks(project, kotlinTask) }
                .forEach { configureSources(it, variantData, null) }
    }

    private fun configureSources(compileTask: AbstractCompile, variantData: V, compilation: KotlinCompilation?) {
        val logger = compileTask.project.logger

        for (provider in getSourceProviders(variantData)) {
            val kotlinSourceSet = provider.getConvention(KOTLIN_DSL_NAME) as? KotlinSourceSet ?: continue
            if (compilation != null) {
                compilation.source(kotlinSourceSet)
            } else {
                compileTask.source(kotlinSourceSet.kotlin)
            }
        }

        for (javaSrcDir in getAllJavaSources(variantData)) {
            compileTask.source(javaSrcDir)
            logger.kotlinDebug("Source directory $javaSrcDir was added to kotlin source for ${compileTask.name}")
        }
    }
}

internal fun configureJavaTask(kotlinTask: KotlinCompile, javaTask: AbstractCompile, logger: Logger) {
    kotlinTask.javaOutputDir = javaTask.destinationDir

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

internal fun Task.registerSubpluginOptionsAsInputs(subpluginId: String, subpluginOptions: List<SubpluginOption>) {
    // There might be several options with the same key. We group them together
    // and add an index to the Gradle input property name to resolve possible duplication:
    val pluginOptionsGrouped = subpluginOptions.groupBy { it.key }
    for ((optionKey, optionsGroup) in pluginOptionsGrouped) {
        optionsGroup.forEachIndexed { index, option ->
            val indexSuffix = if (optionsGroup.size > 1) ".$index" else ""
            when (option) {
                is InternalSubpluginOption -> Unit

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