plugins {
    id("root-config")
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
    embeddedProjectSources(":plugins:js-plain-objects:compiler-plugin")
)
javadocJarWithJavadocFromEmbedded(
    embeddedProjectJavadoc(":plugins:js-plain-objects:compiler-plugin")
)
