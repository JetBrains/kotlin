package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.build.gradle.internal.variant.BaseVariantOutputData
import com.android.build.gradle.internal.variant.LibraryVariantData
import com.android.build.gradle.internal.variant.TestVariantData
import com.android.builder.model.SourceProvider
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.internal.KaptVariantData
import org.jetbrains.kotlin.gradle.internal.registerGeneratedJavaSource
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.checkedReflection
import org.jetbrains.kotlin.incremental.configureMultiProjectIncrementalCompilation
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifferenceRegistryProviderAndroidWrapper
import java.io.File

internal class LegacyAndroidAndroidProjectHandler(kotlinConfigurationTools: KotlinConfigurationTools)
    : AbstractAndroidProjectHandler<BaseVariantData<out BaseVariantOutputData>>(kotlinConfigurationTools) {

    override fun getSourceProviders(variantData: BaseVariantData<out BaseVariantOutputData>): Iterable<SourceProvider> =
            variantData.sourceProviders

    override fun getAllJavaSources(variantData: BaseVariantData<out BaseVariantOutputData>): Iterable<File> =
            AndroidGradleWrapper.getJavaSources(variantData)

    override fun forEachVariant(project: Project, action: (BaseVariantData<out BaseVariantOutputData>) -> Unit) {
        val plugin = (project.plugins.findPlugin("android")
                      ?: project.plugins.findPlugin("android-library")
                      ?: project.plugins.findPlugin("com.android.test")) as BasePlugin
        val variantManager = AndroidGradleWrapper.getVariantDataManager(plugin)
        variantManager.variantDataList.forEach(action)
    }

    override fun wireKotlinTasks(project: Project,
                                 androidPlugin: BasePlugin,
                                 androidExt: BaseExtension,
                                 variantData: BaseVariantData<out BaseVariantOutputData>,
                                 javaTask: AbstractCompile,
                                 kotlinTask: KotlinCompile,
                                 kotlinAfterJavaTask: KotlinCompile?
    ) {
        kotlinTask.dependsOn(*javaTask.dependsOn.toTypedArray())

        kotlinTask.mapClasspath {
            javaTask.classpath + project.files(AndroidGradleWrapper.getRuntimeJars(androidPlugin, androidExt))
        }

        getTestedVariantData(variantData)?.let { testedVariantData ->
            // Android Gradle plugin bypasses the Gradle finalizedBy for its tasks in some cases, and
            // the Kotlin classes may not be copied for the tested variant. Make sure they are.
            kotlinTask.dependsOn(syncOutputTaskName(getVariantName(testedVariantData)))
        }

        configureJavaTask(kotlinTask, javaTask, logger)
        createSyncOutputTask(project, kotlinTask, javaTask, kotlinAfterJavaTask, getVariantName(variantData))

        // In lib modules, the androidTest variants get the classes jar in their classpath instead of the Java
        // destination dir. To use it as a friend path, set the jar as the javaOutputDir (see its usages):
        if (variantData is LibraryVariantData) {
            variantData.dependencyJarOrNull?.let { kotlinTask.javaOutputDir = it }
        }
    }

    private val LibraryVariantData.dependencyJarOrNull: File?
        get() =
            checkedReflection(f@{
                val output = variantConfiguration.javaClass.methods.firstOrNull { it.name == "getOutput" }
                        ?.invoke(variantConfiguration)
                        ?: return@f null

                output.javaClass.methods?.firstOrNull { it.name == "getJarFile" }
                        ?.invoke(output) as? File

            }, { e: Exception ->
                logger.kotlinDebug("dependencyJarOrNull for lib variant $name failed due to $e")
                null
            })

    override fun getVariantName(variant: BaseVariantData<out BaseVariantOutputData>): String = variant.name

    override fun checkVariantIsValid(variant: BaseVariantData<out BaseVariantOutputData>): Unit {
        if (AndroidGradleWrapper.isJackEnabled(variant)) {
            throw ProjectConfigurationException(
                    "Kotlin Gradle plugin does not support the deprecated Jack toolchain.\n" +
                    "Disable Jack or revert to Kotlin Gradle plugin version 1.1.1.", null)
        }
    }

    override fun getJavaTask(variantData: BaseVariantData<out BaseVariantOutputData>): AbstractCompile? =
            AndroidGradleWrapper.getJavaTask(variantData)

    override fun addJavaSourceDirectoryToVariantModel(variantData: BaseVariantData<out BaseVariantOutputData>,
                                                      javaSourceDirectory: File) =
            variantData.addJavaSourceFoldersToModel(javaSourceDirectory)

    override fun configureMultiProjectIc(project: Project, variantData: BaseVariantData<out BaseVariantOutputData>, javaTask: AbstractCompile, kotlinTask: KotlinCompile, kotlinAfterJavaTask: KotlinCompile?) {
        if ((kotlinAfterJavaTask ?: kotlinTask).incremental) {
            val artifactFile = project.tryGetSingleArtifact(variantData)
            val artifactDifferenceRegistryProvider = ArtifactDifferenceRegistryProviderAndroidWrapper(
                    artifactDifferenceRegistryProvider,
                    { AndroidGradleWrapper.getJarToAarMapping(variantData) }
            )
            configureMultiProjectIncrementalCompilation(project, kotlinTask, javaTask, kotlinAfterJavaTask,
                                                        artifactDifferenceRegistryProvider, artifactFile)
        }
    }

    override fun getTestedVariantData(variantData: BaseVariantData<*>): BaseVariantData<*>? =
            ((variantData as? TestVariantData)?.testedVariantData as? BaseVariantData<*>)

    override fun getResDirectories(variantData: BaseVariantData<out BaseVariantOutputData>): List<File> {
        return variantData.mergeResourcesTask?.rawInputFolders?.toList() ?: emptyList()
    }

    private fun Project.tryGetSingleArtifact(variantData: BaseVariantData<*>): File? {
        val log = logger
        log.kotlinDebug { "Trying to determine single artifact for project $path" }

        val outputs = variantData.outputs
        if (outputs.size != 1) {
            log.kotlinDebug { "Output count != 1 for variant: ${outputs.map { it.outputFile.relativeTo(rootDir).path }.joinToString()}" }
            return null
        }

        return variantData.outputs.first().outputFile
    }

    private val BaseVariantData<*>.sourceProviders: List<SourceProvider>
        get() = variantConfiguration.sortedSourceProviders

    private inner class KaptLegacyVariantData(variantData: BaseVariantData<out BaseVariantOutputData>)
        : KaptVariantData<BaseVariantData<out BaseVariantOutputData>>(variantData) {

        override val name: String = variantData.name
        override val sourceProviders: Iterable<SourceProvider> = getSourceProviders(variantData)
        override fun addJavaSourceFoldersToModel(generatedFilesDir: File) =
                addJavaSourceDirectoryToVariantModel(variantData, generatedFilesDir)

        override val annotationProcessorOptions: Map<String, String>? =
                AndroidGradleWrapper.getAnnotationProcessorOptionsFromAndroidVariant(variantData)

        override fun registerGeneratedJavaSource(
            project: Project,
            kaptTask: KaptTask,
            javaTask: AbstractCompile
        ) {
            registerGeneratedJavaSource(kaptTask, javaTask)
        }
    }

    override fun wrapVariantDataForKapt(variantData: BaseVariantData<out BaseVariantOutputData>)
            : KaptVariantData<BaseVariantData<out BaseVariantOutputData>> =
            KaptLegacyVariantData(variantData)
}