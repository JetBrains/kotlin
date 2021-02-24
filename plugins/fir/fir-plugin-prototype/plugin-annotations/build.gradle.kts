plugins {
    kotlin("jvm")
    id("jps-compatible")
}


sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
