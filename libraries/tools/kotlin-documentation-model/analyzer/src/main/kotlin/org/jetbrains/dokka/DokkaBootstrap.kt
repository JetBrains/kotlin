/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

import java.util.function.BiConsumer

internal interface DokkaBootstrap {
    @Throws(Throwable::class)
    fun configure(serializedConfigurationJSON: String, logger: BiConsumer<String, String>)

    @Throws(Throwable::class)
    fun generate()
}
