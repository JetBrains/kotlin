plugins {
    id("org.jetbrains.kotlin.jvm")
    id("gradle-plugin-api-reference") apply false
}

configureKotlinCompileTasksGradleCompatibility()

sourceSets {
    "main" {
        kotlin.srcDir("src/generated/kotlin")
    }
}

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
}

exposeSourcesForDocumentationEmbedding(setOf(kotlin.sourceSets.getByName("main")))
