plugins {
    id("root-config")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":kotlin-allopen-compiler-plugin")) { isTransitive = false }
}

publish {
    artifactId = artifactId.replace(".", "-")
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded(embeddedProjectSources(":kotlin-allopen-compiler-plugin"))
javadocJarWithJavadocFromEmbedded(embeddedProjectJavadoc(":kotlin-allopen-compiler-plugin"))
