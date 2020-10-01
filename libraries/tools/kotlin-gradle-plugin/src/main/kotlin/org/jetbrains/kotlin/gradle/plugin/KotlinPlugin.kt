/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.*
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.SourceKind
import org.gradle.api.*
import org.gradle.api.artifacts.maven.Conf2ScopeMappingContainer
import org.gradle.api.artifacts.maven.MavenResolver
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.InvalidPluginException
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.MavenPluginConvention
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.internal.checkAndroidAnnotationProcessorDependencyUsage
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.model.builder.KotlinModelBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.JsIrBinary
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrLink
import org.jetbrains.kotlin.gradle.targets.js.jsPluginDeprecationMessage
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
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
internal const val KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME = "kotlinKlibCommonizerClasspath"

val KOTLIN_DSL_NAME = "kotlin"
val KOTLIN_JS_DSL_NAME = "kotlin2js"
val KOTLIN_OPTIONS_DSL_NAME = "kotlinOptions"

abstract class KotlinCompilationProcessor<out T : SourceTask>(
    open val kotlinCompilation: KotlinCompilation<*>
) {
    abstract val kotlinTask: TaskProvider<out T>
    abstract fun run()

    protected val project: Project
        get() = kotlinCompilation.target.project

    protected val defaultKotlinDestinationDir: File
        get() {
            val kotlinExt = project.kotlinExtension
            val targetSubDirectory =
                if (kotlinExt is KotlinSingleJavaTargetExtension)
                    "" // In single-target projects, don't add the target name part to this path
                else
                    kotlinCompilation.target.disambiguationClassifier?.let { "$it/" }.orEmpty()
            return File(project.buildDir, "classes/kotlin/$targetSubDirectory${kotlinCompilation.compilationName}")
        }
}

internal abstract class KotlinSourceSetProcessor<T : AbstractKotlinCompile<*>>(
    val tasksProvider: KotlinTasksProvider,
    val taskDescription: String,
    kotlinCompilation: AbstractKotlinCompilation<*>
) : KotlinCompilationProcessor<T>(kotlinCompilation) {
    protected abstract fun doTargetSpecificProcessing()
    protected val logger = Logging.getLogger(this.javaClass)!!

    protected val sourceSetName: String = kotlinCompilation.compilationName

    override val kotlinTask: TaskProvider<out T> = prepareKotlinCompileTask()

    protected val javaSourceSet: SourceSet?
        get() =
            (kotlinCompilation as? KotlinWithJavaCompilation<*>)?.javaSourceSet
                ?: kotlinCompilation.target.let {
                    if (it is KotlinJvmTarget && it.withJavaEnabled)
                        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.maybeCreate(kotlinCompilation.name)
                    else null
                }

    private fun prepareKotlinCompileTask(): TaskProvider<out T> =
        registerKotlinCompileTask(register = ::doRegisterTask).also { task ->
            kotlinCompilation.output.addClassesDir { project.files(task.map { it.destinationDir }).builtBy(task) }
        }

    protected fun registerKotlinCompileTask(
        name: String = kotlinCompilation.compileKotlinTaskName,
        register: (Project, String, (T) -> Unit) -> TaskProvider<out T>
    ): TaskProvider<out T> {
        logger.kotlinDebug("Creating kotlin compile task $name")

        KotlinCompileTaskData.register(name, kotlinCompilation).apply {
            destinationDir.set(project.provider { defaultKotlinDestinationDir })
        }

        return register(project, name) {
            it.description = taskDescription
            it.mapClasspath { kotlinCompilation.compileDependencyFiles }
        }
    }

    override fun run() {
        addKotlinDirectoriesToJavaSourceSet()
        doTargetSpecificProcessing()

        if (kotlinCompilation is KotlinWithJavaCompilation<*>) {
            createAdditionalClassesTaskForIdeRunner()
        }
    }

    private fun addKotlinDirectoriesToJavaSourceSet() {
        val java = javaSourceSet ?: return

        // Try to avoid duplicate Java sources in allSource; run lazily to allow changing the directory set:
        val kotlinSrcDirsToAdd = Callable {
            kotlinCompilation.kotlinSourceSets.map { filterOutJavaSrcDirsIfPossible(it.kotlin) }
        }

        java.allJava.srcDirs(kotlinSrcDirsToAdd)
        java.allSource.srcDirs(kotlinSrcDirsToAdd)
    }

    private fun filterOutJavaSrcDirsIfPossible(sourceDirectorySet: SourceDirectorySet): FileCollection {
        val java = javaSourceSet ?: return sourceDirectorySet

        // If the API used below is not available, fall back to not filtering the Java sources.
        if (SourceDirectorySet::class.java.methods.none { it.name == "getSourceDirectories" }) {
            return sourceDirectorySet
        }

        fun getSourceDirectories(sourceDirectorySet: SourceDirectorySet): FileCollection {
            val method = SourceDirectorySet::class.java.getMethod("getSourceDirectories")
            return method(sourceDirectorySet) as FileCollection
        }

        // Build a lazily-resolved file collection that filters out Java sources from sources of this sourceDirectorySet
        return getSourceDirectories(sourceDirectorySet).minus(getSourceDirectories(java.java))
    }

    private fun createAdditionalClassesTaskForIdeRunner() {
        open class IDEClassesTask : DefaultTask()
        // Workaround: as per KT-26641, when there's a Kotlin compilation with a Java source set, we create another task
        // that has a name composed as '<IDE module name>Classes`, where the IDE module name is the default source set name:
        val expectedClassesTaskName = "${kotlinCompilation.defaultSourceSetName}Classes"
        project.tasks.run {
            var shouldCreateTask = false
            try {
                named(expectedClassesTaskName)
            } catch (e: UnknownDomainObjectException) {
                shouldCreateTask = true
            }
            if (shouldCreateTask) {
                project.registerTask(expectedClassesTaskName, IDEClassesTask::class.java) {
                    it.dependsOn(getByName(kotlinCompilation.compileAllTaskName))
                }
            }
        }
    }

    protected abstract fun doRegisterTask(project: Project, taskName: String, configureAction: (T) -> (Unit)): TaskProvider<out T>
}

