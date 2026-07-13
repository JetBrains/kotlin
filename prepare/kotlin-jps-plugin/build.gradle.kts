plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    `java-library`
}

dependencies {
    @Suppress("UNCHECKED_CAST")
    CompilerModules.kotlinJpsPluginMavenDependencies
        .forEach { implementation(project(it)) }

    @Suppress("UNCHECKED_CAST")
    rootProject.extra["kotlinJpsPluginMavenDependenciesNonTransitiveLibs"]
        .let { it as List<String> }
        .forEach { implementation(it) { isTransitive = false } }
}

@Suppress("UNCHECKED_CAST")
val embeddedDependencies = CompilerModules.kotlinJpsPluginEmbeddedDependencies
publishProjectJars(
    embeddedDependencies + listOf(":jps:jps-plugin", ":jps:jps-common"),
    libraryDependencies = listOf(protobufFull())
)
