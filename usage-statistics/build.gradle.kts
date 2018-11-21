
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    testCompile(intellijDep()) {
        includeJars("platform-api", rootProject = rootProject)
        isTransitive = false
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
