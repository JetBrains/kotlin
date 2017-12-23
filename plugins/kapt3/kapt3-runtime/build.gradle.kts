description = "Kotlin Annotation Processing Runtime"

apply { plugin("kotlin") }

jvmTarget = "1.6"

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()
sourcesJar()
javadocJar()

dist(targetName = "kotlin-annotation-processing-runtime.jar")

publish()
