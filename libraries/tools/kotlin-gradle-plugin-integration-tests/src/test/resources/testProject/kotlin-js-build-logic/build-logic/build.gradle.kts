plugins {
    `embedded-kotlin`
    id("java-gradle-plugin")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val kotlin_version by properties

dependencies {
    compileOnly(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
}

gradlePlugin {
    plugins {
        register("build.logic") {
            id = "build.logic"
            // This plugin is only used for loading the jar using the Marker but never applied
            // We don't need it.
            implementationClass = "build.logic.Unused"
        }
    }
}