plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":rhizomedb-compiler-plugin")) { isTransitive = false }
}

publish {}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded(
    project(":rhizomedb-compiler-plugin").tasks.named<Jar>("sourcesJar")
)
javadocJarWithJavadocFromEmbedded(
    project(":rhizomedb-compiler-plugin").tasks.named<Jar>("javadocJar")
)
