
apply { plugin("kotlin") }

configureIntellijPlugin {
    setPlugins("junit", "gradle", "Groovy", "android",
               "maven", // TODO: check whether it works in AS (it was marked optional before
               "properties")
}

dependencies {
    compile(projectDist(":kotlin-stdlib"))
    compile(project(":core:util.runtime"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:light-classes"))
    testCompile(projectDist(":kotlin-test:kotlin-test-jvm"))
    testCompile(projectTests(":compiler:tests-common"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":compiler:util"))
    testCompile(project(":compiler:cli"))
    testCompile(project(":idea:idea-test-framework"))
    testRuntime(projectDist(":kotlin-reflect"))
    testRuntime(project(":idea:idea-android"))
    testRuntime(project(":idea:idea-gradle"))
    testRuntime(project(":plugins:kapt3-idea")) { isTransitive = false }
    testRuntime(project(":sam-with-receiver-ide-plugin"))
    testRuntime(project(":allopen-ide-plugin"))
    testRuntime(project(":noarg-ide-plugin"))
    testRuntime(project(":plugins:android-extensions-ide"))
}

afterEvaluate {
    dependencies {
        compileOnly(intellij { include("openapi.jar", "idea.jar", "util.jar", "extensions.jar", "asm-all.jar") })
        compileOnly(intellijPlugins("junit", "gradle", "Groovy", "android",
                                    "maven", // TODO: check whether it works in AS (it was marked optional before
                                    "properties"))
        testCompileOnly(intellij { include("idea_rt.jar") })
        testCompile(intellijPlugins("junit", "gradle", "Groovy", "android", "maven", "properties"))
        testRuntime(intellij())
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    workingDir = rootDir
}
