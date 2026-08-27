plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":core:compiler.common"))
    api(project(":js:js.config"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

kotlin {
    explicitApi()
}
