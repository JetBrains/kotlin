plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    id("gradle-plugin-common-configuration")
}



tasks.named("publishPlugins") {
    enabled = false
}
