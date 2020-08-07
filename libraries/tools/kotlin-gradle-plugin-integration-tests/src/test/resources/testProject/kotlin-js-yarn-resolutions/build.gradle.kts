plugins {
    kotlin("js").version("<pluginMarkerVersion>")
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    constraints {
        implementation(org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyConstraint(project, "lodash", "^1.0.0")) {
            version {
                reject("~1.2.1", "1.3.0 - 1.4.0")
            }
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