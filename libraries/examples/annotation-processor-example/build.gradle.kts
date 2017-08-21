description = "Simple Annotation Processor for testing kapt"

apply { plugin("kotlin") }

dependencies {
    compile(project(":kotlin-stdlib"))
}

sourceSets {
    "test" {}
}

