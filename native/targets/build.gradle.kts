import org.jetbrains.kotlin.build.foreign.registerForeignClassUsageTasks
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("kotlin-git.gradle-build-conventions.foreign-class-usage-checker")
}

description = "Kotlin/Native targets"

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))
}

kotlin {
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation()
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

configureKotlinCompileTasksGradleCompatibility()

publish()

runtimeJar {
    // JPMS can't handle the jar file name. Fix that by specifying a valid module name in the manifest:
    manifest.attributes["Automatic-Module-Name"] = "kotlin.native_targets"
}

sourcesJar()
javadocJar()

registerForeignClassUsageTasks {
    outputFile = file("api/kotlin-native-targets-api.foreign")
}
