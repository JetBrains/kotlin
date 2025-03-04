plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    jvmToolchain(11)

    compilerOptions {
        freeCompilerArgs.add("-Xsuppress-version-warnings")
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(gradleApi())
}


