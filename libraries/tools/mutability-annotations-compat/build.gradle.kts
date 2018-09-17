description = "Compatibility artifact with Mutable and ReadOnly annotations"

plugins {
    java
    id("jps-compatible")
}

jvmTarget = "1.6"

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.6"
    targetCompatibility = "1.6"
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

dist()
