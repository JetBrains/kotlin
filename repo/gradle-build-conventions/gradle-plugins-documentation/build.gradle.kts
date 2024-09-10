plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(11)

    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    mavenCentral()
    google()
    gradlePluginPortal()

    extra["bootstrapKotlinRepo"]?.let {
        maven(url = it)
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")
    implementation(libs.dokka.gradlePlugin)
}
