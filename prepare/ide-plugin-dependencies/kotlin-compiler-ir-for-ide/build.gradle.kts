plugins {
    kotlin("jvm")
}

@Suppress("UNCHECKED_CAST")
    val irCompilerModulesForIDE = rootProject.extra["irCompilerModulesForIDE"] as Array<String>

publishJarsForIde(irCompilerModulesForIDE.asList())
