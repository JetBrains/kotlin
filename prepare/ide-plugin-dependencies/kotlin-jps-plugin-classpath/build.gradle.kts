idePluginDependency {
    @Suppress("UNCHECKED_CAST")
    val compilerComponents = rootProject.extra["compilerModulesForJps"] as List<String>

    val otherProjects = listOf(":jps:jps-plugin", ":jps:jps-common")

    publishProjectJars(compilerComponents + otherProjects, libraryDependencies = listOf(protobufFull()))
}
