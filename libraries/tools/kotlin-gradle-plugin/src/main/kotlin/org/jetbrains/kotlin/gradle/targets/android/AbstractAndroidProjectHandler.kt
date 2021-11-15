/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.android

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.api.AndroidSourceSet
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.SourceKind
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.InvalidPluginException
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptionsImpl
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.internal.checkAndroidAnnotationProcessorDependencyUsage
import org.jetbrains.kotlin.gradle.logging.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.syncKotlinAndAndroidSourceSets
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tooling.includeKotlinToolingMetadataInApk
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.androidPluginIds
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import java.net.URL
import java.util.jar.Manifest

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
        androidPlugin: BasePlugin<*>,
        androidExt: BaseExtension,
        variantData: BaseVariant,
        javaTask: TaskProvider<out AbstractCompile>,
        kotlinTask: TaskProvider<out KotlinCompile>
    )

    fun configureTarget(kotlinAndroidTarget: KotlinAndroidTarget) {
        syncKotlinAndAndroidSourceSets(kotlinAndroidTarget)

        val project = kotlinAndroidTarget.project
        val ext = project.extensions.getByName("android") as BaseExtension

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

        val plugin = androidPluginIds
            .asSequence()
            .mapNotNull { project.plugins.findPlugin(it) as? BasePlugin<*> }
            .firstOrNull()
            ?: throw InvalidPluginException("'kotlin-android' expects one of the Android Gradle " +
                                                    "plugins to be applied to the project:\n\t" +
                                                    androidPluginIds.joinToString("\n\t") { "* $it" })

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

                val subpluginEnvironment = SubpluginEnvironment.loadSubplugins(project)
                subpluginEnvironment.addSubpluginOptions(project, compilation)
            }
            checkAndroidAnnotationProcessorDependencyUsage(project)

            addKotlinDependenciesToAndroidSourceSets(project, kotlinAndroidTarget)
        }

        project.includeKotlinToolingMetadataInApk()
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
            minOf(baseExtension.compileOptions.sourceCompatibility, baseExtension.compileOptions.targetCompatibility)
        if (javaVersion == JavaVersion.VERSION_1_6)
            kotlinOptions.jvmTarget = "1.6"
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

        tasksProvider.registerKotlinJVMTask(project, kotlinTaskName, compilation) {
            it.parentKotlinOptionsImpl.set(rootKotlinOptions)

            // store kotlin classes in separate directory. They will serve as class-path to java compiler
            it.destinationDirectory.set(project.layout.buildDirectory.dir("tmp/kotlin-classes/$variantDataName"))
            it.description = "Compiles the $variantDataName kotlin."
        }

        // Register the source only after the task is created, because the task is required for that:
        compilation.source(defaultSourceSet)

        compilation.androidVariant.forEachKotlinSourceSet { kotlinSourceSet -> compilation.source(kotlinSourceSet) }
    }

    private fun postprocessVariant(
        variantData: BaseVariant,
        compilation: KotlinJvmAndroidCompilation,
        project: Project,
        androidExt: BaseExtension,
        androidPlugin: BasePlugin<*>
    ) {

        getTestedVariantData(variantData)?.let { testedVariant ->
            val testedVariantName = getVariantName(testedVariant)
            val testedCompilation = compilation.target.compilations.getByName(testedVariantName)
            compilation.associateWith(testedCompilation)
        }

        val javaTask = variantData.getJavaTaskProvider()
        val kotlinTask = compilation.compileKotlinTaskProvider
        compilation.androidVariant.forEachJavaSourceDir { sources -> kotlinTask.configure { it.source(sources.dir) } }
        wireKotlinTasks(project, compilation, androidPlugin, androidExt, variantData, javaTask, kotlinTask)
    }
}

internal inline fun BaseVariant.forEachKotlinSourceSet(action: (KotlinSourceSet) -> Unit) {
    sourceSets
        .mapNotNull { provider -> provider.getConvention(KOTLIN_DSL_NAME) as? KotlinSourceSet }
        .forEach(action)
}

internal inline fun BaseVariant.forEachJavaSourceDir(action: (ConfigurableFileTree) -> Unit) {
    getSourceFolders(SourceKind.JAVA).forEach(action)
}


//copied from BasePlugin.getLocalVersion
internal val androidPluginVersion by lazy {
    try {
        val clazz = BasePlugin::class.java
        val className = clazz.simpleName + ".class"
        val classPath = clazz.getResource(className).toString()
        if (!classPath.startsWith("jar")) {
            // Class not from JAR, unlikely
            return@lazy null
        }
        val manifestPath = classPath.substring(0, classPath.lastIndexOf("!") + 1) + "/META-INF/MANIFEST.MF"

        val jarConnection = URL(manifestPath).openConnection()
        jarConnection.useCaches = false
        val jarInputStream = jarConnection.inputStream
        val attr = Manifest(jarInputStream).mainAttributes
        jarInputStream.close()
        return@lazy attr.getValue("Plugin-Version")
    } catch (t: Throwable) {
        return@lazy null
    }
}
