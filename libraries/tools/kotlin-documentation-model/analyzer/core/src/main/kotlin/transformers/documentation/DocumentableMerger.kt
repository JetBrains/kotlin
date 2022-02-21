package org.jetbrains.dokka.transformers.documentation

import org.jetbrains.dokka.model.DModule

fun interface DocumentableMerger {
    operator fun invoke(modules: Collection<DModule>): DModule?
}