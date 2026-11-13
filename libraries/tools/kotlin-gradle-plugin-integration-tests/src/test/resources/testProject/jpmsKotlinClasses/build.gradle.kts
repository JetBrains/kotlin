plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

application {
    mainModule.set("org.example.app")
    mainClass.set("org.example.app.Main")
}
