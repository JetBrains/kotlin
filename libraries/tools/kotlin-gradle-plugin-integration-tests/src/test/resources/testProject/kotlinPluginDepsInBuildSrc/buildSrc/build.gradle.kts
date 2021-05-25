plugins {
    kotlin("jvm") version "<pluginMarkerVersion>"
}

repositories {
    mavenCentral()
    mavenLocal()
}

allprojects {
    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:<pluginMarkerVersion>")
    }
}