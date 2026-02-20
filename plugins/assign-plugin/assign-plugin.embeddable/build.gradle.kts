description = "Kotlin Assignment Compiler Plugin (Embeddable)"

plugins {
    id("root-config")
    kotlin("jvm")
}

dependencies {
    embedded(project(":kotlin-assignment-compiler-plugin")) { isTransitive = false }
}

publish {
    artifactId = artifactId.replace(".", "-")
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded(embeddedProjectSources(":kotlin-assignment-compiler-plugin"))
javadocJarWithJavadocFromEmbedded(embeddedProjectJavadoc(":kotlin-assignment-compiler-plugin"))
