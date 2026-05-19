plugins {
    kotlin("jvm")
}

val firCompilerModules: Array<String> by rootProject.extra

publishJarsForIde(firCompilerModules.asList())
