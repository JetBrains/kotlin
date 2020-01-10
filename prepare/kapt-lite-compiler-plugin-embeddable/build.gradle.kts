import org.gradle.jvm.tasks.Jar

description = "Lightweight annotation processing support – Kotlin compiler plugin (for using with embeddable compiler)"

plugins {
    `java`
}

dependencies {
    embedded(project(":kapt-lite:kapt-lite-compiler-plugin")) { isTransitive = false }
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

sourcesJar()
javadocJar()
