idePluginDependency {
    @Suppress("UNCHECKED_CAST")
    val compilerComponents = rootProject.extra["compilerModulesForJps"] as List<String>

    val otherProjects = listOf(":kotlin-daemon-client", ":jps:jps-plugin", ":jps:jps-common", ":kotlin-reflect")

    publishProjectJars(compilerComponents + otherProjects, libraryDependencies = listOf(protobufFull()))
}
