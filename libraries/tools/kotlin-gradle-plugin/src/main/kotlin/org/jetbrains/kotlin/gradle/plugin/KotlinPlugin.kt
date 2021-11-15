/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.*
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.Upload
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.internal.customizeKotlinDependencies
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.model.builder.KotlinModelBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.isMainCompilationData
import org.jetbrains.kotlin.gradle.scripting.internal.ScriptingGradleSubplugin
import org.jetbrains.kotlin.gradle.targets.android.AbstractAndroidProjectHandler
import org.jetbrains.kotlin.gradle.targets.android.androidPluginVersion
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBinaryMode
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.targets.js.jsPluginDeprecationMessage
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File
import java.util.concurrent.Callable
import kotlin.reflect.full.functions
import kotlin.reflect.full.staticProperties
import kotlin.reflect.jvm.isAccessible

const val PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerPluginClasspath"
const val NATIVE_COMPILER_PLUGIN_CLASSPATH_CONFIGURATION_NAME = "kotlinNativeCompilerPluginClasspath"
internal const val COMPILER_CLASSPATH_CONFIGURATION_NAME = "kotlinCompilerClasspath"
internal const val KLIB_COMMONIZER_CLASSPATH_CONFIGURATION_NAME = "kotlinKlibCommonizerClasspath"

val KOTLIN_DSL_NAME = "kotlin"
val KOTLIN_JS_DSL_NAME = "kotlin2js"
val KOTLIN_OPTIONS_DSL_NAME = "kotlinOptions"

