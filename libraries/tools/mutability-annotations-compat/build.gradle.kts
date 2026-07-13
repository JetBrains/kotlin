description = "Compatibility artifact with Mutable and ReadOnly annotations"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    java
}

project.updateJvmTarget("1.8")

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
