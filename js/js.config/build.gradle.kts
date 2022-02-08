plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":core:deserialization"))
    api(project(":compiler:config"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
