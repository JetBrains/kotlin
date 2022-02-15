import org.jetbrains.kotlin.pill.PillExtension

description = "Simple Annotation Processor for testing kapt"

plugins {
    kotlin("jvm")
    `maven-publish` // only used for installing to mavenLocal()
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

dependencies {
    api(kotlinStdlib())
}

sourceSets {
    "test" {}
}

publishing {
    publications {
        create<MavenPublication>("main") {
            from(components["java"])
        }
    }
}

tasks.register("install") {
    dependsOn(tasks.named("publishToMavenLocal"))
}