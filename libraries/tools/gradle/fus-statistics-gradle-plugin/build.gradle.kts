plugins {
    id("root-config")
    id("gradle-plugin-common-configuration")
}



tasks.named("publishPlugins") {
    enabled = false
}
