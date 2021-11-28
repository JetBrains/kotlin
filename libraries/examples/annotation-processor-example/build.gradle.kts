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

// workaround for Gradle configuration cache
// TODO: remove it when https://github.com/gradle/gradle/pull/16945 merged into used in build Gradle version
tasks.withType(PublishToMavenLocal::class.java) {
    val originalTask = this
    val serializablePublishTask =
        tasks.register(originalTask.name + "Serializable", PublishToMavenLocalSerializable::class.java) {
            publication = originalTask.publication
        }
    originalTask.onlyIf { false }
    originalTask.dependsOn(serializablePublishTask)
}