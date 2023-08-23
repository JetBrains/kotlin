plugins {
    kotlin("jvm")
    id("jps-compatible")
}

project.updateJvmTarget("1.8")

dependencies {
    api(kotlinStdlib())
    compileOnly(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
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
