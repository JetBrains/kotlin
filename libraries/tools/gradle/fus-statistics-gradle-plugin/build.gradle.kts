plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    id("gradle-plugin-common-configuration")
}



tasks.named("publishPlugins") {
    enabled = false
}
