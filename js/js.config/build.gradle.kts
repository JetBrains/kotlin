plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:frontend.common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
