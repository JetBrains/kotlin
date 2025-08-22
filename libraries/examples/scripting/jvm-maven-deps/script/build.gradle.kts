
plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-scripting-jvm"))
    api(project(":kotlin-scripting-dependencies"))
    api(project(":kotlin-scripting-dependencies-maven"))
    api(libs.intellij.kotlinx.coroutines.core)
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
