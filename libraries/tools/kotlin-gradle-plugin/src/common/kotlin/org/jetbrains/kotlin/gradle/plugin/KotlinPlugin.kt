/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.*
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.model.builder.KotlinModelBuilder
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.internal.JavaSourceSetsAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.MavenPluginConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.isMainCompilationData
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.configuration.*
import org.jetbrains.kotlin.gradle.utils.*
import java.io.File
import java.util.concurrent.Callable

const val PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerPluginClasspath"
const val NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kotlinNativeCompilerPluginClasspath"
internal const val COMPILER_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerClasspath"
internal const val KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME = "kotlinKlibCommonizerClasspath"

val KOTLIN_DSL_NAME = "kotlin"

@Deprecated("Should be removed with 'platform.js' plugin removal")
val KOTLIN_JS_DSL_NAME = "kotlin2js"
val KOTLIN_OPTIONS_DSL_NAME = "kotlinOptions"

abstract class KotlinCompilationProcessor<out T : AbstractKotlinCompileTool<*>>(
    open val kotlinCompilation: KotlinCompilationData<*>
) {
    abstract val kotlinTask: TaskProvider<out T>
    abstract fun run()

    protected val project: Project
        get() = kotlinCompilation.project

    protected val defaultKotlinDestinationDir: Provider<Directory>
        get() {
            val kotlinExt = project.topLevelExtension
            val targetSubDirectory =
                if (kotlinExt is KotlinSingleJavaTargetExtension)
                    "" // In single-target projects, don't add the target name part to this path
                else
                    kotlinCompilation.compilationClassifier?.let { "$it/" }.orEmpty()
            return project.layout.buildDirectory.dir("classes/kotlin/$targetSubDirectory${kotlinCompilation.compilationPurpose}")
        }
}

internal abstract class KotlinSourceSetProcessor<T : AbstractKotlinCompile<*>>(
    val tasksProvider: KotlinTasksProvider,
    val taskDescription: String,
    kotlinCompilation: KotlinCompilationData<*>
) : KotlinCompilationProcessor<T>(kotlinCompilation) {
    protected abstract fun doTargetSpecificProcessing()
    protected val logger = Logging.getLogger(this.javaClass)!!

    protected val sourceSetName: String = kotlinCompilation.compilationPurpose

    override val kotlinTask: TaskProvider<out T> = prepareKotlinCompileTask()

    protected val javaSourceSet: SourceSet?
        get() {
            val compilation = kotlinCompilation
            return (compilation as? KotlinWithJavaCompilation<*, *>)?.javaSourceSet
                ?: kotlinCompilation.owner.let {
                    if (it is KotlinJvmTarget && it.withJavaEnabled && compilation is KotlinJvmCompilation)
                        project.gradle.variantImplementationFactory<JavaSourceSetsAccessor.JavaSourceSetsAccessorVariantFactory>()
                            .getInstance(project)
                            .sourceSets
                            .maybeCreate(compilation.name)
                    else null
                }
        }

    private fun prepareKotlinCompileTask(): TaskProvider<out T> =
        doRegisterTask(project, kotlinCompilation.compileKotlinTaskName).also { task ->
            kotlinCompilation.output.classesDirs.from(task.flatMap { it.destinationDirectory })
        }

    override fun run() {
        addKotlinDirectoriesToJavaSourceSet()
        doTargetSpecificProcessing()

        if (kotlinCompilation is KotlinWithJavaCompilation<*, *>) {
            createAdditionalClassesTaskForIdeRunner()
        }
    }

    private fun addKotlinDirectoriesToJavaSourceSet() {
        val java = javaSourceSet ?: return

        // Try to avoid duplicate Java sources in allSource; run lazily to allow changing the directory set:
        val kotlinSrcDirsToAdd = Callable {
            kotlinCompilation.kotlinSourceDirectoriesByFragmentName.values.map { filterOutJavaSrcDirsIfPossible(it) }
        }

        java.allJava.srcDirs(kotlinSrcDirsToAdd)
        java.allSource.srcDirs(kotlinSrcDirsToAdd)
    }

    private fun filterOutJavaSrcDirsIfPossible(sourceDirectories: SourceDirectorySet): FileCollection {
        val java = javaSourceSet ?: return sourceDirectories

        // Build a lazily-resolved file collection that filters out Java sources from sources of this sourceDirectorySet
        return sourceDirectories.sourceDirectories.minus(java.java.sourceDirectories)
    }

    private fun createAdditionalClassesTaskForIdeRunner() {
        val kotlinCompilation = kotlinCompilation as? KotlinCompilation<*> ?: return

        open class IDEClassesTask : DefaultTask()
        // Workaround: as per KT-26641, when there's a Kotlin compilation with a Java source set, we create another task
        // that has a name composed as '<IDE module name>Classes`, where the IDE module name is the default source set name:
        val expectedClassesTaskName = "${kotlinCompilation.defaultSourceSetName}Classes"
        project.tasks.run {
            val shouldCreateTask = expectedClassesTaskName !in names
            if (shouldCreateTask) {
                project.registerTask(expectedClassesTaskName, IDEClassesTask::class.java) {
                    it.dependsOn(getByName(kotlinCompilation.compileAllTaskName))
                }
            }
        }
    }

    protected fun applyStandardTaskConfiguration(taskConfiguration: AbstractKotlinCompileConfig<*>) {
        taskConfiguration.configureTask {
            it.description = taskDescription
            if (it is Kotlin2JsCompile) {
                it.defaultDestinationDirectory.convention(defaultKotlinDestinationDir)
            } else {
                it.destinationDirectory.convention(defaultKotlinDestinationDir)
            }
            it.libraries.from({ kotlinCompilation.compileDependencyFiles })
        }
    }

    protected abstract fun doRegisterTask(project: Project, taskName: String): TaskProvider<out T>
}

