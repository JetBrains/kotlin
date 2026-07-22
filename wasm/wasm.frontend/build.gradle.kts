plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:config"))
    api(project(":wasm:wasm.config"))
    implementation(project(":compiler:frontend.common"))
    implementation(project(":core:names"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

optInToK1Deprecation()

generatedConfigurationKeys("WasmConfigurationKeys", dependOnTaskOutput = true)