internal class Kotlin2JvmSourceSetProcessor(
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: AbstractKotlinCompilation<*>,
    private val kotlinPluginVersion: String
) : KotlinSourceSetProcessor<KotlinCompile>(
    tasksProvider, "Compiles the $kotlinCompilation.", kotlinCompilation
) {
    override fun doRegisterTask(
        project: Project,
        taskName: String,
        configureAction: (KotlinCompile) -> (Unit)
    ): TaskProvider<out KotlinCompile> =
        tasksProvider.registerKotlinJVMTask(project, taskName, kotlinCompilation, configureAction)

    override fun doTargetSpecificProcessing() {
        ifKaptEnabled(project) {
            Kapt3GradleSubplugin.createAptConfigurationIfNeeded(project, kotlinCompilation.compilationName)
        }

        ScriptingGradleSubplugin.configureForSourceSet(project, kotlinCompilation.compilationName)

        project.whenEvaluated {
            val subpluginEnvironment = SubpluginEnvironment.loadSubplugins(project, kotlinPluginVersion)
            subpluginEnvironment.addSubpluginOptions(project, kotlinCompilation)

            javaSourceSet?.let { java ->
                val javaTask = project.tasks.withType<AbstractCompile>().named(java.compileJavaTaskName)
                configureJavaTask(kotlinTask, javaTask)
            }

            if (sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME) {
                project.pluginManager.withPlugin("java-library") {
                    registerKotlinOutputForJavaLibrary(kotlinTask.map { it.destinationDir }, kotlinTask)
                }
            }
        }
    }

    private fun registerKotlinOutputForJavaLibrary(outputDir: Provider<File>, taskDependency: TaskProvider<*>) {
        val configuration = project.configurations.getByName("apiElements")
        configuration.outgoing.variants.getByName("classes").artifact(outputDir) {
            it.builtBy(taskDependency)
            it.type = "java-classes-directory"
        }
    }
}

