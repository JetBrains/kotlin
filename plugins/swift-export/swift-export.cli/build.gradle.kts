description = "Swift Compiler Plugin (CLI)"

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":kotlin-swift-export-compiler-plugin.backend"))

    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:fir:entrypoint"))

    embedded(project(":analysis:analysis-api-standalone")) { isTransitive = false }
    embedded(project(":analysis:analysis-api")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-fir")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-impl-base")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-impl-barebone")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-standalone:analysis-api-standalone-base")) { isTransitive = false }
    embedded(project(":analysis:analysis-api-standalone:analysis-api-fir-standalone-base")) { isTransitive = false }
    embedded(project(":analysis:analysis-internal-utils")) { isTransitive = false }
    embedded(project(":analysis:low-level-api-fir")) { isTransitive = false }
    embedded(project(":analysis:symbol-light-classes")) { isTransitive = false }
}

optInToExperimentalCompilerApi()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
sourcesJar()
javadocJar()
