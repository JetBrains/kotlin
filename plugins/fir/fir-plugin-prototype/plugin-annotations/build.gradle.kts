plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

runtimeJar()
