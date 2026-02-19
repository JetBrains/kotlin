plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":compiler:resolution.common"))
    api(project(":core:compiler.common.js"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

