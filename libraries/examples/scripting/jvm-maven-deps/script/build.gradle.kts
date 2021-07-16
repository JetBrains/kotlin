
plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":kotlin-scripting-jvm"))
    compile(project(":kotlin-scripting-dependencies"))
    compile(project(":kotlin-scripting-dependencies-maven"))
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}
