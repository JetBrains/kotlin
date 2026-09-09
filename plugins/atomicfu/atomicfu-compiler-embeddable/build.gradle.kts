plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":kotlin-atomicfu-compiler-plugin")) { isTransitive = false }
}

publish()

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
sourcesJarWithSourcesFromEmbedded()
javadocJarWithJavadocFromEmbedded()
