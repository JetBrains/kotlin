description = "Kotlin metadata manipulation library"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

dependencies {
    compile(project(":kotlin-stdlib"))
    compileOnly(project(":core:metadata"))
    compileOnly(protobufLite())
}
