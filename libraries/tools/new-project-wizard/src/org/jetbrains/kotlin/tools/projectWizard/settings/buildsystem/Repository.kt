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

data class BintrayRepository(val repository: String, val base: String = "https://dl.bintray.com") : CustomMavenRepository {
    override val url: String = "$base/$repository"

    override val idForMaven: String
        get() = "bintray." + repository.replace('/', '.')
}

data class JetBrainsSpace(val repository: String) : CustomMavenRepository {
    override val url: String = "https://maven.pkg.jetbrains.space/public/p/$repository"

    override val idForMaven: String
        get() = "jetbrains." + repository.replace('/', '.')
}

object Repositories {
    val KTOR_BINTRAY = BintrayRepository("kotlin/ktor")
    val KOTLINX = BintrayRepository("kotlin/kotlinx")
    val KOTLIN_JS_WRAPPERS_BINTRAY = BintrayRepository("kotlin/kotlin-js-wrappers")
    val KOTLIN_EAP_BINTRAY = BintrayRepository("kotlin/kotlin-eap")
    val KOTLIN_DEV_BINTRAY = BintrayRepository("kotlin/kotlin-dev")
    val JETBRAINS_COMPOSE_DEV = JetBrainsSpace("compose/dev")
}
