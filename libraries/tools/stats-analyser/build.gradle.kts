plugins {
    kotlin("jvm")
    application
    id("test-inputs-check")
}

dependencies {
    implementation(project(":core:compiler.common"))
    implementation(libs.gson)
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

application {
    mainClass.set("org.jetbrains.kotlin.MainKt")
}
