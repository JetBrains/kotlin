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

val copySources by task<Sync> {
    val stdlibProjectDir = file("$rootDir/libraries/stdlib/jvm")

    from(stdlibProjectDir.resolve("runtime"))
        .include("kotlin/TypeAliases.kt",
                 "kotlin/text/TypeAliases.kt")
    from(stdlibProjectDir.resolve("src"))
        .include("kotlin/collections/TypeAliases.kt",
                 "kotlin/enums/EnumEntriesSerializationProxy.kt",
                 "kotlin/enums/EnumEntriesJVM.kt")
    from(stdlibProjectDir.resolve("../src"))
        .include("kotlin/util/Standard.kt",
                 "kotlin/internal/Annotations.kt",
                 "kotlin/contracts/ContractBuilder.kt",
                 "kotlin/contracts/Effect.kt",
                 "kotlin/annotations/WasExperimental.kt",
                 "kotlin/enums/EnumEntries.kt",
                 "kotlin/collections/AbstractList.kt",
                 "kotlin/io/Serializable.kt")
    into(layout.buildDirectory.dir("src"))
}

sourceSets {
    "main" {
        java.srcDir(copySources)
    }
    "test" {}
}

tasks.compileKotlin {
    dependsOn(copySources)
    val commonSources = listOf(
        "kotlin/enums/EnumEntries.kt"
    ).map { copySources.get().destinationDir.resolve(it) }
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xallow-kotlin-package",
            "-Xexpect-actual-classes",
            "-Xmulti-platform",
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-opt-in=kotlin.ExperimentalMultiplatform",
        )
        moduleName = "kotlin-stdlib"
    }
    doFirst {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xcommon-sources=${commonSources.joinToString(File.pathSeparator)}",
        )
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
        maven(rootProject.layout.buildDirectory.dir("internal/repo"))
    }
}
