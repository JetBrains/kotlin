@file:Suppress("HasPlatformType")

import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import java.util.regex.Pattern.quote

description = "Kotlin JKlib Compiler"

plugins {
    `java-library`
    kotlin("jvm")
}

val fatJarContents by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

val compilerDist by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    compilerDist(project(":kotlin-compiler", configuration = "distElements"))
    
    fatJarContents(project(":compiler:cli-jklib")) { isTransitive = false }
    fatJarContents(project(":compiler:ir.serialization.jklib")) { isTransitive = false }
}

val distDir: String by rootProject.extra
val compilerBaseName = name

val jar = runtimeJar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    val baseCompilerJar = project(":kotlin-compiler").tasks.named<Jar>("jar")
    dependsOn(baseCompilerJar)
    
    from {
        zipTree(baseCompilerJar.get().archiveFile)
    }

    dependsOn(fatJarContents)
    from {
        fatJarContents.map(::zipTree)
    }

    manifest.attributes["Class-Path"] = baseCompilerJar.get().manifest.attributes["Class-Path"]
    manifest.attributes["Main-Class"] = "org.jetbrains.kotlin.cli.jklib.K2JKlibCompiler"
}

val distBase = project(":kotlin-compiler").tasks.named<Copy>("dist")

val dist = tasks.register<Sync>("dist") {
    dependsOn(distBase)
    // We copy the base distribution contents
    val originalDistFolder = File(distDir)
    destinationDir = File("$distDir/jklib-compiler")

    from(originalDistFolder) {
        exclude("kotlinc/lib/kotlin-compiler.jar")
        // Don't copy over other build outputs inside dist like maven, common, etc. if not needed
        // Just take the kotlinc directory structure
        include("kotlinc/**")
        include("build.txt")
        include("license/**")
    }

    into("kotlinc/lib") {
        from(jar) {
            rename { "kotlin-compiler.jar" }
        }
    }
}

artifacts {
    val distElements = configurations.create("distElements")
    add(distElements.name, dist)
}
