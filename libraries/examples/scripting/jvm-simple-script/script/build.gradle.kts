
plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-scripting-jvm"))
}

kotlin {
    jvmToolchain(17)
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
