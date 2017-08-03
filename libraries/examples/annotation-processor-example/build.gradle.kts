description = "Simple Annotation Processor for testing kapt"

apply { plugin("kotlin") }

dependencies {
    val compile by configurations
    compile(project(":kotlin-stdlib"))
}

configureKotlinProjectSources("src/kotlin")
configureKotlinProjectResources("src/resources")
configureKotlinProjectNoTests()
