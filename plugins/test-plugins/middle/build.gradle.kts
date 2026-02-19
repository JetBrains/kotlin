plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(project(":compiler:cli"))
    compileOnly(project(":compiler:ir.backend.common"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(intellijCore())
}

optInToExperimentalCompilerApi()
optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
