/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.api

import kotlin.script.experimental.util.typedKey

object ProcessedScriptDataProperties {
    val foundAnnotations by typedKey<List<Annotation>>()

    val foundFragments by typedKey<List<ScriptSourceNamedFragment>>()
}

