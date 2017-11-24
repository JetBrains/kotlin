
description = "Annotation Processor for Kotlin"

apply { plugin("kotlin") }

configureIntellijPlugin {
    setExtraDependencies("intellij-core")
}

dependencies {
    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:plugin-api"))

    testCompile(project(":compiler:tests-common"))
    testCompile(projectTests(":compiler:tests-common"))

    compileOnly(project(":kotlin-annotation-processing-runtime"))

    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-annotation-processing-runtime"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellijCoreJar())
        compileOnly(intellij { include("asm-all.jar") })
        testCompile(intellijCoreJar())
        testCompile(intellij { include("idea.jar", "idea_rt.jar", "openapi.jar") })
    }
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