abstract class KotlinCompilationProcessor<out T : SourceTask>(
    open val kotlinCompilation: KotlinCompilationData<*>
) {
    abstract val kotlinTask: TaskProvider<out T>
    abstract fun run()

    protected val project: Project
        get() = kotlinCompilation.project

    protected val defaultKotlinDestinationDir: File
        get() {
            val kotlinExt = project.topLevelExtension
            val targetSubDirectory =
                if (kotlinExt is KotlinSingleJavaTargetExtension)
                    "" // In single-target projects, don't add the target name part to this path
                else
                    kotlinCompilation.compilationClassifier?.let { "$it/" }.orEmpty()
            return File(project.buildDir, "classes/kotlin/$targetSubDirectory${kotlinCompilation.compilationPurpose}")
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
            return (compilation as? KotlinWithJavaCompilation<*>)?.javaSourceSet
                ?: kotlinCompilation.owner.let {
                    if (it is KotlinJvmTarget && it.withJavaEnabled && compilation is KotlinJvmCompilation)
                        project.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.maybeCreate(compilation.name)
                    else null
                }
        }

    private fun prepareKotlinCompileTask(): TaskProvider<out T> =
        registerKotlinCompileTask(register = ::doRegisterTask).also { task ->
            kotlinCompilation.output.classesDirs.from(task.flatMap { it.destinationDirectory })
        }

    protected fun registerKotlinCompileTask(
        name: String = kotlinCompilation.compileKotlinTaskName,
        register: (Project, String, (T) -> Unit) -> TaskProvider<out T>
    ): TaskProvider<out T> {
        logger.kotlinDebug("Creating kotlin compile task $name")

        return register(project, name) {
            it.description = taskDescription
            it.destinationDirectory.set(defaultKotlinDestinationDir)
            it.classpath = project.files({ kotlinCompilation.compileDependencyFiles })
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

    protected abstract fun doRegisterTask(project: Project, taskName: String, configureAction: (T) -> (Unit)): TaskProvider<out T>
}

internal class Kotlin2JvmSourceSetProcessor(
    tasksProvider: KotlinTasksProvider,
    kotlinCompilation: KotlinCompilationData<*>
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
                val kotlinOptions = kotlinTaskInstance.kotlinOptions
                val outputFile = kotlinTaskInstance.outputFileProperty.get()
                val outputDir: File = outputFile.parentFile
                kotlinOptions.outputFile = if (!kotlinOptions.isProduceUnzippedKlib()) {
                    outputFile.absolutePath
                } else {
                    outputFile.parentFile.absolutePath
                }
                if (outputDir.isParentOf(project.rootDir))
                    throw InvalidUserDataException(
                        "The output directory '$outputDir' (defined by outputFile of $kotlinTaskInstance) contains or " +
                                "matches the project root directory '${project.rootDir}'.\n" +
                                "Gradle will not be able to build the project because of the root directory lock.\n" +
                                "To fix this, consider using the default outputFile location instead of providing it explicitly."
                    )
                kotlinTaskInstance.destinationDirectory.set(outputDir)

                if (
                    kotlinOptions.freeCompilerArgs.contains(PRODUCE_JS) ||
                    kotlinOptions.freeCompilerArgs.contains(PRODUCE_UNZIPPED_KLIB) ||
                    kotlinOptions.freeCompilerArgs.contains(PRODUCE_ZIPPED_KLIB)
                ) {
                    // Configure FQ module name to avoid cyclic dependencies in klib manifests (see KT-36721).
                    val baseName = if (kotlinCompilation.isMainCompilationData()) {
                        project.name
                    } else {
                        "${project.name}_${kotlinCompilation.compilationPurpose}"
                    }
                    kotlinTaskInstance.kotlinOptions.freeCompilerArgs += "$MODULE_NAME=${project.klibModuleName(baseName)}"
                }
            }

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

        if (kotlinCompilation is AbstractKotlinCompilation<*>) {
            project.whenEvaluated {
                val subpluginEnvironment: SubpluginEnvironment = SubpluginEnvironment.loadSubplugins(project)
                subpluginEnvironment.addSubpluginOptions(project, kotlinCompilation)
            }
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
        registry.register(KotlinModelBuilder(kotlinPluginVersion, null))

        project.components.addAll(target.components)
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

        if (GradleVersion.version(project.gradle.gradleVersion) < GradleVersion.version("7.0")) {
            project.pluginManager.withPlugin("maven") {
                project.tasks.withType(Upload::class.java).configureEach { uploadTask ->
                    uploadTask
                        .repositories
                        .withType(
                            Class.forName("org.gradle.api.artifacts.maven.MavenResolver")
                                .cast<Class<ArtifactRepository>>()
                        )
                        .configureEach { mavenResolver ->
                            val pomRewriter = PomDependenciesRewriter(project, target.kotlinComponents.single())
                            val mavenPom = mavenResolver::class
                                .functions
                                .first { it.name == "getPom" }
                                .also { it.isAccessible = true }
                                .call(mavenResolver)!!
                            mavenPom::class
                                .functions
                                .first { func ->
                                    // On older Gradle versions there were two 'withXml' method - one with 'Closure' and one with 'Action'
                                    func.name == "withXml" &&
                                            func.parameters.any {
                                                it.type.toString() == "org.gradle.api.Action<org.gradle.api.XmlProvider!>!"
                                            }
                                }
                                .call(mavenPom, Action<XmlProvider> { xml ->
                                    if (shouldRewritePoms.get()) {
                                        pomRewriter.rewritePomMppDependenciesToActualTargetModules(xml)
                                    }
                                })
                        }
                }

                // Setup conf2ScopeMappings so that the API dependencies are written
                // with compile scope in the POMs in case of 'java' plugin
                val mavenPluginConvention = project
                    .convention
                    .getPlugin(Class.forName("org.gradle.api.plugins.MavenPluginConvention"))

                val conf2ScopeMappingContainer = mavenPluginConvention::class
                    .functions
                    .first { it.name == "getConf2ScopeMappings" }
                    .call(mavenPluginConvention)!!

                conf2ScopeMappingContainer::class
                    .functions
                    .first { it.name == "addMapping" }
                    .call(
                        conf2ScopeMappingContainer,
                        0,
                        project.configurations.getByName("api"),
                        conf2ScopeMappingContainer::class
                            .staticProperties
                            .first { it.name == "COMPILE" }
                            .get()
                    )
            }
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

            project.configurations.getByName("default").apply {
                setupAsLocalTargetSpecificConfigurationIfSupported(kotlinTarget)
            }

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
            kotlinTarget: KotlinWithJavaTarget<*>,
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
    registry: ToolingModelBuilderRegistry
) : AbstractKotlinPlugin(KotlinTasksProvider(), registry) {

    companion object {
        private const val targetName = "2Js"
        internal const val NOWARN_2JS_FLAG = "kotlin.2js.nowarn"
    }

    override fun buildSourceSetProcessor(
        project: Project,
        compilation: AbstractKotlinCompilation<*>
    ): KotlinSourceSetProcessor<*> =
        Kotlin2JsSourceSetProcessor(tasksProvider, compilation)

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
    private val registry: ToolingModelBuilderRegistry
) : Plugin<Project> {

    override fun apply(project: Project) {
        checkGradleCompatibility()

        project.dynamicallyApplyWhenAndroidPluginIsApplied(
            {
                KotlinAndroidTarget("", project).also {
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
        fun androidTargetHandler(): AbstractAndroidProjectHandler {
            val tasksProvider = AndroidTasksProvider()

            if (androidPluginVersion != null) {
                val minimalVersion = "3.0.0"
                if (compareVersionNumbers(androidPluginVersion, minimalVersion) < 0) {
                    throw IllegalStateException("Kotlin: Unsupported version of com.android.tools.build:gradle plugin: version $minimalVersion or higher should be used with kotlin-android plugin")
                }
            }

            val kotlinTools = KotlinConfigurationTools(
                tasksProvider
            )

            return Android25ProjectHandler(kotlinTools)
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
