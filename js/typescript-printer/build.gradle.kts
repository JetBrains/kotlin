plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":js:js.serializer"))
    api(project(":js:typescript-export-model"))

    implementation(project(":core:util.runtime"))
    implementation(project(":js:js.ast"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

kotlin {
    explicitApi()
}
