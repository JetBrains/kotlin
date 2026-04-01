plugins {
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
