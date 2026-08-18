/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.model.DModule

public fun interface DocumentableMerger {
    public operator fun invoke(modules: Collection<DModule>): DModule?
}

