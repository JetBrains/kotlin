plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0"

rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension>().apply {
        overrides2.register("lodash") {
            range.set(">=1.0.0 <1.2.1 || >1.4.0 <2.0.0")
        }
        overrides2.register("react") {
            range.set("16.0.0")
        }
//        override("lodash") {
//            include("^1.0.0")
//            exclude("~1.2.1", "1.3.0 - 1.4.0")
//        }
//        override("react", "16.0.0")
    }
}

kotlin {
    js {
        useCommonJs()
        nodejs {
        }
    }
}
