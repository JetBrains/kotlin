package org.jetbrains.dokka

import java.util.function.BiConsumer

interface DokkaBootstrap {

    fun configure(logger: BiConsumer<String, String>, serializedConfigurationJSON: String)

    fun generate()
}