import org.jetbrains.kotlin.pill.PillExtension

description = "Simple Annotation Processor for testing kapt"

plugins {
    kotlin("jvm")
    maven // only used for installing to mavenLocal()
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

dependencies {
    compile(kotlinStdlib())
}

sourceSets {
    "test" {}
}