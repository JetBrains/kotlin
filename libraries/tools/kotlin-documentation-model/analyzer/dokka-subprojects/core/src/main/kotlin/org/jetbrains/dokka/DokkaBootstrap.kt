/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

import java.util.function.BiConsumer

public interface DokkaBootstrap {
    @Throws(Throwable::class)
    public fun configure(serializedConfigurationJSON: String, logger: BiConsumer<String, String>)

    @Throws(Throwable::class)
    public fun generate()
}
