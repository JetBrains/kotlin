plugins {
    id("root-config")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":kotlin-noarg-compiler-plugin")) { isTransitive = false }
}

publish {
    artifactId = artifactId.replace(".", "-")
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded(embeddedProjectSources(":kotlin-noarg-compiler-plugin"))
javadocJarWithJavadocFromEmbedded(embeddedProjectJavadoc(":kotlin-noarg-compiler-plugin"))
