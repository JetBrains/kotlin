plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":compiler:backend.wasm"))
}

sourceSets {
    "main" { projectDefault() }
}

val run by task<JavaExec> {
    classpath = sourceSets["main"].runtimeClasspath
    main = "GenerateWasmRuntimeKt"
    args = listOf("$rootDir")
}