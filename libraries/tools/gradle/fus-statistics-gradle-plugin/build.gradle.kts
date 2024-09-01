plugins {
    id("gradle-plugin-common-configuration")
}


dependencies {
    commonApi(project(":kotlin-gradle-plugin-api"))
    commonApi(project(":kotlin-gradle-plugin"))
}

tasks.named("publishPlugins") {
    enabled = false
}
