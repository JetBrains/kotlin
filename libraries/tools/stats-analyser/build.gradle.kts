plugins {
    kotlin("jvm")
    id("jps-compatible")
    application
    id("test-inputs-check")
}

dependencies {
    implementation(project(":core:compiler.common"))
    implementation(libs.gson)
    testImplementation(kotlin("test"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

application {
    mainClass.set("org.jetbrains.kotlin.MainKt")
}
