// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB

import lombok.Builder

// Currently crashes: `BuilderGenerator.addSpecialBuilderMethods` asks for the resolved return type of `create` to declare
// `build()`, while that very type is being resolved (`Unexpected returnTypeRef. Expected is FirResolvedTypeRef, but was
// FirImplicitTypeRefImplWithoutSource`; the LL runner finds unresolved elements instead).

// The implicit return type of `create` depends on the return type of `build()`, which is the return type of `create`.
class Recursive(val x: Int) {
    companion object {
        @Builder(builderClassName = "RecursiveBuilder")
        fun create(x: Int) = if (x > 0) builder().x(x - 1).build() else Recursive(x)
    }
}
