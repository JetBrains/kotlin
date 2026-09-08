plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation("org.test.example:lib:1.0.0")
}

application {
    mainClass.set("foo.MainKt")
}
