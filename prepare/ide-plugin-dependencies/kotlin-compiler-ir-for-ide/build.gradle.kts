plugins {
    id("root-config")
    kotlin("jvm")
}

val irCompilerModulesForIDE = ProjectModuleLists.irCompilerModulesForIDE

publishJarsForIde(irCompilerModulesForIDE.asList())
