plugins {
    kotlin("jvm")
    application
}

application {
    mainClass = "MainKt"
}

dependencies {
    implementation(project(":litmuskt:core"))
    implementation(project(":litmuskt:testsuite"))
    implementation(kotlin("reflect"))
}
