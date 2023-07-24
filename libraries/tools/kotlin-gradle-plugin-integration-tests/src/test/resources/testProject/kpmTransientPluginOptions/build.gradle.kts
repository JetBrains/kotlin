import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPm20PluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.project.model.*

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:<pluginMarkerVersion>")
    }
}

apply<KotlinPm20PluginWrapper>()

repositories {
    mavenLocal()
    mavenCentral()
}

plugins.apply(KpmGradlePluginWithTransientPluginOptions::class.java)

configure<KotlinPm20ProjectExtension> {
    main {
        jvm
    }
}

class KpmPluginWithTransientPluginOptions(
    private val regularOptionValue: String,
    private val transientOptionValue: String
) : KpmCompilerPlugin {
    private fun pluginData() = PluginData(
        pluginId = "test-plugin",
        // allopen artifact is used to avoid boilerplate with cooking custom compiler plugin
        artifact = PluginData.ArtifactCoordinates("org.jetbrains.kotlin", "kotlin-allopen"),
        options = listOf(
            StringOption("regular", regularOptionValue, isTransient = false),
            StringOption("transient", transientOptionValue, isTransient = true)
        )
    )

    override fun forMetadataCompilation(fragment: KpmFragment) = pluginData()
    override fun forNativeMetadataCompilation(fragment: KpmFragment) = pluginData()
    override fun forPlatformCompilation(variant: KpmVariant) = pluginData()
}

class KpmGradlePluginWithTransientPluginOptions : GradleKpmCompilerPlugin {
    private lateinit var project: Project

    override fun apply(target: Project) {
        project = target
    }

    override val kpmCompilerPlugin by lazy {
        KpmPluginWithTransientPluginOptions(
            regularOptionValue = project.property("test-plugin.regular") as String,
            transientOptionValue = project.property("test-plugin.transient") as String
        )
    }
}
