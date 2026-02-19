plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":compiler:serialization"))
    api(project(":js:js.ast"))
    api(project(":js:js.config"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
