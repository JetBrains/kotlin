package org.jetbrains.dokka

import java.util.function.BiConsumer
import kotlin.jvm.Throws

interface DokkaBootstrap {
    @Throws(Throwable::class)
    fun configure(serializedConfigurationJSON: String, logger: BiConsumer<String, String>)

    @Throws(Throwable::class)
    fun generate()
}
