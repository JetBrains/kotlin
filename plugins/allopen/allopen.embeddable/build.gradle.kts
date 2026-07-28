plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":kotlin-allopen-compiler-plugin")) { isTransitive = false }
}

publish {
    artifactId = artifactId.replace(".", "-")
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded()
javadocJarWithJavadocFromEmbedded()
