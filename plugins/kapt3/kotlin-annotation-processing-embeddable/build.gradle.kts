description = "Annotation Processor for Kotlin (for using with embeddable compiler)"

plugins {
    `java-library`
}

dependencies {
    embedded(project(":kotlin-annotation-processing")) { isTransitive = false }
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJar()
javadocJar()
