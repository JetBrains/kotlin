description = "Kotlin Annotation Processing Runtime"

apply { plugin("kotlin") }
apply { plugin("jps-compatible") }

dependencies {
    compile(projectDist(":kotlin-stdlib"))
}

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
