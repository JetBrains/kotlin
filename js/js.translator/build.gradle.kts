
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":core"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:backend-common"))
    compile(project(":js:js.ast"))
    compile(project(":js:js.frontend"))
    compile(project(":js:js.parser"))
    compile(ideaSdkCoreDeps("intellij-core"))
}

configureKotlinProjectSources("js.translator/src", "js.inliner/src", sourcesBaseDir = File(rootDir, "js"))
configureKotlinProjectNoTests()

