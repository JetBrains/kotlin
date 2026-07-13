// This artifact is deprecated and will be remove in the near future. Use `kotlin-jps-plugin` instead
plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
}

idePluginPublishingLatch {
    @Suppress("UNCHECKED_CAST")
    val embeddedDependencies = CompilerModules.kotlinJpsPluginEmbeddedDependencies
    @Suppress("UNCHECKED_CAST")
    val mavenDependencies = CompilerModules.kotlinJpsPluginMavenDependencies
    @Suppress("UNCHECKED_CAST")
    val mavenDependenciesLibs = rootProject.extra["kotlinJpsPluginMavenDependenciesNonTransitiveLibs"] as List<String>

    val otherProjects = listOf(":jps:jps-plugin", ":jps:jps-common")

    publishProjectJars(
        embeddedDependencies + mavenDependencies + otherProjects,
        libraryDependencies = mavenDependenciesLibs + listOf(protobufFull())
    )
}
