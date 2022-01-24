package org.jetbrains.dokka

import java.util.*

object DokkaVersion {
    val version: String by lazy {
        javaClass.getResourceAsStream("/META-INF/dokka/dokka-version.properties").use { stream ->
            Properties().apply { load(stream) }.getProperty("dokka-version")
        }
    }
}
