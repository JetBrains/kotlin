plugins {
    kotlin("js")
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
