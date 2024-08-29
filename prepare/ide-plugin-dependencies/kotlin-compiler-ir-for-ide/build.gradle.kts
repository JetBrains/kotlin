plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

val irCompilerModulesForIDE: Array<String> by rootProject.extra

publishJarsForIde(irCompilerModulesForIDE.asList())
