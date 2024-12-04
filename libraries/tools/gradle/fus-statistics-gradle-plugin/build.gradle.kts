plugins {
    id("gradle-plugin-common-configuration")
}



tasks.named("publishPlugins") {
    enabled = false
}
