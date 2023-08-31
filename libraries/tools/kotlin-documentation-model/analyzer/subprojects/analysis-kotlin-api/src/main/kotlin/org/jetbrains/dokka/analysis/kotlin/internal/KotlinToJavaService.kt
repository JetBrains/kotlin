/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.internal

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.links.DRI

@InternalDokkaApi
public interface KotlinToJavaService {
    public fun findAsJava(kotlinDri: DRI): DRI?
}
