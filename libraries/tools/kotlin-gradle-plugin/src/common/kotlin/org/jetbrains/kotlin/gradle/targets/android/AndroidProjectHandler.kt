/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin

import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.gradle.*
import com.android.build.gradle.api.*
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.InvalidPluginException
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.internal.checkAndroidAnnotationProcessorDependencyUsage
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.sources.android.KotlinAndroidSourceSets.applyKotlinAndroidSourceSetLayout
import org.jetbrains.kotlin.gradle.plugin.sources.android.findKotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.configuration.KotlinCompileConfig
import org.jetbrains.kotlin.gradle.tasks.thisTaskProvider
import org.jetbrains.kotlin.gradle.testing.internal.kotlinTestRegistry
import org.jetbrains.kotlin.gradle.tooling.includeKotlinToolingMetadataInApk
import org.jetbrains.kotlin.gradle.utils.*
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.concurrent.Callable

internal class AndroidProjectHandler(
    private val kotlinTasksProvider: KotlinTasksProvider
) {
    private val logger = Logging.getLogger(this.javaClass)

    fun configureTarget(kotlinAndroidTarget: KotlinAndroidTarget) {
        val project = kotlinAndroidTarget.project
        val ext = project.extensions.getByName("android") as BaseExtension

        applyKotlinAndroidSourceSetLayout(kotlinAndroidTarget)

        val plugin = androidPluginIds
            .asSequence()
            .mapNotNull { project.plugins.findPlugin(it) as? BasePlugin }
            .firstOrNull()
            ?: throw InvalidPluginException("'kotlin-android' expects one of the Android Gradle " +
                                                    "plugins to be applied to the project:\n\t" +
                                                    androidPluginIds.joinToString("\n\t") { "* $it" })

        project.forAllAndroidVariants { variant ->
            val compilationFactory = KotlinJvmAndroidCompilationFactory(kotlinAndroidTarget, variant)
            val variantName = getVariantName(variant)

            // Create the compilation and configure it first, then add to the compilations container. As this code is executed
            // in afterEvaluate, a user's build script might have already attached item handlers to the compilations container, and those
            // handlers might break when fired on a compilation that is not yet properly configured (e.g. KT-29964):
            compilationFactory.create(variantName).let { compilation ->
                setUpDependencyResolution(variant, compilation)
                preprocessVariant(variant, compilation, project, kotlinTasksProvider)

                @Suppress("UNCHECKED_CAST")
                (kotlinAndroidTarget.compilations as NamedDomainObjectCollection<in KotlinJvmAndroidCompilation>).add(compilation)
            }

        }

        project.whenEvaluated {
            forAllAndroidVariants { variant ->
                val compilation = kotlinAndroidTarget.compilations.getByName(getVariantName(variant))
                postprocessVariant(variant, compilation, project, ext, plugin)

                val subpluginEnvironment = SubpluginEnvironment.loadSubplugins(project)
                subpluginEnvironment.addSubpluginOptions(project, compilation)
            }
            checkAndroidAnnotationProcessorDependencyUsage(project)

            addKotlinDependenciesToAndroidSourceSets(project)
        }

        project.includeKotlinToolingMetadataInApk()

        addAndroidUnitTestTasksAsDependenciesToAllTest(project)
    }

    /**
     * The Android variants have their configurations extendsFrom relation set up in a way that only some of the configurations of the
     * variants propagate the dependencies from production variants to test ones. To make this dependency propagation work for the Kotlin
     * source set dependencies as well, we need to add them to the Android source sets' api/implementation-like configurations,
     * not just the classpath-like configurations of the variants.
     */
    private fun addKotlinDependenciesToAndroidSourceSets(project: Project) {
        fun addDependenciesToAndroidSourceSet(
            androidSourceSet: AndroidSourceSet,
            apiConfigurationName: String,
            implementationConfigurationName: String,
            compileOnlyConfigurationName: String,
            runtimeOnlyConfigurationName: String
        ) {
            if (project.configurations.findByName(androidSourceSet.apiConfigurationName) != null) {
                project.addExtendsFromRelation(androidSourceSet.apiConfigurationName, apiConfigurationName)
            } else {
                // If any dependency is added to this configuration, report an error:
                project.configurations.getByName(apiConfigurationName).dependencies.all {
                    throw InvalidUserCodeException(
                        "API dependencies are not allowed for Android source set ${androidSourceSet.name}. " +
                                "Please use an implementation dependency instead."
                    )
                }
            }
            project.addExtendsFromRelation(androidSourceSet.implementationConfigurationName, implementationConfigurationName)
            project.addExtendsFromRelation(androidSourceSet.compileOnlyConfigurationName, compileOnlyConfigurationName)
            project.addExtendsFromRelation(androidSourceSet.runtimeOnlyConfigurationName, runtimeOnlyConfigurationName)
        }

        /** First, just add the dependencies from Kotlin source sets created for the Android source sets,
         * see [org.jetbrains.kotlin.gradle.plugin.AndroidProjectHandler.configureTarget]
         */
        (project.extensions.getByName("android") as BaseExtension).sourceSets.forEach { androidSourceSet ->
            project.findKotlinSourceSet(androidSourceSet)?.let { kotlinSourceSet ->
                addDependenciesToAndroidSourceSet(
                    androidSourceSet,
                    kotlinSourceSet.apiConfigurationName,
                    kotlinSourceSet.implementationConfigurationName,
                    kotlinSourceSet.compileOnlyConfigurationName,
                    kotlinSourceSet.runtimeOnlyConfigurationName
                )
            }
        }
    }

    private fun addAndroidUnitTestTasksAsDependenciesToAllTest(project: Project) {
        val allTestTaskName = project.kotlinTestRegistry.allTestsTaskName
        project.tasks.matching { it.name == allTestTaskName }.configureEach { task ->
            task.dependsOn(project.provider {
                val androidUnitTestTasks = mutableListOf<Any>()
                project.forAllAndroidVariants { variant ->
                    if (variant is UnitTestVariant) {
                        // There's no API for getting the Android unit test tasks from the variant, so match them by name:
                        androidUnitTestTasks.add(project.provider {
                            project.tasks.matching { it.name == lowerCamelCaseName("test", variant.name) }
                        })
                    }
                }
                androidUnitTestTasks
            })
        }
    }

    private fun preprocessVariant(
        variantData: BaseVariant,
        compilation: KotlinJvmAndroidCompilation,
        project: Project,
        tasksProvider: KotlinTasksProvider
    ) {
        // This function is called before the variantData is completely filled by the Android plugin.
        // The fine details of variantData, such as AP options or Java sources, should not be trusted here.
        val variantDataName = getVariantName(variantData)
        logger.kotlinDebug("Process variant [$variantDataName]")

        val defaultSourceSet = project.kotlinExtension.sourceSets.maybeCreate(compilation.defaultSourceSetName)

        val configAction = KotlinCompileConfig(KotlinCompilationInfo(compilation))
        configAction.configureTask { task ->
            task.useModuleDetection.value(true).disallowChanges()
            // store kotlin classes in separate directory. They will serve as class-path to java compiler
            task.destinationDirectory.set(project.layout.buildDirectory.dir("tmp/kotlin-classes/$variantDataName"))
            task.description = "Compiles the $variantDataName kotlin."
        }
        tasksProvider.registerKotlinJVMTask(
            project,
            compilation.compileKotlinTaskName,
            compilation.compilerOptions.options as KotlinJvmCompilerOptions,
            configAction
        )

        // Register the source only after the task is created, because the task is required for that:
        compilation.source(defaultSourceSet)

        compilation.androidVariant.forEachKotlinSourceSet(project) { kotlinSourceSet ->
            compilation.source(kotlinSourceSet)
        }
    }

    private fun postprocessVariant(
        variantData: BaseVariant,
        compilation: KotlinJvmAndroidCompilation,
        project: Project,
        androidExt: BaseExtension,
        androidPlugin: BasePlugin
    ) {

        getTestedVariantData(variantData)?.let { testedVariant ->
            val testedVariantName = getVariantName(testedVariant)
            val testedCompilation = compilation.target.compilations.getByName(testedVariantName)
            compilation.associateWith(testedCompilation)
        }

        val javaTask = variantData.getJavaTaskProvider()
        val kotlinTask = compilation.compileKotlinTaskProvider
        compilation.androidVariant.forEachJavaSourceDir { sources ->
            kotlinTask.configure {
                it.setSource(sources.dir)
                it.dependsOn(sources)
            }
        }
        wireKotlinTasks(project, compilation, androidPlugin, androidExt, variantData, javaTask, kotlinTask)
    }

    private fun wireKotlinTasks(
        project: Project,
        compilation: KotlinJvmAndroidCompilation,
        androidPlugin: BasePlugin,
        androidExt: BaseExtension,
        variantData: BaseVariant,
        javaTask: TaskProvider<out AbstractCompile>,
        kotlinTask: TaskProvider<out KotlinCompile>
    ) {
        val preJavaKotlinOutput = project.files(project.provider {
            mutableListOf<File>().apply {
                add(kotlinTask.get().destinationDirectory.get().asFile)
                if (Kapt3GradleSubplugin.isEnabled(project)) {
                    // Add Kapt3 output as well, since there's no SyncOutputTask with the new API
                    val kaptClasssesDir = Kapt3GradleSubplugin.getKaptGeneratedClassesDir(project, getVariantName(variantData))
                    add(kaptClasssesDir)
                }
            }
        }).builtBy(kotlinTask)

        val preJavaClasspathKey = variantData.registerPreJavacGeneratedBytecode(preJavaKotlinOutput)
        kotlinTask.configure { kotlinTaskInstance ->
            kotlinTaskInstance.libraries
                .from(variantData.getCompileClasspath(preJavaClasspathKey))
                .from(Callable { AndroidGradleWrapper.getRuntimeJars(androidPlugin, androidExt) })

            kotlinTaskInstance.javaOutputDir.set(javaTask.flatMap { it.destinationDirectory })
        }

        // Find the classpath entries that come from the tested variant and register them as the friend paths, lazily
        val originalArtifactCollection = variantData.getCompileClasspathArtifacts(preJavaClasspathKey)
        val testedVariantDataIsNotNull = getTestedVariantData(variantData) != null
        val projectPath = project.path
        compilation.testedVariantArtifacts.set(
            originalArtifactCollection.artifactFiles.filter(
                AndroidTestedVariantArtifactsFilter(
                    originalArtifactCollection,
                    testedVariantDataIsNotNull,
                    projectPath
                )
            )
        )

        compilation.output.classesDirs.from(
            kotlinTask.flatMap { it.destinationDirectory },
            javaTask.flatMap { it.destinationDirectory }
        )
    }

    fun getFlavorNames(variant: BaseVariant): List<String> = variant.productFlavors.map { it.name }

    fun getBuildTypeName(variant: BaseVariant): String = variant.buildType.name

    // TODO the return type is actually `AbstractArchiveTask | TaskProvider<out AbstractArchiveTask>`;
    //      change the signature once the Android Gradle plugin versions that don't support task providers are dropped
    fun getLibraryOutputTask(variant: BaseVariant): Any? {
        val getPackageLibraryProvider = variant.javaClass.methods
            .find { it.name == "getPackageLibraryProvider" && it.parameterCount == 0 }

        return if (getPackageLibraryProvider != null) {
            @Suppress("UNCHECKED_CAST")
            getPackageLibraryProvider(variant) as TaskProvider<out AbstractArchiveTask>
        } else {
            (variant as? LibraryVariant)?.packageLibrary
        }
    }

    fun setUpDependencyResolution(variant: BaseVariant, compilation: KotlinJvmAndroidCompilation) {
        val project = compilation.target.project

        compilation.compileDependencyFiles = variant.compileConfiguration.apply {
            usesPlatformOf(compilation.target)
            project.addExtendsFromRelation(name, compilation.compileDependencyConfigurationName)
        }

        compilation.runtimeDependencyFiles = variant.runtimeConfiguration.apply {
            usesPlatformOf(compilation.target)
            project.addExtendsFromRelation(name, compilation.runtimeDependencyConfigurationName)
        }

        val buildTypeAttrValue = project.objects.named(BuildTypeAttr::class.java, variant.buildType.name)
        listOf(compilation.compileDependencyConfigurationName, compilation.runtimeDependencyConfigurationName).forEach {
            project.configurations.findByName(it)?.attributes?.attribute(Attribute.of(BuildTypeAttr::class.java), buildTypeAttrValue)
        }

        // TODO this code depends on the convention that is present in the Android plugin as there's no public API
        // We should request such API in the Android plugin
        val apiElementsConfigurationName = "${variant.name}ApiElements"
        val runtimeElementsConfigurationName = "${variant.name}RuntimeElements"

        // KT-29476, the Android *Elements configurations need Kotlin MPP dependencies:
        if (project.configurations.findByName(apiElementsConfigurationName) != null) {
            project.addExtendsFromRelation(apiElementsConfigurationName, compilation.apiConfigurationName)
        }
        if (project.configurations.findByName(runtimeElementsConfigurationName) != null) {
            project.addExtendsFromRelation(runtimeElementsConfigurationName, compilation.implementationConfigurationName)
            project.addExtendsFromRelation(runtimeElementsConfigurationName, compilation.runtimeOnlyConfigurationName)
        }

        listOf(apiElementsConfigurationName, runtimeElementsConfigurationName).forEach { outputConfigurationName ->
            project.configurations.findByName(outputConfigurationName)?.let { configuration ->
                configuration.usesPlatformOf(compilation.target)
                configuration.attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
            }
        }
    }
}

