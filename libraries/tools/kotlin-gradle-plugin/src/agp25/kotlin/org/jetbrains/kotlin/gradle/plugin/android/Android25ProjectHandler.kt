package org.jetbrains.kotlin.gradle.plugin

import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.SourceKind
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.android.builder.model.SourceProvider
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
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
}