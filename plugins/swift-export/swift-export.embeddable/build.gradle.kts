plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":kotlin-swift-export-compiler-plugin")) { isTransitive = false }
}

if (project.kotlinBuildProperties.isSwiftExportPluginPublishingEnabled) {
    // todo: is you are removing this check - don't forget to run tests in repo/artifacts-tests/src/test/kotlin/org/jetbrains/kotlin/code/ArtifactsTest.kt
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
