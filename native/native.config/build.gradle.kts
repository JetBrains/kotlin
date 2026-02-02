plugins {
    kotlin("jvm")
    id("generated-sources")
}

dependencies {
    api(project(":core:compiler.common")) { isTransitive = false }
    api(project(":compiler:config")) { isTransitive = false }
    api(project(":native:kotlin-native-utils"))
    compileOnly(intellijCore())

    compileOnly(project(":core:metadata")) { isTransitive = false }
    embedded(project(":core:metadata")) { isTransitive = false }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

generatedConfigurationKeys("NativeConfigurationKeys")
