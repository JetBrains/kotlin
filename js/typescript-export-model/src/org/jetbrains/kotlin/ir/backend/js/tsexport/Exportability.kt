/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.tsexport

public sealed class Exportability {
    public object Allowed : Exportability()
    public object NotNeeded : Exportability()
    public object Implicit : Exportability()
    public class Prohibited(public val reason: String) : Exportability()
}