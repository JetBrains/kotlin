plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    maven("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
    mavenCentral { setUrl("https://cache-redirector.jetbrains.com/maven-central") }
    gradlePluginPortal()

    extra["bootstrapKotlinRepo"]?.let {
        maven(url = it)
    }
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        allWarningsAsErrors.set(true)
        optIn.add("kotlin.ExperimentalStdlibApi")
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

dependencies {
    compileOnly(kotlin("stdlib", embeddedKotlinVersion))
    implementation(libs.develocity.gradlePlugin)
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion.get()}")
    api(project(":utilities"))
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
    implementation(project(":d8-configuration"))
    compileOnly(libs.node.gradlePlugin)

    constraints {
        api(libs.apache.commons.lang)
    }
}
