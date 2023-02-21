plugins {
    kotlin("js")
}

dependencies {
    implementation(project(":lib"))
    testImplementation(kotlin("test-js"))
}

kotlin {
    js {
        browser {
        }
        binaries.executable()
    }
}

configurations["compileClasspath"].apply {
    attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "kotlin-runtime"))
}
