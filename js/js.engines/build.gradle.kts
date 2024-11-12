
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":js:js.ast"))
    api(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
