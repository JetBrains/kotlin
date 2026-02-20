plugins {
    id("root-config")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":kotlinx-serialization-compiler-plugin")) { isTransitive = false }
}

publish {
    artifactId = artifactId.replace(".", "-").replace("kotlinx-", "kotlin-")
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded(embeddedProjectSources(":kotlinx-serialization-compiler-plugin"))
javadocJarWithJavadocFromEmbedded(embeddedProjectJavadoc(":kotlinx-serialization-compiler-plugin"))