internal fun KotlinCompilationOutput.addClassesDir(classesDirProvider: () -> FileCollection) {
    classesDirs.from(Callable { classesDirProvider() })
}

internal class Kotlin2JsSourceSetProcessor(
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: AbstractKotlinCompilation<*>,
    private val kotlinPluginVersion: String
) : KotlinSourceSetProcessor<Kotlin2JsCompile>(
    tasksProvider,
    taskDescription = "Compiles the Kotlin sources in $kotlinCompilation to JavaScript.",
    kotlinCompilation = kotlinCompilation
) {
    override fun doRegisterTask(
        project: Project,
        taskName: String,
        configureAction: (Kotlin2JsCompile) -> (Unit)
    ): TaskProvider<out Kotlin2JsCompile> =
        tasksProvider.registerKotlinJSTask(project, taskName, kotlinCompilation, configureAction)

    override fun doTargetSpecificProcessing() {
        project.tasks.named(kotlinCompilation.compileAllTaskName).configure {
            it.dependsOn(kotlinTask)
        }

        if (kotlinCompilation is KotlinWithJavaCompilation<*>) {
            kotlinCompilation.javaSourceSet.clearJavaSrcDirs()
        }

        // outputFile can be set later during the configuration phase, get it only after the phase:
        project.whenEvaluated {
            kotlinTask.configure { kotlinTaskInstance ->
                kotlinTaskInstance.kotlinOptions.outputFile = kotlinTaskInstance.outputFile.absolutePath
                val outputDir = kotlinTaskInstance.outputFile.parentFile
                if (outputDir.isParentOf(project.rootDir))
                    throw InvalidUserDataException(
                        "The output directory '$outputDir' (defined by outputFile of $kotlinTaskInstance) contains or " +
                                "matches the project root directory '${project.rootDir}'.\n" +
                                "Gradle will not be able to build the project because of the root directory lock.\n" +
                                "To fix this, consider using the default outputFile location instead of providing it explicitly."
                    )
                kotlinTaskInstance.destinationDir = outputDir
            }

            val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project, kotlinPluginVersion)
            subpluginEnvironment.addSubpluginOptions(project, kotlinCompilation)
        }
    }
}

internal class KotlinJsIrSourceSetProcessor(
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: AbstractKotlinCompilation<*>,
    private val kotlinPluginVersion: String
) : KotlinSourceSetProcessor<Kotlin2JsCompile>(
    tasksProvider, taskDescription = "Compiles the Kotlin sources in $kotlinCompilation to JavaScript.",
    kotlinCompilation = kotlinCompilation
) {
    override fun doRegisterTask(
        project: Project,
        taskName: String,
        configureAction: (Kotlin2JsCompile) -> (Unit)
    ): TaskProvider<out Kotlin2JsCompile> =
        tasksProvider.registerKotlinJSTask(project, taskName, kotlinCompilation, configureAction)

    private fun registerJsLink(
        project: Project,
        taskName: String,
        mode: KotlinJsBinaryMode,
        configureAction: (Kotlin2JsCompile) -> Unit
    ): TaskProvider<out KotlinJsIrLink> {
        return tasksProvider.registerKotlinJsIrTask(
            project,
            taskName,
            kotlinCompilation
        ) { task ->
            task.mode = mode
            configureAction(task)
        }
    }

    override fun doTargetSpecificProcessing() {
        project.tasks.named(kotlinCompilation.compileAllTaskName).configure {
            it.dependsOn(kotlinTask)
        }

        val compilation = kotlinCompilation as KotlinJsIrCompilation

        compilation.binaries
            .withType(JsIrBinary::class.java)
            .all { binary ->
                registerKotlinCompileTask(
                    binary.linkTaskName
                ) { project, name, action ->
                    registerJsLink(project, name, binary.mode) { compileTask ->
                        action(compileTask)
                        compileTask.dependsOn(kotlinTask)
                    }
                }
            }

        project.whenEvaluated {
            val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project, kotlinPluginVersion)
            subpluginEnvironment.addSubpluginOptions(project, kotlinCompilation)
        }

        // outputFile can be set later during the configuration phase, get it only after the phase:
        project.runOnceAfterEvaluated("KotlinJsIrSourceSetProcessor.doTargetSpecificProcessing", kotlinTask) {
            val kotlinTaskInstance = kotlinTask.get()
            kotlinTaskInstance.kotlinOptions.outputFile = kotlinTaskInstance.outputFile.absolutePath
            val outputDir = kotlinTaskInstance.outputFile.parentFile

            if (outputDir.isParentOf(project.rootDir))
                throw InvalidUserDataException(
                    "The output directory '$outputDir' (defined by outputFile of $kotlinTaskInstance) contains or " +
                            "matches the project root directory '${project.rootDir}'.\n" +
                            "Gradle will not be able to build the project because of the root directory lock.\n" +
                            "To fix this, consider using the default outputFile location instead of providing it explicitly."
                )

            kotlinTaskInstance.destinationDir = outputDir
        }
    }
}

