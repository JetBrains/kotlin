plugins {
    kotlin("jvm")
}

val irCompilerModules: Array<String> by rootProject.extra

publishJarsForIde(irCompilerModules.asList())
