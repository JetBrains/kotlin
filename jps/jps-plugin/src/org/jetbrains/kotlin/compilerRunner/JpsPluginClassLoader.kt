/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import java.net.URL
import java.util.*

/**
    ClassLoader that prevents a [ServiceLoader] from loading services available in parent [ClassLoader]s.
 */
class JpsPluginClassLoader(parent: ClassLoader) : ClassLoader(parent) {
    private companion object {
        private const val SERVICE_DIRECTORY_LOCATION = "META-INF/services/"
    }

    override fun getResource(name: String): URL? {
        if (name.startsWith(SERVICE_DIRECTORY_LOCATION)) {
            return findResource(name)
        }

        return super.getResource(name)
    }

    override fun getResources(name: String): Enumeration<URL> {
        if (name.startsWith(SERVICE_DIRECTORY_LOCATION)) {
            return findResources(name)
        }

        return super.getResources(name)
    }
}