internal class KotlinCommonSourceSetProcessor(
    compilation: AbstractKotlinCompilation<*>,
    tasksProvider: KotlinTasksProvider,
    private val kotlinPluginVersion: String
) : KotlinSourceSetProcessor<KotlinCompileCommon>(
    tasksProvider, taskDescription = "Compiles the kotlin sources in $compilation to Metadata.", kotlinCompilation = compilation
) {
    override fun doTargetSpecificProcessing() {
        project.tasks.named(kotlinCompilation.compileAllTaskName).dependsOn(kotlinTask)
        // can be missing (e.g. in case of tests)
        if (kotlinCompilation.isMain()) {
            project.locateTask<Task>(kotlinCompilation.target.artifactsTaskName)?.dependsOn(kotlinTask)
        }

        project.whenEvaluated {
            val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project, kotlinPluginVersion)
            subpluginEnvironment.addSubpluginOptions(project, kotlinCompilation)
        }
    }

    // protected abstract fun doRegisterTask(project: Project, taskName: String, configureAction: (T) -> (Unit)): TaskHolder<out T>
    override fun doRegisterTask(
        project: Project,
        taskName: String,
        configureAction: (KotlinCompileCommon) -> (Unit)
    ): TaskProvider<out KotlinCompileCommon> =
        tasksProvider.registerKotlinCommonTask(project, taskName, kotlinCompilation, configureAction)
}

