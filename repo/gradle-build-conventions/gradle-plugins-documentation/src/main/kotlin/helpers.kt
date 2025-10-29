import gradle.GradlePluginVariant
import gradle.commonSourceSetName
import gradle.publishGradlePluginsJavadoc
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.dokka.gradle.GradleExternalDocumentationLinkBuilder
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File
import java.net.URI

// Workaround for https://github.com/Kotlin/dokka/issues/2097
// Gradle 7.6 javadoc does not have published 'package-list' file
internal fun GradleExternalDocumentationLinkBuilder.addWorkaroundForElementList(pluginVariant: GradlePluginVariant) {
    if (pluginVariant == GradlePluginVariant.GRADLE_MIN ||
        pluginVariant == GradlePluginVariant.GRADLE_80 ||
        pluginVariant == GradlePluginVariant.GRADLE_81 ||
        pluginVariant == GradlePluginVariant.GRADLE_82 ||
        pluginVariant == GradlePluginVariant.GRADLE_85 ||
        pluginVariant == GradlePluginVariant.GRADLE_86 ||
        pluginVariant == GradlePluginVariant.GRADLE_88 ||
        pluginVariant == GradlePluginVariant.GRADLE_811
    ) {
        packageListUrl.set(URI("${pluginVariant.gradleApiJavadocUrl}element-list").toURL())
    }
}

internal fun Project.generateJavadocForPluginVariant(gradlePluginVariant: GradlePluginVariant) {
    val javaExtension = extensions.getByType<JavaPluginExtension>()
    val commonSourceSet = javaExtension.sourceSets.getByName(commonSourceSetName)
    val variantSourceSet = javaExtension.sourceSets.getByName(gradlePluginVariant.sourceSetName)

    val dokkaTaskSuffix = if (gradlePluginVariant == GradlePluginVariant.GRADLE_MIN) {
        ""
    } else {
        gradlePluginVariant.sourceSetName.replaceFirstChar { it.uppercaseChar() }
    }
    val dokkaTaskName = "dokka${dokkaTaskSuffix}Html"

    val dokkaTask = if (tasks.names.contains(dokkaTaskName)) {
        tasks.named<DokkaTask>(dokkaTaskName)
    } else {
        tasks.register<DokkaTask>(dokkaTaskName)
    }

    dokkaTask.configure {
        description = "Generates API documentation for '${variantSourceSet.name}' variant"
        notCompatibleWithConfigurationCache("Dokka is not compatible with Configuration Cache yet.")

        configureCommonDokkaConfiguration(gradlePluginVariant, commonSourceSet, variantSourceSet)
    }

    tasks.named<Jar>(variantSourceSet.javadocJarTaskName).configure {
        from(dokkaTask.flatMap { it.outputDirectory })
    }
}

internal fun Project.configureTaskForKotlinlang() {
    if (!kotlinBuildProperties.publishGradlePluginsJavadoc) return

    tasks.named<DokkaTaskPartial>("dokkaHtmlPartial").configure {
        notCompatibleWithConfigurationCache("Dokka is not compatible with Configuration Cache yet.")

        val gradlePluginVariant = GradlePluginVariant.GRADLE_MIN
        val javaExtension = this@configureTaskForKotlinlang.extensions.getByType<JavaPluginExtension>()
        val commonSourceSet = javaExtension.sourceSets.getByName(commonSourceSetName)
        val variantSourceSet = javaExtension.sourceSets.getByName(gradlePluginVariant.sourceSetName)

        configureCommonDokkaConfiguration(gradlePluginVariant, commonSourceSet, variantSourceSet)
    }
}

fun AbstractDokkaLeafTask.configureCommonDokkaConfiguration(
    gradlePluginVariant: GradlePluginVariant,
    commonSourceSet: SourceSet,
    variantSourceSet: SourceSet,
) {
    suppressInheritedMembers.set(true)
    suppressObviousFunctions.set(true)

    dokkaSourceSets.named(commonSourceSet.name) {
        suppress.set(false)
        jdkVersion.set(8)
    }

    dokkaSourceSets.named(variantSourceSet.name) {
        dependsOn(commonSourceSet)
        suppress.set(false)
        jdkVersion.set(8)


        externalDocumentationLink {
            url.set(URI(gradlePluginVariant.gradleApiJavadocUrl).toURL())

            addWorkaroundForElementList(gradlePluginVariant)
        }
    }

    /**
     * The [GradlePluginVariant.GRADLE_MIN] ("main") source set is a peer of [variantSourceSet], so it should be suppressed in the Dokka generation for the variant source set.
     *
     * This hack relies on dokkaSourceSets set being unique per AbstractDokkaLeafTask.
     */
    if (gradlePluginVariant != GradlePluginVariant.GRADLE_MIN) {
        dokkaSourceSets.named(GradlePluginVariant.GRADLE_MIN.sourceSetName) {
            suppress.set(true)
        }
    }
}


private val DOKKA_EMBEDDED_SOURCES_ATTRIBUTE = Attribute.of("dokka-embedded-sources", String::class.java)
private const val attributeDefaultValue = "embedded-sources"

internal abstract class EmbeddedDokkaSourcesInfoTask : DefaultTask() {
    @get:InputFiles
    @get:NormalizeLineEndings
    val sources: ConfigurableFileCollection = project.objects.fileCollection()

    @get:OutputFile
    val outputFile: Provider<RegularFile> = project.layout.buildDirectory.dir("dokkaEmbedded").map { it.file("dokka-embedded.txt") }

    @TaskAction
    fun generateSourcesInfo() {
        val resultFile = outputFile.get().asFile
        resultFile.parentFile.mkdirs()
        resultFile.writeText(
            sources.asFileTree.files.joinToString(separator = "\n") { it.absolutePath }
        )
    }
}

/**
 * Exposes project sources information to generate API reference for embedded project dependencies.
 */
fun Project.exposeSourcesForDocumentationEmbedding(sourceSets: Set<KotlinSourceSet>) {
    val generatorTask = tasks.register<EmbeddedDokkaSourcesInfoTask>("generateDokkaEmbeddedSourcesInfo") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        sources.from(sourceSets.map { it.allKotlinSources })
    }

    val outgoingConfiguration = configurations.consumable("dokkaSourcesForEmbedding") {
        description = "Exposes project sources that Dokka should consume for embedded dependencies"

        attributes.attribute(DOKKA_EMBEDDED_SOURCES_ATTRIBUTE, attributeDefaultValue)
    }

    artifacts.add(outgoingConfiguration.name, generatorTask)
}

internal fun Project.consumeEmbeddedSources(embedProject: ProjectDependency) {
    val decConf = configurations.dependencyScope("dokkaEmbedded")
    val resConf = configurations.resolvable("dokkaEmbeddedResolvable") {
        extendsFrom(decConf.get())
        attributes.attribute(DOKKA_EMBEDDED_SOURCES_ATTRIBUTE, attributeDefaultValue)
    }

    dependencies.add(decConf.name, embedProject)
    val embedSources = objects.fileCollection()
    embedSources.from(
        resConf.map {
            it.asFileTree.files.fold(mutableListOf<File>()) { acc, file ->
                val sourceFiles = file.readText().lineSequence().map { File(it) }.toList()
                acc.addAll(sourceFiles)
                acc
            }
        }
    )

    project.tasks.named<DokkaTaskPartial>("dokkaHtmlPartial").configure {
        dokkaSourceSets.register("jvm") {
            sourceRoots.from(embedSources)
        }
    }
}