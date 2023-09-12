plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(kotlin("stdlib-common"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
