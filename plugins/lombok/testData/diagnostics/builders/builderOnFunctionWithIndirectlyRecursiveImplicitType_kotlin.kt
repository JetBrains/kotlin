// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import lombok.Builder

// Currently crashes: `BuilderGenerator.addSpecialBuilderMethods` asks for the resolved return type of `create` to declare
// `build()`, while that very type is being resolved (`Unexpected returnTypeRef. Expected is FirResolvedTypeRef, but was
// FirImplicitTypeRefImplWithoutSource`; the LL runner finds unresolved elements instead).

// The same as `builderOnFunctionWithRecursiveImplicitType_kotlin.kt`, with the recursion going through another function.
class IndirectlyRecursive(val x: Int) {
    companion object {
        @Builder(builderClassName = "IndirectBuilder")
        fun create(x: Int) = helper(x)

        fun helper(x: Int) = if (x > 0) builder().x(x - 1).build() else IndirectlyRecursive(x)
    }
}
