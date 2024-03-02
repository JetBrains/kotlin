plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-native:backend.native"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

sourcesJar()
javadocJar()