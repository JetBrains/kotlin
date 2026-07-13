import org.gradle.jvm.tasks.Jar

description = "ABI generation for Kotlin/JVM (for using with embeddable compiler)"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    `java`
}

dependencies {
    embedded(project(":plugins:jvm-abi-gen")) { isTransitive = false }
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

sourcesJar()

javadocJar()
