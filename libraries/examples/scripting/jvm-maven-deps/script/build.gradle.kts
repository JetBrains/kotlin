
plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-scripting-jvm"))
    api(project(":kotlin-scripting-dependencies"))
    api(project(":kotlin-scripting-dependencies-maven"))
    api(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
