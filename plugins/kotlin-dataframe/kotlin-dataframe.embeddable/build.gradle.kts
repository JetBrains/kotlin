description = "Kotlin DataFrame Compiler Plugin (Embeddable)"

plugins {
    kotlin("jvm")
}

dependencies {
    embedded(project(":kotlin-dataframe-compiler-plugin")) { isTransitive = false }
}

publish {
    artifactId = artifactId.replace(".", "-")
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded(
    project(":kotlin-dataframe-compiler-plugin").tasks.named<Jar>("sourcesJar")
)
javadocJarWithJavadocFromEmbedded(
    project(":kotlin-dataframe-compiler-plugin").tasks.named<Jar>("javadocJar")
)
