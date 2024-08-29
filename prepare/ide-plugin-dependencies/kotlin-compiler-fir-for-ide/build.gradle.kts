plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

val firCompilerCoreModules: Array<String> by rootProject.extra

publishJarsForIde(firCompilerCoreModules.asList())
