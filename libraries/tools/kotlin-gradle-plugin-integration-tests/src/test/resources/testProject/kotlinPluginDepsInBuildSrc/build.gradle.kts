import foo.bar.configureFromBuildSrc

plugins {
    kotlin("jvm") version "<pluginMarkerVersion>"
}

kotlin {
    sourceSets {
        main {
            languageSettings {
                configureFromBuildSrc()
            }
        }
    }
}