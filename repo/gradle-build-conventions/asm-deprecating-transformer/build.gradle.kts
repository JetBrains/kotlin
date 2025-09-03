plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    maven(url = "file:///dump")
    mavenCentral()
    gradlePluginPortal()
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") {
        content {
            includeGroupByRegex("org\\.jetbrains\\.intellij\\.deps(\\..+)?")
        }
    }
}

kotlin {
    jvmToolchain(8)

    compilerOptions {
        allWarningsAsErrors.set(true)
        optIn.add("kotlin.ExperimentalStdlibApi")
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

dependencies {
    compileOnly(kotlin("stdlib", embeddedKotlinVersion))
    implementation(libs.intellij.asm)
    implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:${project.bootstrapKotlinVersion}")
    implementation(libs.diff.utils)
    compileOnly(libs.shadow.gradlePlugin)
}
