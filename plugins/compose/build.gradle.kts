plugins {
    kotlin("jvm")
}

sourceSets {
    "main" { projectDefault() }
}

dependencies {
    implementation(project(":kotlin-stdlib"))
}

tasks.register("composeReleaseNotes", JavaExec::class.java) {
    classpath(sourceSets.main.map { it.runtimeClasspath })

    description = "Generate Compose plugin release notes"
    mainClass = "GenerateReleaseNotesKt"
}

description = "Compose compiler plugin"