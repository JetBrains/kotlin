plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:frontend.common"))
    api(project(":compiler:resolution.common"))
    api(project(":core:compiler.common.js"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

