plugins {
    kotlin("jvm")
}

dependencies {
    embedded(project(":coverage-compiler-plugin")) { isTransitive = false }
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded(
    project(":coverage-compiler-plugin").tasks.named<Jar>("sourcesJar")
)
javadocJarWithJavadocFromEmbedded(
    project(":coverage-compiler-plugin").tasks.named<Jar>("javadocJar")
)
