plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.configureJvmToolchain(JdkMajorVersion.JDK_1_8)

dependencies {
    api(project(":kotlin-script-runtime"))
    api(kotlinStdlib())
    api(project(":kotlin-scripting-common"))
    testImplementation(libs.junit4)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>> {
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xallow-kotlin-package"
    )
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
