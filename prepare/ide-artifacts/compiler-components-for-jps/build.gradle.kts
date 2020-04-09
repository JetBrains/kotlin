@Suppress("UNCHECKED_CAST")
val compilerComponentProjects = project(":kotlin-jps-plugin").extra["compilerComponents"] as List<String>

val otherProjects = listOf(":kotlin-daemon-client")

publishProjectJars(compilerComponentProjects + otherProjects)