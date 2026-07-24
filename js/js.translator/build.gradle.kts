plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    idea
    kotlin("jvm")
}

dependencies {
    api(project(":js:js.ast"))
    compileOnly(intellijCore())
    compileOnly(libs.guava)
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}
