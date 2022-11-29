plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

dependencies {
    implementation(project(":utilities"))
}

application {
    mainModule.set("org.gradle.sample.app")
    mainClass.set("org.gradle.sample.app.Main")
}
