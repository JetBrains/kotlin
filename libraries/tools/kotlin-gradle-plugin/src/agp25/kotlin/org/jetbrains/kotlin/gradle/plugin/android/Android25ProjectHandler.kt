package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.SourceKind
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.android.builder.model.SourceProvider
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.internal.Kapt3KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.internal.WrappedVariantData
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

@Suppress("unused")
class Android25ProjectHandler(kotlinConfigurationTools: KotlinConfigurationTools)
    : AbstractAndroidProjectHandler<BaseVariant>(kotlinConfigurationTools) {

    override fun forEachVariant(project: Project, action: (BaseVariant) -> Unit) {
        project.plugins.all { plugin ->
            val androidExtension = project.extensions.getByName("android")
            var testedExtension: TestedExtension? = null
            when (plugin.javaClass) {
                AppPlugin::class.java -> {
                    (androidExtension as AppExtension).applicationVariants.all(action)
                    testedExtension = androidExtension
                }
                LibraryPlugin::class.java -> {
                    (androidExtension as LibraryExtension).libraryVariants.all(action)
                    testedExtension = androidExtension
                }
                TestPlugin::class.java -> {
                    (androidExtension as TestExtension).applicationVariants.all(action)
                }
            }
            testedExtension?.apply {
                testVariants.all(action)
                unitTestVariants.all(action)
            }
        }
    }

    override fun wireKotlinTasks(project: Project,
                                 androidPlugin: BasePlugin,
                                 androidExt: BaseExtension,
                                 variantData: BaseVariant,
                                 javaTask: AbstractCompile,
                                 kotlinTask: KotlinCompile,
                                 kotlinAfterJavaTask: KotlinCompile?) {

        val preJavaKotlinOutput =
                (if (kotlinAfterJavaTask == null)
                    project.files(kotlinTask.destinationDir).let { kotlinOutput ->
                        if (Kapt3GradleSubplugin.isEnabled(project))
                            // Add Kapt3 output as well, since there's no SyncOutputTask with the new API
                            kotlinOutput.from(Kapt3KotlinGradleSubplugin.getKaptClasssesDir(project, getVariantName(variantData)))
                        else
                            kotlinOutput
                    }
                else
                    // Don't register the output, but add the task to the pipeline
                    project.files()
                ).builtBy(kotlinTask)


        val preJavaClasspathKey = variantData.registerPreJavacGeneratedBytecode(preJavaKotlinOutput)
        kotlinTask.dependsOn(variantData.getSourceFolders(SourceKind.JAVA))

        kotlinTask.conventionMapping.map("classpath") {
            val kotlinClasspath = variantData.getCompileClasspath(preJavaClasspathKey)
            kotlinClasspath + project.files(AndroidGradleWrapper.getRuntimeJars(androidPlugin, androidExt))
        }

        kotlinTask.setJavaOutput(javaTask.destinationDir)
        kotlinAfterJavaTask?.setJavaOutput(javaTask.destinationDir)

        // Use kapt1 annotations file for up-to-date check since annotation processing is done with javac
        kotlinTask.annotationsFile?.let { javaTask.inputs.file(it) }

        if (kotlinAfterJavaTask != null) {
            val kotlinAfterJavaOutput = project.files(kotlinAfterJavaTask.destinationDir).builtBy(kotlinAfterJavaTask)
            variantData.registerPostJavacGeneratedBytecode(kotlinAfterJavaOutput)

            // Then don't register kotlinTask output, but only use it for Java compilation
            javaTask.classpath = project.files(kotlinTask.destinationDir).from(javaTask.classpath)
        }
    }

    override fun getSourceProviders(variantData: BaseVariant): Iterable<SourceProvider> =
            variantData.sourceSets

    override fun getAllJavaSources(variantData: BaseVariant): Iterable<File> =
            variantData.getSourceFolders(SourceKind.JAVA).map { it.dir }

    override fun getVariantName(variant: BaseVariant): String = variant.name

    override fun checkVariant(variant: BaseVariant) = Unit

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

    override fun configureMultiProjectIc(project: Project,
                                         variantData: BaseVariant,
                                         javaTask: AbstractCompile,
                                         kotlinTask: KotlinCompile,
                                         kotlinAfterJavaTask: KotlinCompile?) {
        //todo: No easy solution because of the absence of the output information in library modules
        // Though it is affordable not to implement this for the first previews, because the impact is tolerable
        // to some degree -- the dependent projects will rebuild non-incrementally when a library project changes
    }

    private inner class WrappedVariant(variantData: BaseVariant) : org.jetbrains.kotlin.gradle.internal.WrappedVariantData<BaseVariant>(variantData) {
        override val name: String = getVariantName(variantData)
        override val sourceProviders: Iterable<SourceProvider> = getSourceProviders(variantData)
        override fun addJavaSourceFoldersToModel(generatedFilesDir: File) = addJavaSourceDirectoryToVariantModel(variantData, generatedFilesDir)

        override val annotationProcessorOptions: Map<String, String>? = variantData.javaCompileOptions.annotationProcessorOptions.arguments

        override fun wireKaptTask(project: Project, task: KaptTask, kotlinTask: KotlinCompile, javaTask: AbstractCompile) {
            task.dependsOn(kotlinTask.dependsOn.minus(task))

            val kaptSourceOutput = project.fileTree(task.destinationDir).builtBy(task)
            variantData.registerExternalAptJavaOutput(kaptSourceOutput)
        }
    }

    override fun wrapVariantData(variantData: BaseVariant): WrappedVariantData<BaseVariant> = WrappedVariant(variantData)
}