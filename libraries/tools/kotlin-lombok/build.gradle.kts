import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import plugins.configureDefaultPublishing

description = "Kotlin lombok compiler plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    embedded(project(":plugins:lombok:lombok-compiler-plugin")) { isTransitive = false }

    compileOnly(gradleApi())
    api(project(":kotlin-gradle-plugin-api"))
    api(project(":kotlin-gradle-plugin-model"))
}

projectTest(parallel = true) {
    workingDir = projectDir
}

publish()

sourcesJar()
javadocJar()
runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

tasks {
    withType<KotlinCompile> {
//        kotlinOptions.jdkHome = rootProject.extra["JDK_18"] as String
        kotlinOptions.languageVersion = "1.3"
        kotlinOptions.apiVersion = "1.3"
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xskip-prerelease-check", "-Xsuppress-version-warnings"
        )
    }

    named<Jar>("jar") {
        callGroovy("manifestAttributes", manifest, project)
    }
}

pluginBundle {
    fun create(name: String, id: String, display: String) {
        (plugins).create(name) {
            this.id = id
            this.displayName = display
            this.description = display
        }
    }

    create(
        name = "kotlinLombokPlugin",
        id = "org.jetbrains.kotlin.plugin.lombok",
        display = "Kotlin Lombok plugin"
    )
}

publishPluginMarkers()
