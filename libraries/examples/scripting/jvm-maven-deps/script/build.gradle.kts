
plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-scripting-jvm"))
    api(project(":kotlin-scripting-dependencies"))
    api(project(":kotlin-scripting-dependencies-maven"))
    api(libs.kotlinx.coroutines.core)
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
