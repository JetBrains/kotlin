/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("JvmMetadataUtil")
package kotlinx.metadata.jvm

import kotlinx.metadata.ClassName
import kotlinx.metadata.isLocal

/**
 * JVM internal name of the class, where package names are separated by '/', and class names are separated by '$',
 * for example: `"org/foo/bar/Baz$Nested"`
 */
val ClassName.jvmInternalName: String
    get() =
        if (this.isLocal) substring(1)
        else replace('.', '$')
