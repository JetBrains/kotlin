plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    application
    id("test-inputs-check")
}

group = "org.jetbrains.kdumputil"
version = "1.0.0"

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test-junit5"))
}

projectTests {
    testTask()
}

application {
    mainClass.set("MainKt")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}