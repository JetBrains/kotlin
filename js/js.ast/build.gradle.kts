
apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(ideaSdkCoreDeps("trove4j", "intellij-core"))
}

configureKotlinProjectSourcesDefault()
configureKotlinProjectNoTests()

