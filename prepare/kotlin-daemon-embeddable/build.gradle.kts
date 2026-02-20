description = "Kotlin Daemon (for using with embeddable compiler)"

plugins {
    id("root-config")
    `java`
}

dependencies {
    embedded(project(":kotlin-daemon")) { isTransitive = false }
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())

sourcesJar()
javadocJar()
