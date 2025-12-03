plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:util.runtime"))

    api(kotlinStdlib())
    compileOnly(intellijCore())
    compileOnly(libs.intellij.fastutil)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
