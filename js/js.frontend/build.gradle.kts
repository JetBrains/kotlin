plugins {
    kotlin("jvm")
    id("jps-compatible")
    id("generated-sources")
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

generatedConfigurationKeys("JSConfigurationKeys")
