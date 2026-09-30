/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.templates

import kotlin.script.experimental.annotations.KotlinScript

/**
 * Script base class with the command-line arguments available as [args]; the default for regular `.kts` scripts.
 */
@KotlinScript
abstract class ScriptWithArgs(val args: Array<String>)

/**
 * Script base class with generic name-to-value [bindings]; also used as the implicit receiver in JSR-223 scripts.
 */
@KotlinScript
abstract class ScriptWithBindings(val bindings: Map<String, Any?>)
