description = "Simple Annotation Processor for testing kapt"

apply {
    plugin("kotlin")
    plugin("maven") // only used for installing to mavenLocal()
}

dependencies {
    compile(projectDist(":kotlin-stdlib"))
}

sourceSets {
    "test" {}
}

