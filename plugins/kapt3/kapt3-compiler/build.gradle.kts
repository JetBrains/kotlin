
description = "Annotation Processor for Kotlin"

apply { plugin("kotlin") }

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:plugin-api"))
    compileOnly(project(":kotlin-annotation-processing-runtime"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all") }

    testCompile(project(":compiler:tests-common"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
    testCompile(intellijCoreDep()) { includeJars("intellij-core") }
    testCompile(intellijDep()) { includeJars("idea", "idea_rt", "openapi") }
    testCompile(project(":kotlin-annotation-processing-runtime"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar {
    from(getSourceSetsFrom(":kotlin-annotation-processing-runtime")["main"].output.classesDirs)
}

testsJar {}

projectTest {
    workingDir = rootDir
    dependsOnTaskIfExistsRec("dist", project = rootProject)
}

runtimeJar()
sourcesJar()
javadocJar()

dist()

publish()
