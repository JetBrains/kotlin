plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":plugins:jso:compiler-plugin")) { isTransitive = false }
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded(
    project(":plugins:jso:compiler-plugin").tasks.named<Jar>("sourcesJar")
)
javadocJarWithJavadocFromEmbedded(
    project(":plugins:jso:compiler-plugin").tasks.named<Jar>("javadocJar")
)
