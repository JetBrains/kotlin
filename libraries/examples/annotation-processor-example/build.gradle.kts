description = "Simple Annotation Processor for testing kapt"

apply { plugin("kotlin") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
}

sourceSets {
    "test" {}
}

