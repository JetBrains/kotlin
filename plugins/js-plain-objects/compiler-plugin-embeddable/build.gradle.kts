plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":plugins:js-plain-objects:compiler-plugin")) { isTransitive = false }
}

publish {
    artifactId = "kotlinx-js-plain-objects-compiler-plugin-embeddable"
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded(
    project(":plugins:js-plain-objects:compiler-plugin").tasks.named<Jar>("sourcesJar")
)
javadocJarWithJavadocFromEmbedded(
    project(":plugins:js-plain-objects:compiler-plugin").tasks.named<Jar>("javadocJar")
)
