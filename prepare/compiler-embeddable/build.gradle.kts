
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

description = "Kotlin Compiler (embeddable)"

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:${property("versions.shadow")}")
    }
}

plugins {
    `java`
}

val compilerJar by configurations.creating

val kotlinEmbeddableRootPackage = "org.jetbrains.kotlin"

val packagesToRelocate =
        listOf(
//               "com.intellij",
               "com.google",
               "com.sampullara",
               "org.apache",
               "org.jdom",
               "org.picocontainer",
               "jline",
               "gnu",
               "javax.inject",
               "org.fusesource")

dependencies {
    val compile by configurations

    compilerJar(projectRuntimeJar(":kotlin-compiler"))

    compile(project(":kotlin-stdlib"))
    compile(project(":kotlin-script-runtime"))
    compile(project(":kotlin-reflect"))
}

noDefaultJar()

runtimeJar(task<ShadowJar>("embeddable")) {
    destinationDir = File(buildDir, "libs")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//    dependsOn(":kotlin-compiler:proguard")
    from(compilerJar)
    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf")
    packagesToRelocate.forEach {
        relocate(it, "$kotlinEmbeddableRootPackage.$it")
    }
    relocate("org.fusesource", "$kotlinEmbeddableRootPackage.org.fusesource") {
        // TODO: remove "it." after #KT-12848 get addressed
        exclude("org.fusesource.jansi.internal.CLibrary")
    }
}

sourcesJar()
javadocJar()

publish()

