description = "Kotlin metadata manipulation library"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

dependencies {
    api(kotlinStdlib())
    compileOnly(project(":core:metadata"))
    compileOnly(protobufLite())
}

kotlin {
    explicitApi()
    compilerOptions {
        freeCompilerArgs.add("-Xallow-kotlin-package")
    }
}
