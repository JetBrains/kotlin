plugins {
    kotlin("js")
}

dependencies {
    implementation(project(":lib"))
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
