plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Kotlin/Native utils"

dependencies {
    compile(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

standardPublicJars()

publish()
