
plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-scripting-jvm"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
