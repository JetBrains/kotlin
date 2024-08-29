plugins {
    id("java-instrumentation")
    id("gradle-plugin-dependency-configuration")
    id("jps-compatible")
}

dependencies {
    api(platform(project(":kotlin-gradle-plugins-bom")))
}
