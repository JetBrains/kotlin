plugins {
    kotlin("jvm")
}

kotlin {
    sourceSets.configureEach {
        languageSettings.languageVersion = "1.4"
        languageSettings.apiVersion = "1.4"
    }
}

dependencies {
    implementation(kotlinStdlib())
}

publish()
