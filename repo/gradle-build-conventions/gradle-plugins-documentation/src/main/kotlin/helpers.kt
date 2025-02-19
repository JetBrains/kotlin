import gradle.GradlePluginVariant
import gradle.commonSourceSetName
import gradle.publishGradlePluginsJavadoc
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.dokka.gradle.AbstractDokkaLeafTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial
import org.jetbrains.dokka.gradle.GradleExternalDocumentationLinkBuilder
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

    val dokkaTaskSuffix = if (gradlePluginVariant.sourceSetName == "main") {
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
}
