plugins {
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:util"))
    implementation(project(":compiler:container"))
    implementation(project(":compiler:frontend"))
    implementation(project(":compiler:resolution"))
    implementation(project(":core:descriptors"))
    implementation(project(":js:js.frontend"))
    api(project(":wasm:wasm.config"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

generatedConfigurationKeys("WasmConfigurationKeys")
