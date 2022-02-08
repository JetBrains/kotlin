
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":js:js.ast"))
    api(project(":js:js.translator"))
    api(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
