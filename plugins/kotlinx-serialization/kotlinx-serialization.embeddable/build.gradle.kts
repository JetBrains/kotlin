plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":kotlinx-serialization-compiler-plugin")) { isTransitive = false }
}

publish {
    artifactId = artifactId.replace(".", "-").replace("kotlinx-", "kotlin-")
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded(
    project(":kotlinx-serialization-compiler-plugin").tasks.named<Jar>("sourcesJar")
)
javadocJarWithJavadocFromEmbedded(
    project(":kotlinx-serialization-compiler-plugin").tasks.named<Jar>("javadocJar")
)
