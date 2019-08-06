import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.pill.PillExtension

description = "Kotlin annotations for Android"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

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

dependencies {
    compileOnly(kotlinBuiltins())
}

publish()

sourcesJar()
javadocJar()
runtimeJar()
