import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `kotlin-dsl`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "5.1.0"
}

val ultimateTools: Map<String, Any> by rootProject.extensions
val proprietaryRepositories: Project.() -> Unit by ultimateTools

val cidrVersion = rootProject.extra["versions.cidrPlatform"] as String
val pluginName = "applePlugin"
group = "org.jetbrains.gradle.apple"
version = "$cidrVersion-0.1"

repositories {
    proprietaryRepositories()
    google()
}

// needed to prevent inclusion of gradle-api into shadow JAR
configurations.named(JavaPlugin.API_CONFIGURATION_NAME) {
    dependencies.remove(project.dependencies.gradleApi())
}

dependencies {
    shadow("com.jetbrains.intellij.platform:core:$cidrVersion")
    shadow("com.jetbrains.intellij.platform:util:$cidrVersion")
    shadow("com.jetbrains.intellij.platform:util-rt:$cidrVersion")
    shadow("com.jetbrains.intellij.platform:util-class-loader:$cidrVersion")
    shadow("com.jetbrains.intellij.platform:util-ex:$cidrVersion")
    shadow("com.jetbrains.intellij.platform:extensions:$cidrVersion")
    shadow("com.jetbrains.intellij.platform:core-impl:$cidrVersion")
    shadow("com.jetbrains.intellij.cidr:cidr-xcode-model-core:$cidrVersion")

    compile(project(":kotlin-ultimate:libraries:tools:apple-gradle-plugin-api"))
}

tasks {
    shadowJar { classifier = null }
    jar { enabled = false }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.languageVersion = "1.3"
        kotlinOptions.apiVersion = "1.3"
        kotlinOptions.freeCompilerArgs += listOf("-Xskip-metadata-version-check", "-Xjvm-default=enable")
    }

    named<ValidateTaskProperties>("validateTaskProperties") {
        failOnWarning = true
    }
}

gradlePlugin {
    isAutomatedPublishing = false

    val applePlugin by plugins.registering {
        id = "$group.$pluginName"
        implementationClass = "ApplePlugin"
    }
}

publishing {
    repositories {
        maven {
            url = uri("$buildDir/repo")
        }
    }

    val applePlugin by publications.registering(MavenPublication::class) {
        artifactId = pluginName
        pom.withXml {
            asNode().appendNode("packaging", "pom")
        }
        project.shadow.component(this)
    }

    val applePluginMarker by publications.registering(MavenPublication::class) {
        groupId = "$group.$pluginName"
        artifactId = "$groupId.gradle.plugin"
        pom.withXml {
            val plugin = applePlugin.get()
            asNode().appendNode("dependencies").appendNode("dependency").run {
                appendNode("groupId", plugin.groupId)
                appendNode("artifactId", plugin.artifactId)
                appendNode("version", plugin.version)
            }
        }
    }
}