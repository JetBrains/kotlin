plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:util"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:serialization"))
    implementation(project(":core:descriptors"))
    implementation(project(":core:deserialization"))
    api(project(":js:js.ast"))
    api(project(":js:js.config"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
