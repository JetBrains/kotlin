plugins {
    kotlin("multiplatform").version("<pluginMarkerVersion>")
    id("maven-publish")
}

group = "org.sample"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    <SingleNativeTarget>("native")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>> {
    compilerOptions {
        freeCompilerArgs.add("-Xklib-relative-path-base=$projectDir/src/nativeMain/kotlin/foo,$projectDir/src/nativeMain/kotlin/bar")
    }
}

publishing {
    repositories {
        maven {
            url = uri("<LocalRepo>")
        }
    }
}
