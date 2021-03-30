package org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem

interface Repository {
    val url: String
    val idForMaven: String
}

data class DefaultRepository(val type: Type) : Repository {
    override val url: String
        get() = type.url

    override val idForMaven: String
        get() = type.gradleName

    enum class Type(val gradleName: String, val url: String) {
        JCENTER("jcenter", "https://jcenter.bintray.com/"),
        MAVEN_CENTRAL("mavenCentral", "https://repo1.maven.org/maven2/"),
        GOOGLE("google", "https://dl.google.com/dl/android/maven2/"),
        GRADLE_PLUGIN_PORTAL("gradlePluginPortal", "https://plugins.gradle.org/m2/")
    }

    companion object {
        val JCENTER = DefaultRepository(Type.JCENTER)
        val MAVEN_CENTRAL = DefaultRepository(Type.MAVEN_CENTRAL)
        val GOOGLE = DefaultRepository(Type.GOOGLE)
        val GRADLE_PLUGIN_PORTAL = DefaultRepository(Type.GRADLE_PLUGIN_PORTAL)
    }
}


interface CustomMavenRepository : Repository

data class CustomMavenRepositoryImpl(val repository: String, val base: String) : CustomMavenRepository {
    override val url: String = "$base/$repository"

    override val idForMaven: String
        get() = "bintray." + repository.replace('/', '.')
}

data class JetBrainsSpace(val repository: String) : CustomMavenRepository {
    override val url: String = "https://maven.pkg.jetbrains.space/$repository"

    override val idForMaven: String
        get() = "jetbrains." + repository.replace('/', '.')
}

object Repositories {
    val KTOR = DefaultRepository.MAVEN_CENTRAL
    val KOTLINX_HTML = JetBrainsSpace("public/p/kotlinx-html/maven")
    val KOTLIN_JS_WRAPPERS_BINTRAY = JetBrainsSpace("kotlin/p/kotlin/kotlin-js-wrappers")
    val KOTLIN_EAP_MAVEN_CENTRAL = DefaultRepository.MAVEN_CENTRAL
    val JETBRAINS_COMPOSE_DEV = JetBrainsSpace("public/p/compose/dev")
    val JETBRAINS_KOTLIN_DEV = JetBrainsSpace("kotlin/p/kotlin/dev")
}
