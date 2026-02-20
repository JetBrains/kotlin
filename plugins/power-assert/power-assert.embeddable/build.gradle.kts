plugins {
    id("root-config")
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
sourcesJarWithSourcesFromEmbedded(embeddedProjectSources(":kotlin-power-assert-compiler-plugin"))
javadocJarWithJavadocFromEmbedded(embeddedProjectJavadoc(":kotlin-power-assert-compiler-plugin"))
