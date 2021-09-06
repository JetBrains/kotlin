plugins {
    kotlin("jvm")
}

val firCompilerCoreModules: Array<String> by rootProject.extra

publishJarsForIde(firCompilerCoreModules.asList())
