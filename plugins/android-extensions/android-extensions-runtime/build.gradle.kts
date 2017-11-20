description = "Kotlin Android Extensions Runtime"

apply { plugin("kotlin") }

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-stdlib"))
    compileOnly(commonDep("com.google.android", "android"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()
sourcesJar()
javadocJar()

dist(targetName = "android-extensions-runtime.jar")

publish()
