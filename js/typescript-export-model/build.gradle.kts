plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":js:js.serializer"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