internal class Kotlin2JvmSourceSetProcessor(
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: KotlinCompilationData<*>
) : KotlinSourceSetProcessor<KotlinCompile>(
    tasksProvider, "Compiles the $kotlinCompilation.", kotlinCompilation
) {
    override fun doRegisterTask(project: Project, taskName: String): TaskProvider<out KotlinCompile> {
        val configAction = KotlinCompileConfig(kotlinCompilation)
        applyStandardTaskConfiguration(configAction)
        return tasksProvider.registerKotlinJVMTask(
            project,
            taskName,
            kotlinCompilation.compilerOptions.options as KotlinJvmCompilerOptions,
            configAction
        )
    }

    override fun doTargetSpecificProcessing() {
        ifKaptEnabled(project) {
            Kapt3GradleSubplugin.createAptConfigurationIfNeeded(project, kotlinCompilation.compilationPurpose)
        }

        ScriptingGradleSubplugin.configureForSourceSet(project, kotlinCompilation.compilationPurpose)

        project.whenEvaluated {
            val subpluginEnvironment = SubpluginEnvironment.loadSubplugins(project)

            if (kotlinCompilation is KotlinCompilation<*>) // FIXME support compiler plugins with PM20
                subpluginEnvironment.addSubpluginOptions(project, kotlinCompilation)

            javaSourceSet?.let { java ->
                val javaTask = project.tasks.withType<AbstractCompile>().named(java.compileJavaTaskName)
                javaTask.configure { javaCompile ->
                    javaCompile.classpath += project.files(kotlinTask.flatMap { it.destinationDirectory })
                }
                kotlinTask.configure { kotlinCompile ->
                    kotlinCompile.javaOutputDir.set(javaTask.flatMap { it.destinationDirectory })
                }
            }

            if (sourceSetName == SourceSet.MAIN_SOURCE_SET_NAME) {
                project.pluginManager.withPlugin("java-library") {
                    registerKotlinOutputForJavaLibrary(kotlinTask.flatMap { it.destinationDirectory })
                }
            }
        }
    }

    private fun registerKotlinOutputForJavaLibrary(outputDir: Provider<Directory>) {
        val configuration = project.configurations.getByName("apiElements")
        configuration.outgoing.variants.getByName("classes").artifact(outputDir) {
            it.type = "java-classes-directory"
        }
    }
}

