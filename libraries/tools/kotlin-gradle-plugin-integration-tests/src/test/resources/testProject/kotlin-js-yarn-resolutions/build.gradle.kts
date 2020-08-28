plugins {
    kotlin("js").version("<pluginMarkerVersion>")
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
}

rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin::class.java) {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
        resolution("lodash") {
            include("^1.0.0")
            exclude("~1.2.1", "1.3.0 - 1.4.0")
        }
    }
}

kotlin {
    js {
        useCommonJs()
        nodejs {
        }
    }
}