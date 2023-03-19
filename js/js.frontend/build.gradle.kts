plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":kotlin-util-io"))
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":core:compiler.common.js"))
    api(project(":js:js.ast"))
    api(project(":js:js.parser"))
    api(project(":js:js.serializer"))
    api(project(":js:js.config"))
    compileOnly(intellijCore())
    compileOnly(commonDependency("com.google.guava:guava"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
