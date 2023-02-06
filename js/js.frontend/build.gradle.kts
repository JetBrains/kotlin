plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":core:compiler.common.js"))
    api(project(":js:js.ast"))
    api(project(":js:js.parser"))
    api(project(":js:js.serializer"))
    api(project(":js:js.config"))
    api(project(":compiler:config.web"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("com.google.guava:guava"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
