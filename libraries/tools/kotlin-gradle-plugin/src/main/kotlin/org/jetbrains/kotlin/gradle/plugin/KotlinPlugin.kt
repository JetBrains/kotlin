package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.SourceProvider
import groovy.lang.Closure
import org.gradle.api.*
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.MutableVersionConstraint
import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer
import org.gradle.api.artifacts.maven.MavenResolver
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.InvalidPluginException
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.MavenPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.CompileClasspathNormalizer
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.Kapt3KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.internal.KaptVariantData
import org.jetbrains.kotlin.gradle.internal.checkAndroidAnnotationProcessorDependencyUsage
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.logging.kotlinWarn
import org.jetbrains.kotlin.gradle.model.builder.KotlinModelBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.*
import java.io.File
import java.net.URL
import java.util.concurrent.Callable
import java.util.jar.Manifest

const val PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerPluginClasspath"
const val NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kotlinNativeCompilerPluginClasspath"
internal const val COMPILER_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerClasspath"
val KOTLIN_DSL_NAME = "kotlin"
val KOTLIN_JS_DSL_NAME = "kotlin2js"
val KOTLIN_OPTIONS_DSL_NAME = "kotlinOptions"

internal abstract class KotlinSourceSetProcessor<T : AbstractKotlinCompile<*>>(
    val project: Project,
    val tasksProvider: KotlinTasksProvider,
    val taskDescription: String,
    val kotlinCompilation: KotlinCompilation<*>
) {
    protected abstract fun doTargetSpecificProcessing()
    protected val logger = Logging.getLogger(this.javaClass)!!

    protected val sourceSetName: String = kotlinCompilation.compilationName

    protected val kotlinTask: TaskHolder<out T> = registerKotlinCompileTask()

    protected val javaSourceSet: SourceSet? = (kotlinCompilation as? KotlinWithJavaCompilation<*>)?.javaSourceSet

    private val defaultKotlinDestinationDir: File
        get() {
            val kotlinExt = project.kotlinExtension
            val targetSubDirectory =
                if (kotlinExt is KotlinSingleJavaTargetExtension)
                    "" // In single-target projects, don't add the target name part to this path
                else
                    kotlinCompilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
            return File(project.buildDir, "classes/kotlin/$targetSubDirectory${kotlinCompilation.compilationName}")
        }

    private fun registerKotlinCompileTask(): TaskHolder<out T> {
        val name = kotlinCompilation.compileKotlinTaskName
        logger.kotlinDebug("Creating kotlin compile task $name")
        val kotlinCompile = doRegisterTask(project, name) {
            it.description = taskDescription
            it.mapClasspath { kotlinCompilation.compileDependencyFiles }
            it.setDestinationDir { defaultKotlinDestinationDir }
            kotlinCompilation.output.addClassesDir { project.files(kotlinTask.doGetTask().destinationDir).builtBy(kotlinTask.doGetTask()) }
        }
        return kotlinCompile
    }

    open fun run() {
        addKotlinDirectoriesToJavaSourceSet()
        doTargetSpecificProcessing()

        if (kotlinCompilation is KotlinWithJavaCompilation<*>) {
            createAdditionalClassesTaskForIdeRunner()
        }
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

    private fun createAdditionalClassesTaskForIdeRunner() {
        open class IDEClassesTask : DefaultTask()
        // Workaround: as per KT-26641, when there's a Kotlin compilation with a Java source set, we create another task
        // that has a name composed as '<IDE module name>Classes`, where the IDE module name is the default source set name:
        val expectedClassesTaskName = "${kotlinCompilation.defaultSourceSetName}Classes"
        project.tasks.run {
            var shouldCreateTask = false
            if (useLazyTaskConfiguration) {
                try {
                    named(expectedClassesTaskName)
                } catch (e: Exception) {
                    shouldCreateTask = true
                }
            } else {
                shouldCreateTask = findByName(expectedClassesTaskName) == null
            }
            if (shouldCreateTask) {
                registerTask(project, expectedClassesTaskName, IDEClassesTask::class.java) {
                    it.dependsOn(getByName(kotlinCompilation.compileAllTaskName))
                }
            }
        }
    }

    protected abstract fun doRegisterTask(project: Project, taskName: String, configureAction: (T) -> (Unit)): TaskHolder<out T>
}

internal class Kotlin2JvmSourceSetProcessor(
    project: Project,
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: KotlinCompilation<*>,
    private val kotlinPluginVersion: String
) : KotlinSourceSetProcessor<KotlinCompile>(
    project, tasksProvider, "Compiles the $kotlinCompilation.", kotlinCompilation
) {
    override fun doRegisterTask(project: Project, taskName: String, configureAction: (KotlinCompile)->(Unit)): TaskHolder<out KotlinCompile> =
        tasksProvider.registerKotlinJVMTask(project, taskName, kotlinCompilation, configureAction)

    override fun doTargetSpecificProcessing() {
        ifKaptEnabled(project) {
            Kapt3KotlinGradleSubplugin.createAptConfigurationIfNeeded(project, kotlinCompilation.compilationName)
        }

        ScriptingGradleSubplugin.configureForSourceSet(project, kotlinCompilation.compilationName)

        project.runOnceAfterEvaluated("Kotlin2JvmSourceSetProcessor.doTargetSpecificProcessing", kotlinTask) {
            val kotlinTaskInstance = kotlinTask.doGetTask()
            val javaTask = javaSourceSet?.let { project.tasks.findByName(it.compileJavaTaskName) as JavaCompile }

            val subpluginEnvironment = SubpluginEnvironment.loadSubplugins(project, kotlinPluginVersion)
            val appliedPlugins = subpluginEnvironment.addSubpluginOptions(
                project, kotlinTaskInstance, javaTask, null, null, kotlinCompilation
            )

            appliedPlugins
                .flatMap { it.getSubpluginKotlinTasks(project, kotlinTaskInstance) }
                .forEach { plugin -> kotlinCompilation.allKotlinSourceSets.forEach { sourceSet -> plugin.source(sourceSet.kotlin) } }

            javaTask?.let { configureJavaTask(kotlinTaskInstance, it, logger) }

            if (project.pluginManager.hasPlugin("java-library") && sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME) {
                registerKotlinOutputForJavaLibrary(kotlinTaskInstance.destinationDir, kotlinTaskInstance)
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

internal fun KotlinCompilationOutput.addClassesDir(classesDirProvider: () -> FileCollection) {
    classesDirs.from(Callable { classesDirProvider() })
}

internal class Kotlin2JsSourceSetProcessor(
    project: Project,
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: KotlinCompilation<*>,
    private val kotlinPluginVersion: String
) : KotlinSourceSetProcessor<Kotlin2JsCompile>(
    project, tasksProvider, taskDescription = "Compiles the Kotlin sources in $kotlinCompilation to JavaScript.",
    kotlinCompilation = kotlinCompilation
) {
    override fun doRegisterTask(
        project: Project,
        taskName: String,
        configureAction: (Kotlin2JsCompile) -> (Unit)
    ): TaskHolder<out Kotlin2JsCompile> =
        tasksProvider.registerKotlinJSTask(project, taskName, kotlinCompilation, configureAction)

    override fun doTargetSpecificProcessing() {
        project.tasks.findByName(kotlinCompilation.compileAllTaskName)!!.dependsOn(kotlinTask.getTaskOrProvider())

        registerCleanSourceMapTask()

        if (kotlinCompilation is KotlinWithJavaCompilation<*>) {
            kotlinCompilation.javaSourceSet.clearJavaSrcDirs()
        }

        // outputFile can be set later during the configuration phase, get it only after the phase:
        project.runOnceAfterEvaluated("Kotlin2JsSourceSetProcessor.doTargetSpecificProcessing", kotlinTask) {
            val kotlinTaskInstance = kotlinTask.doGetTask()
            kotlinTaskInstance.kotlinOptions.outputFile = kotlinTaskInstance.outputFile.absolutePath
            val outputDir = kotlinTaskInstance.outputFile.parentFile

            val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project, kotlinPluginVersion)
            val appliedPlugins = subpluginEnvironment.addSubpluginOptions(
                project, kotlinTaskInstance, null, null, null, kotlinCompilation
            )

            if (outputDir.isParentOf(project.rootDir))
                throw InvalidUserDataException(
                    "The output directory '$outputDir' (defined by outputFile of $kotlinTaskInstance) contains or " +
                            "matches the project root directory '${project.rootDir}'.\n" +
                            "Gradle will not be able to build the project because of the root directory lock.\n" +
                            "To fix this, consider using the default outputFile location instead of providing it explicitly."
                )

            kotlinTaskInstance.destinationDir = outputDir

            appliedPlugins
                .flatMap { it.getSubpluginKotlinTasks(project, kotlinTaskInstance) }
                .forEach { task -> kotlinCompilation.allKotlinSourceSets.forEach { sourceSet -> task.source(sourceSet.kotlin) } }
        }
    }

    private fun registerCleanSourceMapTask() {
        val taskName = kotlinCompilation.composeName("clean", "sourceMap")
        registerTask(project, taskName, Delete::class.java) {
            it.onlyIf { kotlinTask.doGetTask().kotlinOptions.sourceMap }
            it.delete(object : Closure<String>(this) {
                override fun call(): String? = (kotlinTask.doGetTask().property("outputFile") as File).canonicalPath + ".map"
            })
        }
        project.tasks.findByName("clean")?.dependsOn(taskName)
    }
}

internal class KotlinCommonSourceSetProcessor(
    project: Project,
    compilation: KotlinCompilation<*>,
    tasksProvider: KotlinTasksProvider,
    private val kotlinPluginVersion: String
) : KotlinSourceSetProcessor<KotlinCompileCommon>(
    project, tasksProvider, taskDescription = "Compiles the kotlin sources in $compilation to Metadata.",
    kotlinCompilation = compilation
) {
    override fun doTargetSpecificProcessing() {
        project.tasks.findByName(kotlinCompilation.compileAllTaskName)!!.dependsOn(kotlinTask.getTaskOrProvider())
        // can be missing (e.g. in case of tests)
        if (kotlinCompilation.compilationName == KotlinCompilation.MAIN_COMPILATION_NAME) {
            project.tasks.findByName(kotlinCompilation.target.artifactsTaskName)?.dependsOn(kotlinTask.getTaskOrProvider())
        }

        project.runOnceAfterEvaluated("KotlinCommonSourceSetProcessor.doTargetSpecificProcessing", kotlinTask) {
            val kotlinTaskInstance = kotlinTask.doGetTask()
            val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project, kotlinPluginVersion)
            val appliedPlugins = subpluginEnvironment.addSubpluginOptions(
                project, kotlinTaskInstance, null, null, null, kotlinCompilation
            )
            appliedPlugins
                .flatMap { it.getSubpluginKotlinTasks(project, kotlinTaskInstance) }
                .forEach { kotlinCompilation.allKotlinSourceSets.forEach { sourceSet -> it.source(sourceSet.kotlin) } }
        }
    }

    // protected abstract fun doRegisterTask(project: Project, taskName: String, configureAction: (T) -> (Unit)): TaskHolder<out T>
    override fun doRegisterTask(project: Project, taskName: String, configureAction: (KotlinCompileCommon) -> (Unit)): TaskHolder<out KotlinCompileCommon> =
        tasksProvider.registerKotlinCommonTask(project, taskName, kotlinCompilation, configureAction)
}

internal abstract class AbstractKotlinPlugin(
    val tasksProvider: KotlinTasksProvider,
    protected val kotlinPluginVersion: String,
    val registry: ToolingModelBuilderRegistry
) : Plugin<Project> {

    internal abstract fun buildSourceSetProcessor(
        project: Project,
        compilation: KotlinCompilation<*>,
        kotlinPluginVersion: String
    ): KotlinSourceSetProcessor<*>

    override fun apply(project: Project) {
        project.plugins.apply(JavaPlugin::class.java)

        val target = (project.kotlinExtension as KotlinSingleJavaTargetExtension).target

        configureTarget(
            target,
            { compilation -> buildSourceSetProcessor(project, compilation, kotlinPluginVersion) }
        )

        applyUserDefinedAttributes(target)

        rewriteMppDependenciesInPom(target)

        configureProjectGlobalSettings(project, kotlinPluginVersion)
        registry.register(KotlinModelBuilder(kotlinPluginVersion, null))

        project.components.addAll(target.components)
    }

    private fun rewriteMppDependenciesInPom(target: AbstractKotlinTarget) {
        val project = target.project

        fun shouldRewritePoms(): Boolean =
            PropertiesProvider(project).keepMppDependenciesIntactInPoms != true

        project.pluginManager.withPlugin("maven-publish") {
            project.extensions.configure(PublishingExtension::class.java) { publishing ->
                publishing.publications.withType(MavenPublication::class.java).all { publication ->
                    publication.pom.withXml { xml ->
                        if (shouldRewritePoms())
                            project.rewritePomMppDependenciesToActualTargetModules(xml, target.kotlinComponents.single())
                    }
                }
            }
        }

        project.pluginManager.withPlugin("maven") {
            project.tasks.withType(Upload::class.java).all { uploadTask ->
                uploadTask.repositories.withType(MavenResolver::class.java).all { mavenResolver ->
                    mavenResolver.pom.withXml { xml ->
                        if (shouldRewritePoms())
                            project.rewritePomMppDependenciesToActualTargetModules(xml, target.kotlinComponents.single())
                    }
                }
            }

            // Setup conf2ScopeMappings so that the API dependencies are wriiten with the compile scope in the POMs in case of 'java' plugin
            project.convention.getPlugin(MavenPluginConvention::class.java)
                .conf2ScopeMappings.addMapping(0, project.configurations.getByName("api"), Conf2ScopeMappingContainer.COMPILE)
        }
    }

    companion object {
        fun configureProjectGlobalSettings(project: Project, kotlinPluginVersion: String) {
            configureDefaultVersionsResolutionStrategy(project, kotlinPluginVersion)
            configureClassInspectionForIC(project)
        }

        fun configureTarget(
            target: KotlinWithJavaTarget<*>,
            buildSourceSetProcessor: (KotlinCompilation<*>) -> KotlinSourceSetProcessor<*>
        ) {
            setUpJavaSourceSets(target)
            configureSourceSetDefaults(target, buildSourceSetProcessor)
            configureAttributes(target)
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
            val inspectTask = registerTask(project, "inspectClassesForKotlinIC", InspectClassesForMultiModuleIC::class.java) {
                it.sourceSetName = SourceSet.MAIN_SOURCE_SET_NAME
                it.jarTask = jarTask
                it.dependsOn(classesTask)
            }
            jarTask.dependsOn(inspectTask.getTaskOrProvider())
        }

        internal fun setUpJavaSourceSets(
            kotlinTarget: KotlinTarget,
            duplicateJavaSourceSetsAsKotlinSourceSets: Boolean = true
        ) {
            val project = kotlinTarget.project
            val javaSourceSets = project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets

            val kotlinSourceSetDslName = when (kotlinTarget.platformType) {
                KotlinPlatformType.js -> KOTLIN_JS_DSL_NAME
                else -> KOTLIN_DSL_NAME
            }

            // Workaround for indirect mutual recursion between the two `all { ... }` handlers:
            val compilationsUnderConstruction = mutableMapOf<String, KotlinCompilation<*>>()

            javaSourceSets.all { javaSourceSet ->
                val kotlinCompilation =
                    compilationsUnderConstruction[javaSourceSet.name] ?: kotlinTarget.compilations.maybeCreate(javaSourceSet.name)
                (kotlinCompilation as? KotlinWithJavaCompilation<*>)?.javaSourceSet = javaSourceSet

                if (duplicateJavaSourceSetsAsKotlinSourceSets) {
                    val kotlinSourceSet = project.kotlinExtension.sourceSets.maybeCreate(kotlinCompilation.name)
                    kotlinSourceSet.kotlin.source(javaSourceSet.java)
                    kotlinCompilation.source(kotlinSourceSet)
                    javaSourceSet.addConvention(kotlinSourceSetDslName, kotlinSourceSet)
                } else {
                    javaSourceSet.addConvention(kotlinSourceSetDslName, kotlinCompilation.defaultSourceSet)
                }
            }

            kotlinTarget.compilations.all { kotlinCompilation ->
                val sourceSetName = kotlinCompilation.name
                compilationsUnderConstruction[sourceSetName] = kotlinCompilation
                (kotlinCompilation as? KotlinWithJavaCompilation<*>)?.javaSourceSet = javaSourceSets.maybeCreate(sourceSetName)

                // Another Kotlin source set following the other convention, named according to the compilation, not the Java source set:
                val kotlinSourceSet = project.kotlinExtension.sourceSets.maybeCreate(kotlinCompilation.defaultSourceSetName)
                kotlinCompilation.source(kotlinSourceSet)
            }

            // Since the 'java' plugin (as opposed to 'java-library') doesn't known anything about the 'api' configurations,
            // add the API dependencies of the main compilation directly to the 'apiElements' configuration, so that the 'api' dependencies
            // are properly published with the 'compile' scope (KT-28355):
            project.whenEvaluated {
                project.configurations.apply {
                    val apiElementsConfiguration = getByName(kotlinTarget.apiElementsConfigurationName)
                    val mainCompilation = kotlinTarget.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
                    val compilationApiConfiguration = getByName(mainCompilation.apiConfigurationName)
                    apiElementsConfiguration.extendsFrom(compilationApiConfiguration)
                }
            }
        }

        private fun configureAttributes(
            kotlinTarget: KotlinWithJavaTarget<*>
        ) {
            val project = kotlinTarget.project

            // Setup the consuming configurations:
            project.dependencies.attributesSchema.attribute(KotlinPlatformType.attribute)
            kotlinTarget.compilations.all { compilation ->
                AbstractKotlinTargetConfigurator.defineConfigurationsForCompilation(compilation)
            }

            project.configurations.getByName("default").apply {
                setupAsLocalTargetSpecificConfigurationIfSupported(kotlinTarget)
            }

            // Setup the published configurations:
            // Don't set the attributes for common module; otherwise their 'common' platform won't be compatible with the one in
            // platform-specific modules
            if (kotlinTarget.platformType != KotlinPlatformType.common) {
                project.configurations.getByName(kotlinTarget.apiElementsConfigurationName).run {
                    attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(kotlinTarget))
                    setupAsPublicConfigurationIfSupported(kotlinTarget)
                    usesPlatformOf(kotlinTarget)
                }

                project.configurations.getByName(kotlinTarget.runtimeElementsConfigurationName).run {
                    attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerRuntimeUsage(kotlinTarget))
                    setupAsPublicConfigurationIfSupported(kotlinTarget)
                    usesPlatformOf(kotlinTarget)
                }
            }
        }

        private fun configureSourceSetDefaults(
            kotlinTarget: KotlinTarget,
            buildSourceSetProcessor: (KotlinCompilation<*>) -> KotlinSourceSetProcessor<*>
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
            fun MutableVersionConstraint.chooseVersion(version: String) {
                if (isGradleVersionAtLeast(5, 0)) {
                    // In Gradle 5.0, the semantics of 'prefer' has changed to be much less imperative, and now it's 'require' that we need:
                    val requireMethod = javaClass.getMethod("require", String::class.java)
                    requireMethod(this, version)
                } else {
                    prefer(version)
                }
            }

            // Use the API introduced in Gradle 4.4 to modify the dependencies directly before they are resolved:
            configuration.withDependencies { dependencySet ->
                dependencySet.filterIsInstance<ExternalDependency>()
                    .filter { it.group == "org.jetbrains.kotlin" && it.version.isNullOrEmpty() }
                    .forEach { it.version { constraint -> constraint.chooseVersion(kotlinPluginVersion) } }
            }
        } else {
            configuration.resolutionStrategy.eachDependency { details ->
                val requested = details.requested
                if (requested.group == "org.jetbrains.kotlin" && requested.version.isNullOrEmpty()) {
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

    override fun buildSourceSetProcessor(project: Project, compilation: KotlinCompilation<*>, kotlinPluginVersion: String) =
        Kotlin2JvmSourceSetProcessor(project, tasksProvider, compilation, kotlinPluginVersion)

    override fun apply(project: Project) {
        val target = KotlinWithJavaTarget<KotlinJvmOptions>(project, KotlinPlatformType.jvm, targetName).apply {
            disambiguationClassifier = null // don't add anything to the task names
        }
        (project.kotlinExtension as KotlinJvmProjectExtension).target = target

        super.apply(project)

        project.pluginManager.apply(ScriptingGradleSubplugin::class.java)
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
        compilation: KotlinCompilation<*>,
        kotlinPluginVersion: String
    ): KotlinSourceSetProcessor<*> =
        KotlinCommonSourceSetProcessor(project, compilation, tasksProvider, kotlinPluginVersion)

    override fun apply(project: Project) {
        val target = KotlinWithJavaTarget<KotlinMultiplatformCommonOptions>(project, KotlinPlatformType.common, targetName)
        (project.kotlinExtension as KotlinCommonProjectExtension).target = target

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
        compilation: KotlinCompilation<*>,
        kotlinPluginVersion: String
    ): KotlinSourceSetProcessor<*> =
        Kotlin2JsSourceSetProcessor(
            project, tasksProvider, compilation, kotlinPluginVersion
        )

    override fun apply(project: Project) {
        val target = KotlinWithJavaTarget<KotlinJsOptions>(project, KotlinPlatformType.js, targetName)

        (project.kotlinExtension as Kotlin2JsProjectExtension).target = target
        super.apply(project)
    }
}

internal open class KotlinAndroidPlugin(
    private val kotlinPluginVersion: String,
    private val registry: ToolingModelBuilderRegistry
) : Plugin<Project> {

    override fun apply(project: Project) {
        checkGradleCompatibility()

        val androidTarget = KotlinAndroidTarget("", project)
        (project.kotlinExtension as KotlinAndroidProjectExtension).target = androidTarget

        applyToTarget(kotlinPluginVersion, androidTarget)

        applyUserDefinedAttributes(androidTarget)

        registry.register(KotlinModelBuilder(kotlinPluginVersion, androidTarget))

        project.whenEvaluated { project.components.addAll(androidTarget.components) }
    }

    companion object {
        fun androidTargetHandler(
            kotlinPluginVersion: String,
            androidTarget: KotlinAndroidTarget
        ): AbstractAndroidProjectHandler<*> {
            val tasksProvider = AndroidTasksProvider(androidTarget.targetName)

            val version = loadAndroidPluginVersion()
            if (version != null) {
                val minimalVersion = "3.0.0"
                if (compareVersionNumbers(version, minimalVersion) < 0) {
                    throw IllegalStateException("Kotlin: Unsupported version of com.android.tools.build:gradle plugin: version $minimalVersion or higher should be used with kotlin-android plugin")
                }
            }

            val kotlinTools = KotlinConfigurationTools(
                tasksProvider,
                kotlinPluginVersion
            )

            return Android25ProjectHandler(kotlinTools)
        }

        fun applyToTarget(
            kotlinPluginVersion: String,
            kotlinTarget: KotlinAndroidTarget
        ) {
            androidTargetHandler(kotlinPluginVersion, kotlinTarget).configureTarget(kotlinTarget)
        }
    }
}

class KotlinConfigurationTools internal constructor(
    val kotlinTasksProvider: KotlinTasksProvider,
    val kotlinPluginVersion: String
)

abstract class AbstractAndroidProjectHandler<V>(private val kotlinConfigurationTools: KotlinConfigurationTools) {
    protected val logger = Logging.getLogger(this.javaClass)

    abstract fun forEachVariant(project: Project, action: (V) -> Unit): Unit
    abstract fun getTestedVariantData(variantData: V): V?
    abstract fun getResDirectories(variantData: V): FileCollection
    abstract fun getVariantName(variant: V): String
    abstract fun getFlavorNames(variant: V): List<String>
    abstract fun getBuildTypeName(variant: V): String
    abstract fun getLibraryOutputTask(variant: V): AbstractArchiveTask?

    protected abstract fun getSourceProviders(variantData: V): Iterable<SourceProvider>
    protected abstract fun getAllJavaSources(variantData: V): Iterable<File>
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

    fun configureTarget(kotlinAndroidTarget: KotlinAndroidTarget) {
        val project = kotlinAndroidTarget.project
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

            ifKaptEnabled(project) {
                Kapt3KotlinGradleSubplugin.createAptConfigurationIfNeeded(project, sourceSet.name)
            }
        }

        val kotlinOptions = KotlinJvmOptionsImpl()
        project.whenEvaluated {
            // TODO don't require the flag once there is an Android Gradle plugin build that supports desugaring of Long.hashCode and
            //  Boolean.hashCode. Instead, run conditionally, only with the AGP versions that play well with Kotlin bytecode for
            //  JVM target 1.8.
            //  See: KT-31027
            if (PropertiesProvider(project).setJvmTargetFromAndroidCompileOptions == true) {
                applyAndroidJavaVersion(project.extensions.getByType(BaseExtension::class.java), kotlinOptions)
            }
        }

        kotlinOptions.noJdk = true
        ext.addExtension(KOTLIN_OPTIONS_DSL_NAME, kotlinOptions)

        val androidPluginIds = listOf(
            "android", "com.android.application", "android-library", "com.android.library",
            "com.android.test", "com.android.feature", "com.android.dynamic-feature", "com.android.instantapp"
        )

        val plugin by lazy {
            androidPluginIds.asSequence()
                .mapNotNull { project.plugins.findPlugin(it) as? BasePlugin }
                .firstOrNull()
                ?: throw InvalidPluginException("'kotlin-android' expects one of the Android Gradle " +
                                                        "plugins to be applied to the project:\n\t" +
                                                        androidPluginIds.joinToString("\n\t") { "* $it" })
        }

        forEachVariant(project) { variant ->
            val variantName = getVariantName(variant)

            // Create the compilation and configure it first, then add to the compilations container. As this code is executed
            // in afterEvaluate, a user's build script might have already attached item handlers to the compilations container, and those
            // handlers might break when fired on a compilation that is not yet properly configured (e.g. KT-29964):
            kotlinAndroidTarget.compilationFactory.create(variantName).let { compilation ->
                compilation.androidVariant = variant as BaseVariant

                setUpDependencyResolution(variant, compilation)

                preprocessVariant(variant, compilation, project, ext, plugin, kotlinOptions, kotlinConfigurationTools.kotlinTasksProvider)

                @Suppress("UNCHECKED_CAST")
                (kotlinAndroidTarget.compilations as NamedDomainObjectCollection<in KotlinJvmAndroidCompilation>).add(compilation)
            }

        }

        project.whenEvaluated {
            forEachVariant(project) { variant ->
                val compilation = kotlinAndroidTarget.compilations.getByName(getVariantName(variant))
                postprocessVariant(variant, compilation, project, ext, plugin)

                val subpluginEnvironment = SubpluginEnvironment.loadSubplugins(project, kotlinConfigurationTools.kotlinPluginVersion)
                applySubplugins(project, compilation, variant, subpluginEnvironment)
            }
            checkAndroidAnnotationProcessorDependencyUsage(project)
        }
    }

    private fun applyAndroidJavaVersion(baseExtension: BaseExtension, kotlinOptions: KotlinJvmOptions) {
        val javaVersion =
            listOf(baseExtension.compileOptions.sourceCompatibility, baseExtension.compileOptions.targetCompatibility).min()!!
        if (javaVersion >= JavaVersion.VERSION_1_8)
            kotlinOptions.jvmTarget = "1.8"
    }

    private fun preprocessVariant(
        variantData: V,
        compilation: KotlinJvmAndroidCompilation,
        project: Project,
        androidExt: BaseExtension,
        androidPlugin: BasePlugin,
        rootKotlinOptions: KotlinJvmOptionsImpl,
        tasksProvider: KotlinTasksProvider
    ) {
        // This function is called before the variantData is completely filled by the Android plugin.
        // The fine details of variantData, such as AP options or Java sources, should not be trusted here.

        checkVariantIsValid(variantData)
        val variantDataName = getVariantName(variantData)
        logger.kotlinDebug("Process variant [$variantDataName]")

        val javaTask = getJavaTask(variantData)

        if (javaTask == null) {
            logger.info("KOTLIN: javaTask is missing for $variantDataName, so Kotlin files won't be compiled for it")
            return
        }

        val defaultSourceSet = project.kotlinExtension.sourceSets.maybeCreate(compilation.defaultSourceSetName)

        val kotlinTaskName = compilation.compileKotlinTaskName

        tasksProvider.registerKotlinJVMTask(project, kotlinTaskName, compilation) {
            it.parentKotlinOptionsImpl = rootKotlinOptions

            // store kotlin classes in separate directory. They will serve as class-path to java compiler
            it.destinationDir = File(project.buildDir, "tmp/kotlin-classes/$variantDataName")
            it.description = "Compiles the $variantDataName kotlin."
        }

        // Register the source only after the task is created, because the task is required for that:
        compilation.source(defaultSourceSet)

        // In MPPs, add the common main Kotlin sources to non-test variants, the common test sources to test variants
        val commonSourceSetName = if (getTestedVariantData(variantData) == null)
            KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME else
            KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME
        project.kotlinExtension.sourceSets.findByName(commonSourceSetName)?.let {
            compilation.source(it)
        }
    }

    private fun postprocessVariant(
        variantData: V,
        compilation: KotlinJvmAndroidCompilation,
        project: Project,
        androidExt: BaseExtension,
        androidPlugin: BasePlugin
    ) {
        val javaTask = getJavaTask(variantData) ?: return

        val kotlinTask = compilation.compileKotlinTask
        configureSources(kotlinTask, variantData, compilation)
        wireKotlinTasks(project, compilation, androidPlugin, androidExt, variantData, javaTask, kotlinTask)
    }

    private fun applySubplugins(
        project: Project,
        compilation: KotlinCompilation<*>,
        variantData: V,
        subpluginEnvironment: SubpluginEnvironment
    ) {
        val kotlinTask = project.tasks.getByName(compilation.compileKotlinTaskName) as KotlinCompile
        val javaTask = getJavaTask(variantData)

        val appliedPlugins = subpluginEnvironment.addSubpluginOptions(
            project, kotlinTask, javaTask, wrapVariantDataForKapt(variantData), this, null
        )

        appliedPlugins.flatMap { it.getSubpluginKotlinTasks(project, kotlinTask) }
            .forEach { configureSources(it, variantData, null) }
    }

    private fun configureSources(compileTask: AbstractCompile, variantData: V, compilation: KotlinCompilation<*>?) {
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
        } else {
            dirCompatible(kotlinTask.destinationDir)
        }
    }
    // Also, use kapt1 annotations file for up-to-date check since annotation processing is done with javac
    javaTask.dependsOn(kotlinTask)
    /*
     * It's important to modify javaTask.classpath only in doFirst,
     * because Android plugin uses ConventionMapping to modify it too (see JavaCompileConfigAction.execute),
     * and setting classpath explicitly prevents usage of Android mappings.
     * Also classpath set by Android can be modified after execution of some tasks (see VarianConfiguration.getCompileClasspath)
     * ex. it adds some support libraries jars after execution of prepareComAndroidSupportSupportV42311Library task,
     * so it's only safe to modify javaTask.classpath right before its usage
     */
    // todo: remove?
    javaTask.appendClasspathDynamically(kotlinTask.destinationDir!!)
}

private fun ifKaptEnabled(project: Project, block: () -> Unit) {
    var triggered = false

    fun trigger() {
        if (triggered) return
        triggered = true
        block()
    }

    project.pluginManager.withPlugin("kotlin-kapt") { trigger() }
    project.pluginManager.withPlugin("org.jetbrains.kotlin.kapt") { trigger() }
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
                    val subpluginIdWithWrapperKey = "$subpluginId.$optionKey$indexSuffix"
                    registerSubpluginOptionsAsInputs(subpluginIdWithWrapperKey, option.originalOptions)
                }

                is FilesSubpluginOption -> when (option.kind) {
                    FilesOptionKind.INTERNAL -> Unit
                }.run { /* exhaustive when */ }

                else -> {
                    inputsCompatible.propertyCompatible("$subpluginId." + option.key + indexSuffix, Callable { option.value })
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