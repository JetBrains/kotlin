import org.gradle.jvm.tasks.Jar

description = "ABI generation for Kotlin/JVM (for using with embeddable compiler)"

plugins {
    `java`
}

dependencies {
    embedded(project(":plugins:jvm-abi-gen")) { isTransitive = false }
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

sourcesJar()

javadocJar()