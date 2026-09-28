plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":kotlinx-serialization-compiler-plugin")) { isTransitive = false }
}

publish {
    artifactId = artifactId.replace(".", "-").replace("kotlinx-", "kotlin-")
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded()
javadocJarWithJavadocFromEmbedded()
