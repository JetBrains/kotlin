pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    maven ("https://dl.bintray.com/kotlin/kotlin-eap")
  }
}

rootProject.name = "kotlin-power-assert"

include(":kotlin-power-assert")
include(":kotlin-power-assert-gradle")
