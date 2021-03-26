plugins {
    kotlin("jvm")
}

@Suppress("UNCHECKED_CAST")
val compilerComponents = rootProject.extra["compilerModulesForJps"] as List<String>

val otherProjects = listOf(":kotlin-daemon-client")

publishJarsForIde(compilerComponents + otherProjects)
