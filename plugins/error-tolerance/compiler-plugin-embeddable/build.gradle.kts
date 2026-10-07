description = "Kotlin Error Tolerance Compiler Plugin (Embeddable)"

plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":plugins:error-tolerance:compiler-plugin")) { isTransitive = false }
}

publish {
    artifactId = "kotlin-error-tolerance-compiler-plugin-embeddable"
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded()
javadocJarWithJavadocFromEmbedded()
