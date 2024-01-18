description = "Swift Compiler Plugin (Backend)"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(intellijCore())

    compileOnly(project(":native:swift:sir"))
    compileOnly(project(":native:swift:sir-analysis-api"))
    compileOnly(project(":native:swift:sir-compiler-bridge"))
    compileOnly(project(":native:swift:sir-passes"))
    compileOnly(project(":native:swift:sir-printer"))

    compileOnly(project(":analysis:analysis-api"))
    compileOnly(project(":analysis:analysis-api-standalone"))

    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    compileOnly(kotlinStdlib())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
