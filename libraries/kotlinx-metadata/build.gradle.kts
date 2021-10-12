description = "Kotlin metadata manipulation library"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_6)

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

dependencies {
    api(kotlinStdlib())
    compileOnly(project(":core:metadata"))
    compileOnly(protobufLite())
}
