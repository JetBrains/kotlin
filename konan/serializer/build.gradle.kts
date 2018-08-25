plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Kotlin/Native deserializer and library reader"

jvmTarget = "1.6"

dependencies {
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":konan:konan-metadata"))
    compile(project(":konan:konan-utils"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
