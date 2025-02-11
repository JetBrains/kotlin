plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    mavenCentral()
    gradlePluginPortal()

    extra["bootstrapKotlinRepo"]?.let {
        maven(url = it)
    }
}

kotlin {
    jvmToolchain(8)

    compilerOptions {
        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

dependencies {
    compileOnly(kotlin("stdlib", embeddedKotlinVersion))
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.bootstrapKotlinVersion}")
}
