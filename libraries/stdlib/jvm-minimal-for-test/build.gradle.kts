import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin Mock Runtime for Tests"

plugins {
    kotlin("jvm")
    `maven-publish`
}

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

val builtins by configurations.creating

val runtime by configurations
val runtimeJar by configurations.creating {
    runtime.extendsFrom(this)
}

dependencies {
    compileOnly(project(":kotlin-stdlib"))
    builtins(project(":core:builtins"))
}

sourceSets {
    "main" {
        java.apply {
            srcDir(File(buildDir, "src"))
        }
    }
    "test" {}
}

val copySources by task<Sync> {
    val stdlibProjectDir = project(":kotlin-stdlib").projectDir

    from(stdlibProjectDir.resolve("runtime"))
        .include("kotlin/TypeAliases.kt",
                 "kotlin/text/TypeAliases.kt")
    from(stdlibProjectDir.resolve("src"))
        .include("kotlin/collections/TypeAliases.kt")
    from(stdlibProjectDir.resolve("../src"))
        .include("kotlin/util/Standard.kt",
                 "kotlin/internal/Annotations.kt",
                 "kotlin/contracts/ContractBuilder.kt",
                 "kotlin/contracts/Effect.kt")
    into(File(buildDir, "src"))
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.6"
    targetCompatibility = "1.6"
}

tasks.withType<KotlinCompile> {
    dependsOn(copySources)
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-module-name",
            "kotlin-stdlib",
            "-Xallow-kotlin-package",
            "-Xmulti-platform",
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlin.contracts.ExperimentalContracts"
        )
    }
}

val jar = runtimeJar {
    archiveFileName.set("kotlin-stdlib-minimal-for-test.jar")
    dependsOn(builtins)
    from(provider { zipTree(builtins.singleFile) }) { include("kotlin/**") }
}

publishing {
    publications {
        create<MavenPublication>("internal") {
            artifactId = "kotlin-stdlib-minimal-for-test"
            artifact(jar.get())
        }
    }

    repositories {
        maven("${rootProject.buildDir}/internal/repo")
    }
}

