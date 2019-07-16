plugins {
    kotlin("jvm")
    id("jps-compatible")
}


dependencies {
    compileOnly(project(":kotlin-reflect-api"))
    compile(project(":j2k"))
    compile(project(":nj2k"))
    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))

    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }

    compileOnly(intellijDep())
    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
    }
}

sourceSets {
    "main" { projectDefault() }
}
