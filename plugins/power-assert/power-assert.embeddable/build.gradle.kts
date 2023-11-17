plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":kotlin-power-assert-compiler-plugin")) { isTransitive = false }
}

publish {
    artifactId = artifactId.replace(".", "-")
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded(
    project(":kotlin-power-assert-compiler-plugin").tasks.named<Jar>("sourcesJar")
)
javadocJarWithJavadocFromEmbedded(
    project(":kotlin-power-assert-compiler-plugin").tasks.named<Jar>("javadocJar")
)
