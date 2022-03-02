plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
}
