description = "Compatibility artifact with Mutable and ReadOnly annotations"

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    java
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}
