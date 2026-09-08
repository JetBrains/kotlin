// This artifact is deprecated and will be remove in the near future. Use `kotlin-jps-plugin` instead
plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
}

idePluginPublishingLatch {
    val embeddedDependencies = CompilerModules.kotlinJpsPluginEmbeddedDependencies
    val mavenDependencies = CompilerModules.kotlinJpsPluginMavenDependencies

    val otherProjects = listOf(":jps:jps-plugin", ":jps:jps-common")

    publishProjectJars(
        embeddedDependencies + mavenDependencies + otherProjects,
        libraryDependencies = listOf(commonDependency("org.jetbrains.kotlin:kotlin-reflect"), protobufFull())
    )
}
