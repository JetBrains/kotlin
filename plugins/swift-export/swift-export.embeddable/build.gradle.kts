plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":kotlin-swift-export-compiler-plugin")) { isTransitive = false }
}

if (project.hasProperty("kotlin-native.swift-export.enabled")) {
    publish {
        artifactId = "kotlin-swift-export-compiler-plugin-embeddable"
    }
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded(
    project(":kotlin-swift-export-compiler-plugin").tasks.named<Jar>("sourcesJar")
)
javadocJarWithJavadocFromEmbedded(
    project(":kotlin-swift-export-compiler-plugin").tasks.named<Jar>("javadocJar")
)
