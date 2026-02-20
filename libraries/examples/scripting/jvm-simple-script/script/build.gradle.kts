
plugins {
    id("root-config")
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-scripting-jvm"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
