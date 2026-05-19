plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":kotlin-util-io"))
    api(project(":compiler:util"))
    implementation(project(":compiler:container"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
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