internal abstract class AbstractKotlinPlugin(
    val tasksProvider: KotlinTasksProvider,
    protected val kotlinPluginVersion: String,
    val registry: ToolingModelBuilderRegistry
) : Plugin<Project> {

    internal abstract fun buildSourceSetProcessor(
        project: Project,
        compilation: AbstractKotlinCompilation<*>,
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

        configureProjectGlobalSettings(project)
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
        fun configureProjectGlobalSettings(project: Project) {
            customizeKotlinDependencies(project)
            configureClassInspectionForIC(project)
            project.setupGeneralKotlinExtensionParameters()
        }

        fun configureTarget(
            target: KotlinWithJavaTarget<*>,
            buildSourceSetProcessor: (AbstractKotlinCompilation<*>) -> KotlinSourceSetProcessor<*>
        ) {
            setUpJavaSourceSets(target)
            configureSourceSetDefaults(target, buildSourceSetProcessor)
            configureAttributes(target)
        }

        private fun configureClassInspectionForIC(project: Project) {
            val classesTask = project.locateTask<Task>(JavaPlugin.CLASSES_TASK_NAME)
            val jarTask = project.locateTask<Jar>(JavaPlugin.JAR_TASK_NAME)

            if (classesTask == null || jarTask == null) {
                project.logger.info(
                    "Could not configure class inspection task " +
                            "(classes task = ${classesTask?.javaClass?.canonicalName}, " +
                            "jar task = ${classesTask?.javaClass?.canonicalName}"
                )
                return
            }
            val inspectTask =
                project.registerTask<InspectClassesForMultiModuleIC>("inspectClassesForKotlinIC") {
                    it.sourceSetName = SourceSet.MAIN_SOURCE_SET_NAME
                    it.archivePath.set(project.provider { jarTask.get().archivePathCompatible.canonicalPath })
                    it.archiveName.set(project.provider { jarTask.get().archiveFileName.get() })
                    it.dependsOn(classesTask)
                }
            jarTask.dependsOn(inspectTask)
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

            kotlinTarget.compilations.run {
                getByName(KotlinCompilation.TEST_COMPILATION_NAME).associateWith(getByName(KotlinCompilation.MAIN_COMPILATION_NAME))
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
            kotlinTarget: KotlinWithJavaTarget<*>,
            buildSourceSetProcessor: (AbstractKotlinCompilation<*>) -> KotlinSourceSetProcessor<*>
        ) {
            kotlinTarget.compilations.all { compilation ->
                buildSourceSetProcessor(compilation).run()
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

    override fun buildSourceSetProcessor(project: Project, compilation: AbstractKotlinCompilation<*>, kotlinPluginVersion: String) =
        Kotlin2JvmSourceSetProcessor(tasksProvider, compilation, kotlinPluginVersion)

    override fun apply(project: Project) {
        val target =
            KotlinWithJavaTarget<KotlinJvmOptions>(
                project,
                KotlinPlatformType.jvm,
                targetName,
                { KotlinJvmOptionsImpl() }
            ).apply {
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
        compilation: AbstractKotlinCompilation<*>,
        kotlinPluginVersion: String
    ): KotlinSourceSetProcessor<*> =
        KotlinCommonSourceSetProcessor(compilation, tasksProvider, kotlinPluginVersion)

    override fun apply(project: Project) {
        val target = KotlinWithJavaTarget<KotlinMultiplatformCommonOptions>(
            project,
            KotlinPlatformType.common,
            targetName,
            { KotlinMultiplatformCommonOptionsImpl() }
        )
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
        internal const val NOWARN_2JS_FLAG = "kotlin.2js.nowarn"
    }

    override fun buildSourceSetProcessor(
        project: Project,
        compilation: AbstractKotlinCompilation<*>,
        kotlinPluginVersion: String
    ): KotlinSourceSetProcessor<*> =
        Kotlin2JsSourceSetProcessor(
            tasksProvider, compilation, kotlinPluginVersion
        )

    override fun apply(project: Project) {
        if (!PropertiesProvider(project).noWarn2JsPlugin) {
            project.logger.warn(jsPluginDeprecationMessage("kotlin2js"))
        }
        val target = KotlinWithJavaTarget<KotlinJsOptions>(project, KotlinPlatformType.js, targetName, { KotlinJsOptionsImpl() })

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

        customizeKotlinDependencies(project)

        registry.register(KotlinModelBuilder(kotlinPluginVersion, androidTarget))

        project.whenEvaluated { project.components.addAll(androidTarget.components) }
    }

    companion object {
        fun androidTargetHandler(
            kotlinPluginVersion: String,
            androidTarget: KotlinAndroidTarget
        ): AbstractAndroidProjectHandler {
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

abstract class AbstractAndroidProjectHandler(private val kotlinConfigurationTools: KotlinConfigurationTools) {
    companion object {
        fun kotlinSourceSetNameForAndroidSourceSet(kotlinAndroidTarget: KotlinAndroidTarget, androidSourceSetName: String) =
            lowerCamelCaseName(kotlinAndroidTarget.disambiguationClassifier, androidSourceSetName)
    }

    protected val logger = Logging.getLogger(this.javaClass)

    abstract fun getFlavorNames(variant: BaseVariant): List<String>
    abstract fun getBuildTypeName(variant: BaseVariant): String
    abstract fun getLibraryOutputTask(variant: BaseVariant): Any?

    protected open fun checkVariantIsValid(variant: BaseVariant) = Unit

    protected open fun setUpDependencyResolution(variant: BaseVariant, compilation: KotlinJvmAndroidCompilation) = Unit

    protected abstract fun wireKotlinTasks(
        project: Project,
        compilation: KotlinJvmAndroidCompilation,
        androidPlugin: BasePlugin,
        androidExt: BaseExtension,
        variantData: BaseVariant,
        javaTask: TaskProvider<out AbstractCompile>,
        kotlinTask: TaskProvider<out KotlinCompile>
    )

    fun configureTarget(kotlinAndroidTarget: KotlinAndroidTarget) {
        val project = kotlinAndroidTarget.project
        val ext = project.extensions.getByName("android") as BaseExtension

        ext.sourceSets.all { sourceSet ->
            logger.kotlinDebug("Creating KotlinBaseSourceSet for source set $sourceSet")
            val kotlinSourceSet = project.kotlinExtension.sourceSets.maybeCreate(
                kotlinSourceSetNameForAndroidSourceSet(kotlinAndroidTarget, sourceSet.name)
            ).apply {
                createDefaultDependsOnEdges(kotlinAndroidTarget, sourceSet, this)
                kotlin.srcDir(project.file(project.file("src/${sourceSet.name}/kotlin")))
                kotlin.srcDirs(sourceSet.java.srcDirs)
            }
            sourceSet.addConvention(KOTLIN_DSL_NAME, kotlinSourceSet)

            ifKaptEnabled(project) {
                Kapt3GradleSubplugin.createAptConfigurationIfNeeded(project, sourceSet.name)
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

        project.forEachVariant { variant ->
            val variantName = getVariantName(variant)

            // Create the compilation and configure it first, then add to the compilations container. As this code is executed
            // in afterEvaluate, a user's build script might have already attached item handlers to the compilations container, and those
            // handlers might break when fired on a compilation that is not yet properly configured (e.g. KT-29964):
            kotlinAndroidTarget.compilationFactory.create(variantName).let { compilation ->
                compilation.androidVariant = variant

                setUpDependencyResolution(variant, compilation)

                preprocessVariant(variant, compilation, project, kotlinOptions, kotlinConfigurationTools.kotlinTasksProvider)

                @Suppress("UNCHECKED_CAST")
                (kotlinAndroidTarget.compilations as NamedDomainObjectCollection<in KotlinJvmAndroidCompilation>).add(compilation)
            }

        }

        project.whenEvaluated {
            forEachVariant { variant ->
                val compilation = kotlinAndroidTarget.compilations.getByName(getVariantName(variant))
                postprocessVariant(variant, compilation, project, ext, plugin)

                val subpluginEnvironment = SubpluginEnvironment.loadSubplugins(project, kotlinConfigurationTools.kotlinPluginVersion)
                subpluginEnvironment.addSubpluginOptions(project, compilation)
            }
            checkAndroidAnnotationProcessorDependencyUsage(project)

            addKotlinDependenciesToAndroidSourceSets(project, kotlinAndroidTarget)
        }
    }

    private fun createDefaultDependsOnEdges(
        kotlinAndroidTarget: KotlinAndroidTarget,
        androidSourceSet: AndroidSourceSet,
        kotlinSourceSet: KotlinSourceSet
    ) {
        val commonSourceSetName = when (androidSourceSet.name) {
            "main" -> "commonMain"
            "test" -> "commonTest"
            "androidTest" -> "commonTest"
            else -> return
        }
        val commonSourceSet = kotlinAndroidTarget.project.kotlinExtension.sourceSets.findByName(commonSourceSetName) ?: return
        kotlinSourceSet.dependsOn(commonSourceSet)
    }

    /**
     * The Android variants have their configurations extendsFrom relation set up in a way that only some of the configurations of the
     * variants propagate the dependencies from production variants to test ones. To make this dependency propagation work for the Kotlin
     * source set dependencies as well, we need to add them to the Android source sets' api/implementation-like configurations,
     * not just the classpath-like configurations of the variants.
     */
    private fun addKotlinDependenciesToAndroidSourceSets(
        project: Project,
        kotlinAndroidTarget: KotlinAndroidTarget
    ) {
        fun addDependenciesToAndroidSourceSet(
            androidSourceSet: AndroidSourceSet,
            apiConfigurationName: String,
            implementationConfigurationName: String,
            compileOnlyConfigurationName: String,
            runtimeOnlyConfigurationName: String
        ) {
            project.addExtendsFromRelation(androidSourceSet.apiConfigurationName, apiConfigurationName)
            project.addExtendsFromRelation(androidSourceSet.implementationConfigurationName, implementationConfigurationName)
            project.addExtendsFromRelation(androidSourceSet.compileOnlyConfigurationName, compileOnlyConfigurationName)
            project.addExtendsFromRelation(androidSourceSet.runtimeOnlyConfigurationName, runtimeOnlyConfigurationName)
        }

        /** First, just add the dependencies from Kotlin source sets created for the Android source sets,
         * see [org.jetbrains.kotlin.gradle.plugin.AbstractAndroidProjectHandler.configureTarget]
         */
        (project.extensions.getByName("android") as BaseExtension).sourceSets.forEach { androidSourceSet ->
            val kotlinSourceSetName = kotlinSourceSetNameForAndroidSourceSet(kotlinAndroidTarget, androidSourceSet.name)
            project.kotlinExtension.sourceSets.findByName(kotlinSourceSetName)?.let { kotlinSourceSet ->
                addDependenciesToAndroidSourceSet(
                    androidSourceSet,
                    kotlinSourceSet.apiConfigurationName,
                    kotlinSourceSet.implementationConfigurationName,
                    kotlinSourceSet.compileOnlyConfigurationName,
                    kotlinSourceSet.runtimeOnlyConfigurationName
                )
            }
        }

        // Then also add the Kotlin compilation dependencies (which include the dependencies from all source sets that
        // take part in the compilation) to Android source sets that are only included into a single variant corresponding
        // to that compilation. This is needed in order for the dependencies to get propagated to
        // the test variants; see KT-29343;

        // Trivial mapping of Android variants to Android source set names is impossible here,
        // because some variants have their dedicated source sets with mismatching names,
        // e.g. variant 'fooBarDebugAndroidTest' <-> source set 'androidTestFooBarDebug'

        // In single-platform projects, the Kotlin compilations already reference the Android plugin's configurations by the names,
        // so there are no such separate things as the configurations of the compilations, and there's no need to setup the
        // extendsFrom relationship.
        if (kotlinAndroidTarget.disambiguationClassifier != null) {

            val sourceSetToVariants = mutableMapOf<AndroidSourceSet, MutableList<BaseVariant>>().apply {
                forEachVariant(project) { variant ->
                    for (sourceSet in variant.sourceSets) {
                        val androidSourceSet = sourceSet as? AndroidSourceSet ?: continue
                        getOrPut(androidSourceSet) { mutableListOf() }.add(variant)
                    }
                }
            }

            for ((androidSourceSet, variants) in sourceSetToVariants) {
                val variant = variants.singleOrNull()
                    ?: continue // skip source sets that are included in multiple Android variants

                val compilation = kotlinAndroidTarget.compilations.getByName(getVariantName(variant))
                addDependenciesToAndroidSourceSet(
                    androidSourceSet,
                    compilation.apiConfigurationName,
                    compilation.implementationConfigurationName,
                    compilation.compileOnlyConfigurationName,
                    compilation.runtimeOnlyConfigurationName
                )
            }
        }
    }

    private fun applyAndroidJavaVersion(baseExtension: BaseExtension, kotlinOptions: KotlinJvmOptions) {
        val javaVersion =
            listOf(baseExtension.compileOptions.sourceCompatibility, baseExtension.compileOptions.targetCompatibility).min()!!
        if (javaVersion >= JavaVersion.VERSION_1_8)
            kotlinOptions.jvmTarget = "1.8"
    }

    private fun preprocessVariant(
        variantData: BaseVariant,
        compilation: KotlinJvmAndroidCompilation,
        project: Project,
        rootKotlinOptions: KotlinJvmOptionsImpl,
        tasksProvider: KotlinTasksProvider
    ) {
        // This function is called before the variantData is completely filled by the Android plugin.
        // The fine details of variantData, such as AP options or Java sources, should not be trusted here.

        checkVariantIsValid(variantData)
        val variantDataName = getVariantName(variantData)
        logger.kotlinDebug("Process variant [$variantDataName]")

        val defaultSourceSet = project.kotlinExtension.sourceSets.maybeCreate(compilation.defaultSourceSetName)

        val kotlinTaskName = compilation.compileKotlinTaskName

        KotlinCompileTaskData.register(kotlinTaskName, compilation).apply {
            // store kotlin classes in separate directory. They will serve as class-path to java compiler
            destinationDir.set(project.provider { File(project.buildDir, "tmp/kotlin-classes/$variantDataName") })
        }

        tasksProvider.registerKotlinJVMTask(project, kotlinTaskName, compilation) {
            it.parentKotlinOptionsImpl = rootKotlinOptions

            it.description = "Compiles the $variantDataName kotlin."
        }

        // Register the source only after the task is created, because the task is required for that:
        compilation.source(defaultSourceSet)
    }

    private fun postprocessVariant(
        variantData: BaseVariant,
        compilation: KotlinJvmAndroidCompilation,
        project: Project,
        androidExt: BaseExtension,
        androidPlugin: BasePlugin
    ) {
        val javaTask = variantData.getJavaTaskProvider()

        getTestedVariantData(variantData)?.let { testedVariant ->
            val testedVariantName = getVariantName(testedVariant)
            val testedCompilation = compilation.target.compilations.getByName(testedVariantName)
            compilation.associateWith(testedCompilation)
        }

        val kotlinTask = compilation.compileKotlinTaskProvider
        processAndroidKotlinAndJavaSources(
            compilation,
            addKotlinSources = { kotlinSourceSet -> compilation.source(kotlinSourceSet) },
            addJavaSources = { sources -> compilation.compileKotlinTaskProvider.configure { it.source(sources) } }
        )
        wireKotlinTasks(project, compilation, androidPlugin, androidExt, variantData, javaTask, kotlinTask)
    }
}

private fun getAllJavaSources(variantData: BaseVariant): Iterable<File> =
    variantData.getSourceFolders(SourceKind.JAVA).map { it.dir }

internal fun processAndroidKotlinAndJavaSources(
    compilation: KotlinJvmAndroidCompilation,
    addKotlinSources: (KotlinSourceSet) -> Unit,
    addJavaSources: (Iterable<File>) -> Unit
) {
    val variantData = compilation.androidVariant

    for (provider in variantData.sourceSets) {
        val kotlinSourceSet = provider.getConvention(KOTLIN_DSL_NAME) as? KotlinSourceSet ?: continue
        addKotlinSources(kotlinSourceSet)
    }

    addJavaSources(getAllJavaSources(variantData))
}

internal fun configureJavaTask(
    kotlinTaskProvider: TaskProvider<out KotlinCompile>,
    javaTaskProvider: TaskProvider<out AbstractCompile>
) {
    javaTaskProvider.configure { javaTask ->
        val kotlinTask = kotlinTaskProvider.get()
        kotlinTask.javaOutputDir = javaTask.destinationDir

        // Make Gradle check if the javaTask is up-to-date based on the Kotlin classes
        javaTask.inputs.run {
            dir(kotlinTask.destinationDir)
                .withNormalizer(CompileClasspathNormalizer::class.java)
                .withPropertyName("${kotlinTask.name}OutputClasses")
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
        javaTask.appendClasspathDynamically(kotlinTask.destinationDir)
    }
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
                    inputs.property("$subpluginId." + option.key + indexSuffix, Callable { option.value })
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

internal fun Project.forEachVariant(action: (BaseVariant) -> Unit) {
    val androidExtension = this.extensions.getByName("android")
    when (androidExtension) {
        is AppExtension -> androidExtension.applicationVariants.all(action)
        is LibraryExtension -> {
            androidExtension.libraryVariants.all(action)
            if (androidExtension is FeatureExtension) {
                androidExtension.featureVariants.all(action)
            }
        }
        is TestExtension -> androidExtension.applicationVariants.all(action)
    }
    if (androidExtension is TestedExtension) {
        androidExtension.testVariants.all(action)
        androidExtension.unitTestVariants.all(action)
    }
}
