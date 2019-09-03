
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(kotlinStdlib())
    compileOnly(project(":kotlin-reflect-api"))
    compile(commonDep("org.jetbrains.kotlinx", "kotlinx-coroutines-core")) { isTransitive = false }
    runtime(project(":kotlin-reflect"))
    testCompile(commonDep("junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
