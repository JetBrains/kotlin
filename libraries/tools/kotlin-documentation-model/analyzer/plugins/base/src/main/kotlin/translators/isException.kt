package org.jetbrains.dokka.base.translators

import org.jetbrains.dokka.links.DRI

internal fun DRI.isDirectlyAnException(): Boolean =
    toString().let { stringed ->
        stringed == "kotlin/Exception///PointingToDeclaration/" ||
                stringed == "java.lang/Exception///PointingToDeclaration/"
    }
