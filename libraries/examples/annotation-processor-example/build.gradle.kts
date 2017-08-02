description = "Simple Annotation Processor for testing kapt"

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(kotlinDep("stdlib"))
}

configureKotlinProjectSources("src/kotlin")
configureKotlinProjectResources("src/resources")
configureKotlinProjectNoTests()
