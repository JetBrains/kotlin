plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(project(":core:language.targets.wasm"))
    api(project(":compiler:config"))
}

sourceSets {
    "main" { projectDefault() }
}
