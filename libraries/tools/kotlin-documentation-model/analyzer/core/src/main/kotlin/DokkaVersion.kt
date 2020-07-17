package org.jetbrains.dokka

import java.util.*

object DokkaVersion {
    val version: String by lazy {
        val stream = javaClass.getResourceAsStream("/META-INF/dokka/dokka-version.properties")
        Properties().apply { load(stream) }.getProperty("dokka-version")
    }
}
