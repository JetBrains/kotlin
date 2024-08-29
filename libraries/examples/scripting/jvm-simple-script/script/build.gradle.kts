
plugins {
    kotlin("jvm")
    id("java-instrumentation")
}

dependencies {
    api(project(":kotlin-scripting-jvm"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
