plugins {
    kotlin("android")
    id("com.android.library")
    `maven-publish`
}

android {
    kotlinOptions {
        jvmTarget = "1.8"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    compileSdk = 31
    defaultConfig {
        minSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    namespace = "org.jetbrains.kotlin.sample"

    publishing {
        multipleVariants {
            allVariants()
        }
    }
}

publishing {
    repositories {
        maven("<localRepo>")
    }

    publications {
        create<MavenPublication>("maven") {
            afterEvaluate {
                from(components["default"])
            }
        }
    }
}

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
        project.resolveDependencies("releaseCompileClasspath")
    }
}