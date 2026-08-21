plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    embedded(project(":kotlin-lombok-compiler-plugin")) { isTransitive = false }
}

publish {
    artifactId = artifactId.replace(".", "-")
}
runtimeJar(
    rewriteDefaultJarDepsToShadedCompiler(
        /* Referencing classifiers by FQN which shall not be relocated (KT-88353) */
        skipRelocatingStringConstants = true
    )
)

sourcesJarWithSourcesFromEmbedded()
javadocJarWithJavadocFromEmbedded()
