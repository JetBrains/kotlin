import org.jetbrains.kotlin.pill.PillExtension

description = "Kotlin metadata manipulation library"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

sourceSets {
    "main" { projectDefault() }
    "test" { }
}

dependencies {
    compile(kotlinStdlib())
    compileOnly(project(":core:metadata"))
    compileOnly(protobufLite())
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions {
        freeCompilerArgs += "-Xsuppress-deprecated-jvm-target-warning"
    }
}
