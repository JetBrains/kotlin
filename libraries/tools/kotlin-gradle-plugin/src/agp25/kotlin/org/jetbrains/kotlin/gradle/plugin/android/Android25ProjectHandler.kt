package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.*
import com.android.build.gradle.api.*
import com.android.build.gradle.tasks.MergeResources
import com.android.builder.model.SourceProvider
import com.android.ide.common.res2.ResourceSet
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.com.intellij.util.ReflectionUtil
import org.jetbrains.kotlin.gradle.internal.Kapt3GradleSubplugin
import org.jetbrains.kotlin.gradle.internal.Kapt3KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.internal.KaptTask
import org.jetbrains.kotlin.gradle.internal.KaptVariantData
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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

    override fun wireKotlinTasks(project: Project,
                                 androidPlugin: BasePlugin,
                                 androidExt: BaseExtension,
                                 variantData: BaseVariant,
                                 javaTask: AbstractCompile,
                                 kotlinTask: KotlinCompile,
                                 kotlinAfterJavaTask: KotlinCompile?) {

        val preJavaKotlinOutputFiles = mutableListOf<File>().apply {
            if (kotlinAfterJavaTask == null) {
                add(kotlinTask.destinationDir)
            }
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

        // Use kapt1 annotations file for up-to-date check since annotation processing is done with javac
        kotlinTask.kaptOptions.annotationsFile?.let { javaTask.inputs.file(it) }

        if (kotlinAfterJavaTask != null) {
            val kotlinAfterJavaOutput = project.files(kotlinAfterJavaTask.destinationDir).builtBy(kotlinAfterJavaTask)
            variantData.registerPostJavacGeneratedBytecode(kotlinAfterJavaOutput)

            // Then we don't need the kotlinTask output in artifacts, but we need to use it for Java compilation.
            // Add it to Java classpath -- note the `from` used to avoid accident classpath resolution.
            javaTask.classpath = project.files(kotlinTask.destinationDir).from(javaTask.classpath)
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

    override fun configureMultiProjectIc(project: Project,
                                         variantData: BaseVariant,
                                         javaTask: AbstractCompile,
                                         kotlinTask: KotlinCompile,
                                         kotlinAfterJavaTask: KotlinCompile?) {
        //todo: No easy solution because of the absence of the output information in library modules
        // Though it is affordable not to implement this for the first previews, because the impact is tolerable
        // to some degree -- the dependent projects will rebuild non-incrementally when a library project changes
    }

    override fun getResDirectories(variantData: BaseVariant): List<File> {
        return variantData.mergeResources?.computeResourceSetList0()?.flatMap { it.sourceFiles } ?: emptyList()
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
    }

    //TODO once the Android plugin reaches its 3.0.0 release, consider compiling against it (remove the reflective call)
    //TODO this is a private API for now
    private fun MergeResources.computeResourceSetList0(): List<ResourceSet>? {
        val computeResourceSetListMethod = MergeResources::class.java.declaredMethods
                .firstOrNull { it.name == "computeResourceSetList" && it.parameterCount == 0 } ?: return null

        val oldIsAccessible = computeResourceSetListMethod.isAccessible
        try {
            computeResourceSetListMethod.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            return computeResourceSetListMethod.invoke(this) as? List<ResourceSet>
        } finally {
            computeResourceSetListMethod.isAccessible = oldIsAccessible
        }
    }

    //TODO once the Android plugin reaches its 3.0.0 release, consider compiling against it (remove the reflective call)
    private val BaseVariant.dataBindingDependencyArtifactsIfSupported: FileCollection?
        get() = ReflectionUtil.findMethod(
                this::class.java.methods.toList(),
                "getDataBindingDependencyArtifacts")?.invoke(this) as? FileCollection

    override fun wrapVariantDataForKapt(variantData: BaseVariant): KaptVariantData<BaseVariant> =
            KaptVariant(variantData)
}