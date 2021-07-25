import org.jetbrains.kotlin.pill.PillExtension

description = "Kotlin metadata manipulation library"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_6)

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

dependencies {
    compile(kotlinStdlib())
    compileOnly(project(":core:metadata"))
    compileOnly(protobufLite())
}
