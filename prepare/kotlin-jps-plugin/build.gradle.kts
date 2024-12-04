plugins {
    `java-library`
}

dependencies {
    @Suppress("UNCHECKED_CAST")
    rootProject.extra["kotlinJpsPluginMavenDependencies"]
        .let { it as List<String> }
        .forEach { implementation(project(it)) }

    @Suppress("UNCHECKED_CAST")
    rootProject.extra["kotlinJpsPluginMavenDependenciesNonTransitiveLibs"]
        .let { it as List<String> }
        .forEach { implementation(it) { isTransitive = false } }
}

@Suppress("UNCHECKED_CAST")
val embeddedDependencies = rootProject.extra["kotlinJpsPluginEmbeddedDependencies"] as List<String>
publishProjectJars(
    embeddedDependencies + listOf(":jps:jps-plugin", ":jps:jps-common"),
    libraryDependencies = listOf(protobufFull())
)
