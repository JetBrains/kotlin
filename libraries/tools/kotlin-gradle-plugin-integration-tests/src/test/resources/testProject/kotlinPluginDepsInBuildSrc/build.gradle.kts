import foo.bar.configureFromBuildSrc

plugins {
    kotlin("jvm")
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