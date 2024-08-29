description = "Kotlin Daemon (for using with embeddable compiler)"

plugins {
    id("java-instrumentation")
    `java`
}

dependencies {
    embedded(project(":kotlin-daemon")) { isTransitive = false }
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

sourcesJar()
javadocJar()
