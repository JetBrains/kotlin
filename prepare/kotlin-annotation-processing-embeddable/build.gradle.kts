import org.gradle.jvm.tasks.Jar

description = "Annotation Processor for Kotlin (for using with embeddable compiler)"

plugins {
    `java`
}

dependencies {
    embedded(project(":kotlin-annotation-processing")) { isTransitive = false }
}

publish()

val jar: Jar by tasks
runtimeJar(rewriteDepsToShadedCompiler(jar))

sourcesJar()

javadocJar()
