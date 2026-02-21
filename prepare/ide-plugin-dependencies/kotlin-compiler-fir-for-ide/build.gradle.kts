plugins {
    id("root-config")
    kotlin("jvm")
}

val firCompilerCoreModules = ProjectModuleLists.firCompilerCoreModules

publishJarsForIde(firCompilerCoreModules.asList())
