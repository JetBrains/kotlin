import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin Mock Runtime for Tests"

plugins {
    kotlin("jvm")
    `maven-publish`
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

val builtins by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
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
        .include("kotlin/collections/TypeAliases.kt",
                 "kotlin/enums/EnumEntriesSerializationProxy.kt")
    from(stdlibProjectDir.resolve("../src"))
        .include("kotlin/util/Standard.kt",
                 "kotlin/internal/Annotations.kt",
                 "kotlin/contracts/ContractBuilder.kt",
                 "kotlin/contracts/Effect.kt",
                 "kotlin/annotations/WasExperimental.kt",
                 "kotlin/enums/EnumEntries.kt",
                 "kotlin/collections/AbstractList.kt",
                 "kotlin/io/Serializable.kt")
    into(File(buildDir, "src"))
}

tasks.withType<KotlinCompile> {
    dependsOn(copySources)
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xallow-kotlin-package",
            "-Xmulti-platform",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.contracts.ExperimentalContracts"
        )
        moduleName = "kotlin-stdlib"
    }
}

val jar = runtimeJar {
    dependsOn(builtins)
    from(provider { zipTree(builtins.singleFile) }) { include("kotlin/**") }
}

publishing {
    publications {
        create<MavenPublication>("internal") {
            artifact(jar.get())
        }
    }

    repositories {
        maven("${rootProject.buildDir}/internal/repo")
    }
}
