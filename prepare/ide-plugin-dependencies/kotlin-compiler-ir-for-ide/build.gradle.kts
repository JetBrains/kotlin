plugins {
    kotlin("jvm")
}

val irCompilerModulesForIDE: Array<String> by rootProject.extra

publishJarsForIde(irCompilerModulesForIDE.asList())
