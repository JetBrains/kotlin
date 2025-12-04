
plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-scripting-common-fir"))
    api(project(":kotlin-scripting-common-ast"))
    api(project(":kotlin-scripting-jvm"))
    api(project(":kotlin-scripting-dependencies"))
    api(project(":kotlin-scripting-dependencies-maven"))
    api(libs.kotlinx.coroutines.core)
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
