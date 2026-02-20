plugins {
    id("root-config")
    kotlin("jvm")
}

val firCompilerCoreModules: Array<String> by rootProject.extra

publishJarsForIde(firCompilerCoreModules.asList())
