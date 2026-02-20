description = "Kotlin DataFrame Compiler Plugin (Embeddable)"

plugins {
    id("root-config")
    kotlin("jvm")
}

dependencies {
    embedded(project(":kotlin-dataframe-compiler-plugin")) { isTransitive = false }
}

publish {
    artifactId = artifactId.replace(".", "-")
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded(embeddedProjectSources(":kotlin-dataframe-compiler-plugin"))
javadocJarWithJavadocFromEmbedded(embeddedProjectJavadoc(":kotlin-dataframe-compiler-plugin"))