internal fun KotlinCompilationOutput.addClassesDir(classesDirProvider: () -> FileCollection) {
    classesDirs.from(Callable { classesDirProvider() })
}

internal class Kotlin2JsSourceSetProcessor(
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: KotlinCompilationData<*>
) : KotlinSourceSetProcessor<Kotlin2JsCompile>(
    tasksProvider,
    taskDescription = "Compiles the Kotlin sources in $kotlinCompilation to JavaScript.",
    kotlinCompilation = kotlinCompilation
) {
    override fun doRegisterTask(project: Project, taskName: String): TaskProvider<out Kotlin2JsCompile> {
        val configAction = Kotlin2JsCompileConfig(kotlinCompilation)
        applyStandardTaskConfiguration(configAction)
        return tasksProvider.registerKotlinJSTask(
            project,
            taskName,
            kotlinCompilation.compilerOptions.options as KotlinJsCompilerOptions,
            configAction
        )
    }

    override fun doTargetSpecificProcessing() {
        project.tasks.named(kotlinCompilation.compileAllTaskName).configure {
            it.dependsOn(kotlinTask)
        }

        if (kotlinCompilation is KotlinWithJavaCompilation<*, *>) {
            kotlinCompilation.javaSourceSet.clearJavaSrcDirs()
        }


        project.whenEvaluated {
            val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project)
            if (kotlinCompilation is KotlinCompilation<*>) { // FIXME support compiler plugins with PM20
                subpluginEnvironment.addSubpluginOptions(project, kotlinCompilation)
            }
        }
    }
}

internal class KotlinJsIrSourceSetProcessor(
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: AbstractKotlinCompilation<*>
) : KotlinSourceSetProcessor<Kotlin2JsCompile>(
    tasksProvider, taskDescription = "Compiles the Kotlin sources in $kotlinCompilation to JavaScript.",
    kotlinCompilation = kotlinCompilation
) {
    override fun doRegisterTask(project: Project, taskName: String): TaskProvider<out Kotlin2JsCompile> {
        val configAction = Kotlin2JsCompileConfig(kotlinCompilation)
        applyStandardTaskConfiguration(configAction)
        return tasksProvider.registerKotlinJSTask(
            project,
            taskName,
            kotlinCompilation.compilerOptions.options as KotlinJsCompilerOptions,
            configAction
        )
    }

    override fun doTargetSpecificProcessing() {
        project.tasks.named(kotlinCompilation.compileAllTaskName).configure {
            it.dependsOn(kotlinTask)
        }

        val compilation = kotlinCompilation as KotlinJsIrCompilation

        compilation.binaries
            .withType(JsIrBinary::class.java)
            .all { binary ->
                val configAction = KotlinJsIrLinkConfig(binary)
                configAction.configureTask {
                    it.description = taskDescription
                    it.libraries.from({ kotlinCompilation.compileDependencyFiles })
                }
                configAction.configureTask { task ->
                    task.modeProperty.set(binary.mode)
                    task.dependsOn(kotlinTask)
                }

                tasksProvider.registerKotlinJsIrTask(project, binary.linkTaskName, configAction)
            }

        project.whenEvaluated {
            val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project)
            subpluginEnvironment.addSubpluginOptions(project, kotlinCompilation)
        }
    }
}

internal class KotlinCommonSourceSetProcessor(
    compilation: KotlinCompilationData<*>,
    tasksProvider: KotlinTasksProvider
) : KotlinSourceSetProcessor<KotlinCompileCommon>(
    tasksProvider, taskDescription = "Compiles the kotlin sources in $compilation to Metadata.", kotlinCompilation = compilation
) {
    override fun doTargetSpecificProcessing() {
        project.tasks.named(kotlinCompilation.compileAllTaskName).dependsOn(kotlinTask)
        // can be missing (e.g. in case of tests)
        if ((kotlinCompilation as? AbstractKotlinCompilation<*>)?.isMainCompilationData() == true) {
            project.locateTask<Task>(kotlinCompilation.target.artifactsTaskName)?.dependsOn(kotlinTask)
        }

        if (kotlinCompilation is KotlinCompilation<*>) {
            project.whenEvaluated {
                val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project)
                subpluginEnvironment.addSubpluginOptions(project, kotlinCompilation)
            }
        }
    }

    override fun doRegisterTask(project: Project, taskName: String): TaskProvider<out KotlinCompileCommon> {
        val configAction = KotlinCompileCommonConfig(kotlinCompilation)
        applyStandardTaskConfiguration(configAction)
        return tasksProvider.registerKotlinCommonTask(
            project,
            taskName,
            kotlinCompilation.compilerOptions.options as KotlinMultiplatformCommonCompilerOptions,
            configAction
        )
    }
}

