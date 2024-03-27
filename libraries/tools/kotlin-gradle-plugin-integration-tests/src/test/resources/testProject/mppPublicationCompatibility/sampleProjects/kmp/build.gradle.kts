plugins {
    kotlin("multiplatform")
    `maven-publish`
    // id("com.android.library") // AGP
}

/* Begin AGP
android {
    compileSdk = 31
    defaultConfig {
        minSdk = 31
    }
    namespace = "org.jetbrains.kotlin.sample"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
kotlin {
    androidTarget {
        publishAllLibraryVariants()
        compilations.all {
            // for compatibility between 1.7.21 and 2.0+
            compileKotlinTaskProvider.configure {
                kotlinOptions.jvmTarget = "1.8"
            }
        }
    }
}
End AGP */

kotlin {
    // jvm() // JVM

    linuxX64()
    linuxArm64()
}

publishing {
    repositories {
        maven("<localRepo>")
    }
}

version = "1.0"

fun Project.resolveDependencies(name: String) {
    val configuration = configurations.findByName(name) ?: return
    configuration.resolve() // ensure that resolution is green
    val allResolvedComponents = configuration.incoming.resolutionResult.allComponents
    val content = allResolvedComponents
        .map { component -> "${component.id} => ${component.variants.map { it.displayName }}" }
        .sorted()
        .joinToString("\n")
    val dir = file("resolvedDependenciesReports")
    dir.mkdirs()
    dir.resolve("${name}.txt").writeText(content)
}

tasks.register("resolveDependencies") {
    doFirst {
        project.resolveDependencies("jvmCompileClasspath")
        project.resolveDependencies("androidReleaseCompileClasspath")
        project.resolveDependencies("linuxX64CompileKlibraries")
        project.resolveDependencies("linuxArm64CompileKlibraries")
    }
}