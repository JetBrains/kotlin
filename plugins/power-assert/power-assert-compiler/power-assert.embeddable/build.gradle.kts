plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":kotlin-power-assert-compiler-plugin")) { isTransitive = false }
    embedded(commonDependency("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm")) { isTransitive = false }
}

publish {
    artifactId = artifactId.replace(".", "-")
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded()
javadocJarWithJavadocFromEmbedded()
