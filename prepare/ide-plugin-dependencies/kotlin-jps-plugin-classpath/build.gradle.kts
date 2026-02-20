plugins {
    id("root-config")
}
// This artifact is deprecated and will be remove in the near future. Use `kotlin-jps-plugin` instead
idePluginDependency {
    val otherProjects = listOf(":jps:jps-plugin", ":jps:jps-common")

    publishProjectJars(
        ProjectModuleLists.kotlinJpsPluginEmbeddedDependencies + ProjectModuleLists.kotlinJpsPluginMavenDependencies + otherProjects,
        libraryDependencies = listOf(commonDependency("org.jetbrains.kotlin:kotlin-reflect"), protobufFull())
    )
}
