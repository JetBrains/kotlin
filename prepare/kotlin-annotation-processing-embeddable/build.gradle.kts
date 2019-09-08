import org.gradle.jvm.tasks.Jar

description = "Annotation Processor for Kotlin (for using with embeddable compiler)"

plugins {
    `java`
}

dependencies {
    embedded(project(":kotlin-annotation-processing")) { isTransitive = false }
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

sourcesJar()

javadocJar()
