plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

val irCompilerModulesForIDE: Array<String> = CompilerModules.irCompilerModulesForIDE

publishJarsForIde(irCompilerModulesForIDE.asList())
