plugins {
    id("root-config")
    kotlin("jvm")
    id("gradle-plugin-compiler-dependency-configuration")
}

dependencies {
    api(project(":compiler:config"))
}

sourceSets {
    "main" { projectDefault() }
}