internal abstract class AbstractKotlinPlugin(
    val tasksProvider: KotlinTasksProvider,
    val registry: ToolingModelBuilderRegistry
) : Plugin<Project> {

    internal abstract fun buildSourceSetProcessor(
        project: Project,
        compilation: AbstractKotlinCompilation<*>
    ): KotlinSourceSetProcessor<*>

    override fun apply(project: Project) {
        val kotlinPluginVersion = project.getKotlinPluginVersion()
        project.plugins.apply(JavaPlugin::class.java)

        val target = (project.kotlinExtension as KotlinSingleJavaTargetExtension).target

        configureTarget(
            target,
            { compilation -> buildSourceSetProcessor(project, compilation) }
        )

        applyUserDefinedAttributes(target)

        rewriteMppDependenciesInPom(target)

        configureProjectGlobalSettings(project)
        configureClassInspectionForIC(project)
        registry.register(KotlinModelBuilder(kotlinPluginVersion, null))

        project.components.addAll(target.components)

    }

    protected open fun configureClassInspectionForIC(project: Project) {
        // Check if task was already added by one of plugin implementations
        if (project.tasks.names.contains(INSPECT_IC_CLASSES_TASK_NAME)) return

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

        val inspectTask = project.registerTask<InspectClassesForMultiModuleIC>(INSPECT_IC_CLASSES_TASK_NAME) { inspectTask ->
            inspectTask.archivePath.set(jarTask.map { it.archivePathCompatible.canonicalPath })
            inspectTask.archivePath.disallowChanges()

            inspectTask.sourceSetName.set(SourceSet.MAIN_SOURCE_SET_NAME)
            inspectTask.sourceSetName.disallowChanges()

            inspectTask.classesListFile.set(
                project.layout.file(
                    (project.kotlinExtension as KotlinSingleJavaTargetExtension)
                        .target
                        .defaultArtifactClassesListFile
                )
            )
            inspectTask.classesListFile.disallowChanges()

            val sourceSetClassesDir = project.gradle
                .variantImplementationFactory<JavaSourceSetsAccessor.JavaSourceSetsAccessorVariantFactory>()
                .getInstance(project)
                .sourceSetsIfAvailable
                ?.findByName(SourceSet.MAIN_SOURCE_SET_NAME)
                ?.output
                ?.classesDirs
                ?: project.objects.fileCollection()
            inspectTask.sourceSetOutputClassesDir.from(sourceSetClassesDir).disallowChanges()

            inspectTask.dependsOn(classesTask)
        }
        classesTask.configure { it.finalizedBy(inspectTask) }
    }

    private fun rewritePom(pom: MavenPom, rewriter: PomDependenciesRewriter, shouldRewritePom: Provider<Boolean>) {
        pom.withXml { xml ->
            if (shouldRewritePom.get())
                rewriter.rewritePomMppDependenciesToActualTargetModules(xml)
        }
    }

    private fun rewriteMppDependenciesInPom(target: AbstractKotlinTarget) {
        val project = target.project

        val shouldRewritePoms = project.provider {
            PropertiesProvider(project).keepMppDependenciesIntactInPoms != true
        }

        project.pluginManager.withPlugin("maven-publish") {
            project.extensions.configure(PublishingExtension::class.java) { publishing ->
                val pomRewriter = PomDependenciesRewriter(project, target.kotlinComponents.single())
                publishing.publications.withType(MavenPublication::class.java).all { publication ->
                    rewritePom(publication.pom, pomRewriter, shouldRewritePoms)
                }
            }
        }

        project.gradle
            .variantImplementationFactory<MavenPluginConfigurator.MavenPluginConfiguratorVariantFactory>()
            .getInstance()
            .applyConfiguration(project, target, shouldRewritePoms)
    }

    companion object {
        private const val INSPECT_IC_CLASSES_TASK_NAME = "inspectClassesForKotlinIC"

        fun configureProjectGlobalSettings(project: Project) {
            customizeKotlinDependencies(project)
            project.setupGeneralKotlinExtensionParameters()
        }

        fun configureTarget(
            target: KotlinWithJavaTarget<*, *>,
            buildSourceSetProcessor: (AbstractKotlinCompilation<*>) -> KotlinSourceSetProcessor<*>
        ) {
            setUpJavaSourceSets(target)
            configureSourceSetDefaults(target, buildSourceSetProcessor)
            configureAttributes(target)
        }

        internal fun setUpJavaSourceSets(
            kotlinTarget: KotlinTarget,
            duplicateJavaSourceSetsAsKotlinSourceSets: Boolean = true
        ) {
            val project = kotlinTarget.project
            val javaSourceSets = project
                .gradle
                .variantImplementationFactory<JavaSourceSetsAccessor.JavaSourceSetsAccessorVariantFactory>()
                .getInstance(project)
                .sourceSets

            @Suppress("DEPRECATION") val kotlinSourceSetDslName = when (kotlinTarget.platformType) {
                KotlinPlatformType.js -> KOTLIN_JS_DSL_NAME
                else -> KOTLIN_DSL_NAME
            }

            // Workaround for indirect mutual recursion between the two `all { ... }` handlers:
            val compilationsUnderConstruction = mutableMapOf<String, KotlinCompilation<*>>()

            javaSourceSets.all { javaSourceSet ->
                val kotlinCompilation =
                    compilationsUnderConstruction[javaSourceSet.name] ?: kotlinTarget.compilations.maybeCreate(javaSourceSet.name)
                (kotlinCompilation as? KotlinWithJavaCompilation<*, *>)?.javaSourceSet = javaSourceSet

                if (duplicateJavaSourceSetsAsKotlinSourceSets) {
                    val kotlinSourceSet = project.kotlinExtension.sourceSets.maybeCreate(kotlinCompilation.name)
                    kotlinSourceSet.kotlin.source(javaSourceSet.java)
                    kotlinCompilation.source(kotlinSourceSet)
                    @Suppress("DEPRECATION")
                    javaSourceSet.addConvention(kotlinSourceSetDslName, kotlinSourceSet)
                    javaSourceSet.addExtension(kotlinSourceSetDslName, kotlinSourceSet.kotlin)
                } else {
                    @Suppress("DEPRECATION")
                    javaSourceSet.addConvention(kotlinSourceSetDslName, kotlinCompilation.defaultSourceSet)
                    javaSourceSet.addExtension(kotlinSourceSetDslName, kotlinCompilation.defaultSourceSet.kotlin)
                }
            }

            kotlinTarget.compilations.all { kotlinCompilation ->
                val sourceSetName = kotlinCompilation.name
                compilationsUnderConstruction[sourceSetName] = kotlinCompilation
                (kotlinCompilation as? KotlinWithJavaCompilation<*, *>)?.javaSourceSet = javaSourceSets.maybeCreate(sourceSetName)

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
            kotlinTarget: KotlinWithJavaTarget<*, *>
        ) {
            val project = kotlinTarget.project

            // Setup the consuming configurations:
            project.dependencies.attributesSchema.attribute(KotlinPlatformType.attribute)

            // Setup the published configurations:
            // Don't set the attributes for common module; otherwise their 'common' platform won't be compatible with the one in
            // platform-specific modules
            if (kotlinTarget.platformType != KotlinPlatformType.common) {
                project.configurations.getByName(kotlinTarget.apiElementsConfigurationName).run {
                    attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerApiUsage(kotlinTarget))
                    attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                    usesPlatformOf(kotlinTarget)
                }

                project.configurations.getByName(kotlinTarget.runtimeElementsConfigurationName).run {
                    attributes.attribute(Usage.USAGE_ATTRIBUTE, KotlinUsages.producerRuntimeUsage(kotlinTarget))
                    attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
                    usesPlatformOf(kotlinTarget)
                }
            }
        }

        private fun configureSourceSetDefaults(
            kotlinTarget: KotlinWithJavaTarget<*, *>,
            buildSourceSetProcessor: (AbstractKotlinCompilation<*>) -> KotlinSourceSetProcessor<*>
        ) {
            kotlinTarget.compilations.all { compilation ->
                AbstractKotlinTargetConfigurator.defineConfigurationsForCompilation(compilation)
                buildSourceSetProcessor(compilation).run()
            }
        }
    }
}

