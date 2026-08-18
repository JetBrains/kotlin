/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

import java.util.*

public object DokkaVersion {
    public val version: String by lazy {
        javaClass.getResourceAsStream("/META-INF/dokka/dokka-version.properties").use { stream ->
            Properties().apply { load(stream) }.getProperty("dokka-version")
        }
    }
}
