
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":compiler:util"))
    compile(project(":js:js.ast"))
    compile(ideaSdkCoreDeps("intellij-core"))
    compile(preloadedDeps("json-org"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

