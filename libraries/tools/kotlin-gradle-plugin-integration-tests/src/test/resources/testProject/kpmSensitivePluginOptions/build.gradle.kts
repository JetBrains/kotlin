import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.project.model.*

plugins {
    kotlin("multiplatform.pm20")
}

repositories {
    mavenCentral()
    mavenLocal()
}

plugins.apply(GradleKpmPluginWithSensitivePluginOptions::class.java)

configure<KotlinPm20ProjectExtension> {
    main {
        jvm
    }
}

class KpmPluginWithSensitivePluginOptions(
    private val sensitiveOptionValue: String,
    private val insensitiveOptionValue: String
) : KpmCompilerPlugin {
    private fun pluginData() = PluginData(
        pluginId = "test-plugin",
        // allopen artifact is used to avoid boilerplate with cooking custom compiler plugin
        artifact = PluginData.ArtifactCoordinates("org.jetbrains.kotlin", "kotlin-allopen"),
        options = listOf(
            StringOption("sensitive", sensitiveOptionValue, true),
            StringOption("insensitive", insensitiveOptionValue, false)
        )
    )

    override fun forMetadataCompilation(fragment: KotlinModuleFragment) = pluginData()
    override fun forNativeMetadataCompilation(fragment: KotlinModuleFragment) = pluginData()
    override fun forPlatformCompilation(variant: KotlinModuleVariant) = pluginData()
}

class GradleKpmPluginWithSensitivePluginOptions : GradleKpmCompilerPlugin {
    private lateinit var project: Project

    override fun apply(target: Project) {
        project = target
    }

    override val kpmCompilerPlugin by lazy {
        KpmPluginWithSensitivePluginOptions(
            sensitiveOptionValue = project.property("test-plugin.sensitive") as String,
            insensitiveOptionValue = project.property("test-plugin.insensitive") as String
        )
    }
}