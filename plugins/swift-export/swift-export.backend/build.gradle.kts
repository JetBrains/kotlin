description = "Swift Compiler Plugin (Backend)"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:backend"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(intellijCore())

    implementation(project(":native:swift:sir"))
    implementation(project(":native:swift:sir-analysis-api"))
    implementation(project(":native:swift:sir-compiler-bridge"))
    implementation(project(":native:swift:sir-passes"))
    implementation(project(":native:swift:sir-printer"))

    implementation(project(":analysis:analysis-api"))
    implementation(project(":analysis:analysis-api-standalone"))

    compileOnly(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    implementation(kotlinStdlib())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
