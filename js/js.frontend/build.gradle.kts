plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(project(":kotlin-util-io"))
    api(project(":compiler:util"))
    api(project(":compiler:frontend"))
    api(project(":core:compiler.common.js"))
    implementation(project(":core:compiler.common.web"))
    api(project(":js:js.ast"))
    api(project(":js:js.parser"))
    api(project(":js:js.serializer"))
    api(project(":js:js.frontend.common"))
    compileOnly(intellijCore())
    compileOnly(libs.guava)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
