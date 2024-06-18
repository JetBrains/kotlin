plugins {
    kotlin("jvm")
    id("jps-compatible")
//    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(project(":compiler:config"))
    compileOnly(intellijCore())
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
