plugins {
    idea
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":js:js.ast"))
    compileOnly(intellijCore())
    compileOnly(libs.guava)
}

sourceSets {
    "main" {
        projectDefault()
        java.srcDir("../js.inliner/src")
    }
    "test" {}
}