internal fun getTestedVariantData(variantData: BaseVariant): BaseVariant? = when (variantData) {
    is TestVariant -> variantData.testedVariant
    is UnitTestVariant -> variantData.testedVariant as? BaseVariant
    else -> null
}

internal fun getVariantName(variant: BaseVariant): String = variant.name

@Suppress("UNCHECKED_CAST")
internal fun BaseVariant.getJavaTaskProvider(): TaskProvider<out JavaCompile> =
    this::class.java.methods.firstOrNull { it.name == "getJavaCompileProvider" }
        ?.invoke(this) as? TaskProvider<JavaCompile>
        ?: @Suppress("DEPRECATION") javaCompile.thisTaskProvider

/** Filter for the AGP test variant classpath artifacts. */
class AndroidTestedVariantArtifactsFilter(
    private val artifactCollection: ArtifactCollection,
    private val testedVariantDataIsNotNull: Boolean,
    private val projectPath: String
) : Serializable, Spec<File> {

    /** Make transient as it should be derived from the [artifactCollection] property which may change in configuration cached runs. */
    @Transient
    private var filteredFiles = initFilteredFiles()

    private fun initFilteredFiles(): Lazy<Set<File>> {
        return lazy {
            artifactCollection.filter {
                it.id.componentIdentifier is TestedComponentIdentifier ||
                        // If tests depend on the main classes transitively, through a test dependency on another module which
                        // depends on this module, then there's no artifact with a TestedComponentIdentifier, so consider the artifact of the
                        // current module a friend path, too:
                        testedVariantDataIsNotNull &&
                        (it.id.componentIdentifier as? ProjectComponentIdentifier)?.projectPath == projectPath
            }
                .mapTo(mutableSetOf()) { it.file }
        }
    }

    private fun writeObject(objectOutputStream: ObjectOutputStream) {
        objectOutputStream.defaultWriteObject()
    }

    private fun readObject(objectInputStream: ObjectInputStream) {
        objectInputStream.defaultReadObject()
        filteredFiles = initFilteredFiles()
    }

    override fun isSatisfiedBy(element: File): Boolean {
        return element in filteredFiles.value
    }
}

internal inline fun BaseVariant.forEachKotlinSourceSet(
    project: Project, action: (KotlinSourceSet) -> Unit
) {
    sourceSets
        .forEach { provider -> action(project.findKotlinSourceSet(provider) ?: return@forEach) }
}

internal inline fun BaseVariant.forEachKotlinSourceDirectorySet(
    project: Project, action: (SourceDirectorySet) -> Unit
) {
    sourceSets
        .forEach { androidSourceSet -> action(project.findKotlinSourceSet(androidSourceSet)?.kotlin ?: return@forEach) }
}

internal inline fun BaseVariant.forEachJavaSourceDir(action: (ConfigurableFileTree) -> Unit) {
    getSourceFolders(SourceKind.JAVA).forEach(action)
}

