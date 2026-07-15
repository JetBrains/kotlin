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

    val otherProjects = listOf(":jps:jps-plugin", ":jps:jps-common")

    publishProjectJars(
        embeddedDependencies + mavenDependencies + otherProjects,
        libraryDependencies = listOf(commonDependency("org.jetbrains.kotlin:kotlin-reflect"), protobufFull())
    )
}
