plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":js:typescript-export-model"))

    implementation(project(":core:util.runtime"))
    implementation(project(":js:js.ast"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

kotlin {
    explicitApi()
}
