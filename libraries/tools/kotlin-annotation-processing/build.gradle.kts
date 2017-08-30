
description = "Annotation Processor wrapper for Kotlin"

apply { plugin("kotlin") }

dependencies {
    compile(project(":plugins:kapt3"))
    compileOnly("org.jetbrains.kotlin:gradle-api:1.6")
    compileOnly("com.android.tools.build:gradle:1.1.0")
    compile(projectDist(":kotlin-stdlib"))
    testCompile(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = projectDir
}

runtimeJar()
sourcesJar()
javadocJar()

dist()

publish()
