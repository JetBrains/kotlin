
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":js:js.ast"))
    api(project(":js:js.translator"))
    compileOnly(project(":js:js.sourcemap"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("com.google.guava:guava"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

