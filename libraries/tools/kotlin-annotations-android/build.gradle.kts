import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

description = "Kotlin annotations for Android"

apply { plugin("kotlin") }

jvmTarget = "1.6"
javaHome = rootProject.extra["JDK_16"] as String

tasks.withType<KotlinCompile> {
    kotlinOptions.freeCompilerArgs += listOf(
            "-Xallow-kotlin-package",
            "-module-name", project.name
    )
}

sourceSets {
    "main" {
        projectDefault()
    }
}

runtimeJar()
dist()

javadocJar()
sourcesJar()

publish()
