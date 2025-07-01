plugins {
    kotlin("jvm")
}

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
    implementation(project(":compiler:config")) { exclude("org.jetbrains.kotlin", "kotlin-stdlib") }
}

configureKotlinCompileTasksGradleCompatibility()
