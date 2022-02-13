plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    implementation(kotlinStdlib())
}

publish()