internal open class KotlinPlugin(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPlugin(KotlinTasksProvider(), registry) {

    companion object {
        private const val targetName = "" // use empty suffix for the task names
    }

    override fun buildSourceSetProcessor(project: Project, compilation: AbstractKotlinCompilation<*>) =
        Kotlin2JvmSourceSetProcessor(tasksProvider, compilation)

    override fun apply(project: Project) {
        @Suppress("UNCHECKED_CAST")
        val target = (project.objects.newInstance(
            KotlinWithJavaTarget::class.java,
            project,
            KotlinPlatformType.jvm,
            targetName,
            {
                object : HasCompilerOptions<KotlinJvmCompilerOptions> {
                    override val options: KotlinJvmCompilerOptions =
                        project.objects.newInstance(KotlinJvmCompilerOptionsDefault::class.java)
                }
            },
            { compilerOptions: KotlinJvmCompilerOptions ->
                object : KotlinJvmOptions {
                    override val options: KotlinJvmCompilerOptions get() = compilerOptions
                }
            }
        ) as KotlinWithJavaTarget<KotlinJvmOptions, KotlinJvmCompilerOptions>)
            .apply {
                disambiguationClassifier = null // don't add anything to the task names
            }

        (project.kotlinExtension as KotlinJvmProjectExtension).target = target

        super.apply(project)

        project.pluginManager.apply(ScriptingGradleSubplugin::class.java)
    }

    override fun configureClassInspectionForIC(project: Project) {
        // For new IC this task is not needed
        if (!project.kotlinPropertiesProvider.useClasspathSnapshot) {
            super.configureClassInspectionForIC(project)
        }
    }
}

internal open class KotlinCommonPlugin(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPlugin(KotlinTasksProvider(), registry) {

    companion object {
        private const val targetName = "common"
    }

    override fun buildSourceSetProcessor(
        project: Project,
        compilation: AbstractKotlinCompilation<*>
    ): KotlinSourceSetProcessor<*> =
        KotlinCommonSourceSetProcessor(compilation, tasksProvider)

    override fun apply(project: Project) {
        @Suppress("UNCHECKED_CAST")
        val target = project.objects.newInstance(
            KotlinWithJavaTarget::class.java,
            project,
            KotlinPlatformType.common,
            targetName,
            {
                object : HasCompilerOptions<KotlinMultiplatformCommonCompilerOptions> {
                    override val options: KotlinMultiplatformCommonCompilerOptions =
                        project.objects.newInstance(KotlinMultiplatformCommonCompilerOptionsDefault::class.java)
                }
            },
            { compilerOptions: KotlinMultiplatformCommonCompilerOptions ->
                object : KotlinMultiplatformCommonOptions {
                    override val options: KotlinMultiplatformCommonCompilerOptions
                        get() = compilerOptions
                }
            }
        ) as KotlinWithJavaTarget<KotlinMultiplatformCommonOptions, KotlinMultiplatformCommonCompilerOptions>
        (project.kotlinExtension as KotlinCommonProjectExtension).target = target

        super.apply(project)
    }
}

@Deprecated(
    message = "Should be removed with Js platform plugin",
    level = DeprecationLevel.ERROR
)
internal open class Kotlin2JsPlugin(
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPlugin(KotlinTasksProvider(), registry) {

    companion object {
        private const val targetName = "2Js"
    }

    override fun buildSourceSetProcessor(
        project: Project,
        compilation: AbstractKotlinCompilation<*>
    ): KotlinSourceSetProcessor<*> =
        Kotlin2JsSourceSetProcessor(tasksProvider, compilation)

    override fun apply(project: Project) {
        @Suppress("UNCHECKED_CAST")
        val target = project.objects.newInstance(
            KotlinWithJavaTarget::class.java,
            project,
            KotlinPlatformType.js,
            targetName,
            {
                object : HasCompilerOptions<KotlinJsCompilerOptions> {
                    override val options: KotlinJsCompilerOptions =
                        project.objects.newInstance(KotlinJsCompilerOptionsDefault::class.java)
                }
            },
            { compilerOptions: KotlinJsCompilerOptions ->
                object : KotlinJsOptions {
                    override val options: KotlinJsCompilerOptions
                        get() = compilerOptions
                }
            }
        ) as KotlinWithJavaTarget<KotlinJsOptions, KotlinJsCompilerOptions>

        (project.kotlinExtension as Kotlin2JsProjectExtension).setTarget(target)
        super.apply(project)
    }
}

internal open class KotlinAndroidPlugin(
    private val registry: ToolingModelBuilderRegistry
) : Plugin<Project> {

    override fun apply(project: Project) {
        checkGradleCompatibility()

        project.dynamicallyApplyWhenAndroidPluginIsApplied(
            {
                project.objects.newInstance(
                    KotlinAndroidTarget::class.java,
                    "",
                    project
                ).also {
                    (project.kotlinExtension as KotlinAndroidProjectExtension).target = it
                }
            }
        ) { androidTarget ->
            applyUserDefinedAttributes(androidTarget)
            customizeKotlinDependencies(project)
            registry.register(KotlinModelBuilder(project.getKotlinPluginVersion(), androidTarget))
            project.whenEvaluated { project.components.addAll(androidTarget.components) }
        }
    }

    companion object {
        private val minimalSupportedAgpVersion = AndroidGradlePluginVersion(4, 1, 3)
        fun androidTargetHandler(): AndroidProjectHandler {
            val tasksProvider = KotlinTasksProvider()
            val androidGradlePluginVersion = AndroidGradlePluginVersion.currentOrNull

            if (androidGradlePluginVersion != null) {
                if (androidGradlePluginVersion < minimalSupportedAgpVersion) {
                    throw IllegalStateException(
                        "Kotlin: Unsupported version of com.android.tools.build:gradle plugin: " +
                                "version $minimalSupportedAgpVersion or higher should be used with kotlin-android plugin"
                    )
                }
            }

            return AndroidProjectHandler(KotlinConfigurationTools(tasksProvider))
        }

        internal fun Project.dynamicallyApplyWhenAndroidPluginIsApplied(
            kotlinAndroidTargetProvider: () -> KotlinAndroidTarget,
            additionalConfiguration: (KotlinAndroidTarget) -> Unit = {}
        ) {
            var wasConfigured = false

            androidPluginIds.forEach { pluginId ->
                plugins.withId(pluginId) {
                    wasConfigured = true
                    val target = kotlinAndroidTargetProvider()
                    androidTargetHandler().configureTarget(target)
                    additionalConfiguration(target)
                }
            }

            afterEvaluate {
                if (!wasConfigured) {
                    throw GradleException(
                        """
                        |'kotlin-android' plugin requires one of the Android Gradle plugins.
                        |Please apply one of the following plugins to '${project.path}' project:
                        |${androidPluginIds.joinToString(prefix = "- ", separator = "\n\t- ")}
                        """.trimMargin()
                    )
                }
            }
        }
    }
}

class KotlinConfigurationTools internal constructor(
    @Suppress("EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_WARNING", "EXPOSED_PROPERTY_TYPE_IN_CONSTRUCTOR_ERROR")
    val kotlinTasksProvider: KotlinTasksProvider
)


internal fun ifKaptEnabled(project: Project, block: () -> Unit) {
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
