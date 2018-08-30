package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.*
import com.android.build.gradle.api.*
import com.android.build.gradle.tasks.MergeResources
import com.android.builder.model.SourceProvider
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.internal.KaptVariantData
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import java.io.File

@Suppress("unused")
class Android25ProjectHandler(kotlinConfigurationTools: KotlinConfigurationTools)
    : AbstractAndroidProjectHandler<BaseVariant>(kotlinConfigurationTools) {

    override fun forEachVariant(project: Project, action: (BaseVariant) -> Unit) {
        val androidExtension = project.extensions.getByName("android")
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

    override fun wireKotlinTasks(
        project: Project,
        compilation: KotlinJvmAndroidCompilation,
        androidPlugin: BasePlugin,
        androidExt: BaseExtension,
        variantData: BaseVariant,
        javaTask: AbstractCompile,
        kotlinTask: KotlinCompile
    ) {
        val preJavaKotlinOutputFiles = mutableListOf<File>().apply {
            add(kotlinTask.destinationDir)
            if (Kapt3GradleSubplugin.isEnabled(project)) {
                // Add Kapt3 output as well, since there's no SyncOutputTask with the new API
                val kaptClasssesDir = Kapt3GradleSubplugin.getKaptGeneratedClassesDir(project, getVariantName(variantData))
                add(kaptClasssesDir)
            }
        }
        val preJavaKotlinOutput = project.files(*preJavaKotlinOutputFiles.toTypedArray()).builtBy(kotlinTask)

        val preJavaClasspathKey = variantData.registerPreJavacGeneratedBytecode(preJavaKotlinOutput)
        kotlinTask.dependsOn(variantData.getSourceFolders(SourceKind.JAVA))

        kotlinTask.mapClasspath {
            val kotlinClasspath = variantData.getCompileClasspath(preJavaClasspathKey)
            kotlinClasspath + project.files(AndroidGradleWrapper.getRuntimeJars(androidPlugin, androidExt))
        }

        // Find the classpath entries that comes from the tested variant and register it as the friend path, lazily
        kotlinTask.friendPaths = lazy {
            variantData.getCompileClasspathArtifacts(preJavaClasspathKey)
                    .filter { it.id.componentIdentifier is TestedComponentIdentifier }
                    .map { it.file.absolutePath }
                    .toTypedArray()
        }
    }

    override fun getSourceProviders(variantData: BaseVariant): Iterable<SourceProvider> =
            variantData.sourceSets

    override fun getAllJavaSources(variantData: BaseVariant): Iterable<File> =
            variantData.getSourceFolders(SourceKind.JAVA).map { it.dir }

    override fun getVariantName(variant: BaseVariant): String = variant.name

    override fun getTestedVariantData(variantData: BaseVariant): BaseVariant? = when (variantData) {
        is TestVariant -> variantData.testedVariant
        is UnitTestVariant -> variantData.testedVariant as? BaseVariant
        else -> null
    }

    override fun getJavaTask(variantData: BaseVariant): AbstractCompile? =
            @Suppress("DEPRECATION") // There is always a Java compile task -- the deprecation was for Jack
            variantData.javaCompile

    override fun addJavaSourceDirectoryToVariantModel(variantData: BaseVariant, javaSourceDirectory: File) =
            variantData.addJavaSourceFoldersToModel(javaSourceDirectory)

    override fun getResDirectories(variantData: BaseVariant): List<File> {
        val getAllResourcesMethod =
            variantData::class.java.methods.firstOrNull { it.name == "getAllRawAndroidResources" }
        if (getAllResourcesMethod != null) {
            val allResources = getAllResourcesMethod.invoke(variantData) as FileCollection
            return allResources.files.toList()
        }

        return variantData.mergeResources?.computeResourceSetList0() ?: emptyList()
    }

    override fun setUpDependencyResolution(variant: BaseVariant, compilation: KotlinJvmAndroidCompilation) {
        val project = compilation.target.project

        AbstractKotlinTargetConfigurator.defineConfigurationsForCompilation(compilation, compilation.target, project.configurations)

        compilation.compileDependencyFiles = variant.compileConfiguration.apply {
            usesPlatformOf(compilation.target)
            project.addExtendsFromRelation(name, compilation.compileDependencyConfigurationName)
        }

        compilation.runtimeDependencyFiles = variant.runtimeConfiguration.apply {
            usesPlatformOf(compilation.target)
            project.addExtendsFromRelation(name, compilation.runtimeDependencyConfigurationName)
        }

        // TODO this code depends on the convention that is present in the Android plugin as there's no public API
        // We should request such API in the Android plugin
        val apiElementsConfigurationName = "${variant.name}ApiElements"
        val runtimeElementsConfigurationName = "${variant.name}RuntimeElements"
        listOf(apiElementsConfigurationName, runtimeElementsConfigurationName).forEach { outputConfigurationName ->
            project.configurations.findByName(outputConfigurationName)?.usesPlatformOf(compilation.target)
        }
    }

    private inner class KaptVariant(variantData: BaseVariant) : KaptVariantData<BaseVariant>(variantData) {
        override val name: String = getVariantName(variantData)
        override val sourceProviders: Iterable<SourceProvider> = getSourceProviders(variantData)
        override fun addJavaSourceFoldersToModel(generatedFilesDir: File) =
                addJavaSourceDirectoryToVariantModel(variantData, generatedFilesDir)

        override val annotationProcessorOptions: Map<String, String>? =
                variantData.javaCompileOptions.annotationProcessorOptions.arguments

        override fun registerGeneratedJavaSource(
            project: Project,
            kaptTask: KaptTask,
            javaTask: AbstractCompile
        ) {
            val kaptSourceOutput = project.fileTree(kaptTask.destinationDir).builtBy(kaptTask)
            variantData.registerExternalAptJavaOutput(kaptSourceOutput)
            variantData.dataBindingDependencyArtifactsIfSupported?.let { kaptTask.dependsOn(it) }
        }

        override val annotationProcessorOptionProviders: List<*>
            get() = try {
                // Public API added in Android Gradle Plugin 3.2.0-alpha15:
                val apOptions = variantData.javaCompileOptions.annotationProcessorOptions
                apOptions.javaClass.getMethod("getCompilerArgumentProviders").invoke(apOptions) as List<*>
            } catch (e: NoSuchMethodException) {
                emptyList<Any>()
            }
    }

    //TODO A public API is expected for this purpose. Once it is available, use the public API
    private fun MergeResources.computeResourceSetList0(): List<File>? {
        val computeResourceSetListMethod = MergeResources::class.java.declaredMethods
                .firstOrNull { it.name == "computeResourceSetList" && it.parameterCount == 0 } ?: return null

        val oldIsAccessible = computeResourceSetListMethod.isAccessible
        try {
            computeResourceSetListMethod.isAccessible = true

            val resourceSets = computeResourceSetListMethod.invoke(this) as? Iterable<*>

            return resourceSets
                ?.mapNotNull { resourceSet ->
                    val getSourceFiles = resourceSet?.javaClass?.methods?.find { it.name == "getSourceFiles" && it.parameterCount == 0 }
                    val files = getSourceFiles?.invoke(resourceSet)
                    @Suppress("UNCHECKED_CAST")
                    files as? Iterable<File>
                }
                ?.flatten()

        } finally {
            computeResourceSetListMethod.isAccessible = oldIsAccessible
        }
    }

    //TODO once the Android plugin reaches its 3.0.0 release, consider compiling against it (remove the reflective call)
    private val BaseVariant.dataBindingDependencyArtifactsIfSupported: FileCollection?
        get() = this::class.java.methods
            .find { it.name == "getDataBindingDependencyArtifacts" }
            ?.also { it.isAccessible = true }
            ?.invoke(this) as? FileCollection

    override fun wrapVariantDataForKapt(variantData: BaseVariant): KaptVariantData<BaseVariant> =
            KaptVariant(variantData)
}