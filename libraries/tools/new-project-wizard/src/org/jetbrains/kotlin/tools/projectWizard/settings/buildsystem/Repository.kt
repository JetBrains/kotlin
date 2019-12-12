package org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem

interface Repository

data class DefaultRepository(val type: Type) : Repository {
    enum class Type {
        JCENTER, MAVEN_CENTRAL, GOOGLE, GRADLE_PLUGIN_PORTAL
    }

    companion object  {
        val JCENTER = DefaultRepository(Type.JCENTER)
        val MAVEN_CENTRAL = DefaultRepository(Type.MAVEN_CENTRAL)
        val GOOGLE = DefaultRepository(Type.GOOGLE)
        val GRADLE_PLUGIN_PORTAL = DefaultRepository(Type.GRADLE_PLUGIN_PORTAL)
    }
}
