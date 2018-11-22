/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.js.backend.ast.JsProgramFragment
import org.jetbrains.kotlin.js.inline.util.FunctionWithWrapper

sealed class InlineFunctionDefinition {
    abstract val functionWithWrapper: FunctionWithWrapper

    abstract val tag: String?

    open fun process() {}

// imports / nameBindings
    // modules
    // renameFor scope?


}


// Current module, new fragment. Expressed through `defineInlineFunction`. Should be self-contained
open class PublicInlineFunctionDefinition(
    override val tag: String,
    override val functionWithWrapper: FunctionWithWrapper,
    val fragment: JsProgramFragment,
    val scope: ProgramFragmentInliningScope
) : InlineFunctionDefinition() {
    override fun process() {
        scope.process()
    }
}

// Current module, from Binary AST
class BinaryInlineFunctionDefinition(
    override val tag: String,
    override val functionWithWrapper: FunctionWithWrapper,
    val fragment: JsProgramFragment
): InlineFunctionDefinition() {


}

// Current module, new fragment, used within scope only. Private functions, lambdas.
class LocalInlineFunctionDefinition(
    override val functionWithWrapper: FunctionWithWrapper,
    val scope: InliningScope
) : InlineFunctionDefinition() {

    override val tag = null

    override fun process() {
        // TODO this is incorrect!
        scope.process()
    }
}

// Deserialized from a binary dependency (<module>.js file)
open class LibraryInlineFunctionDefinition(
    override val tag: String,
    override val functionWithWrapper: FunctionWithWrapper,
    val moduleInfo: FunctionReader.ModuleInfo
): InlineFunctionDefinition() {


}