plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":kotlin-formver-compiler-plugin")) { isTransitive = false }
}

publish {
    artifactId = artifactId.replace(".", "-")
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded(
    project(":kotlin-formver-compiler-plugin").tasks.named<Jar>("sourcesJar")
)
javadocJarWithJavadocFromEmbedded(
    project(":kotlin-formver-compiler-plugin").tasks.named<Jar>("javadocJar")
)
