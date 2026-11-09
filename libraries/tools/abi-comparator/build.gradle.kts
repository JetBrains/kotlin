plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

dependencies {
    implementation(libs.intellij.asm)
    implementation(libs.apache.commons.text)

    implementation(project(":tools:kotlinp-jvm"))
    implementation(project(":kotlin-metadata-jvm"))
    implementation(project(":kotlin-metadata"))

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    constraints {
        api(libs.apache.commons.lang)
    }
}
