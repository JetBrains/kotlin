plugins {
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
    id("generated-sources")
}

dependencies {
    api(project(":compiler:config"))
    api(project(":native:kotlin-native-utils"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

generatedConfigurationKeys("NativeConfigurationKeys")
