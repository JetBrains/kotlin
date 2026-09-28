// KIND: STANDALONE
// MODULE: SealedNesting
// FILE: nested_root.kt

// The root and its inheritors sit at the bottom of a `class -> interface -> object` chain, which
// Swift Export flattens into one declaration; the cases must stay reachable through the nested path.

class Outer {
    interface Middle {
        object Inner {
            sealed interface Root

            class Leaf : Root {
                override fun toString(): String = "Leaf"
            }

            object LeafObject : Root {
                override fun toString(): String = "LeafObject"
            }

            /** Not exported: forces the `unknown` case. */
            internal class Hidden : Root {
                override fun toString(): String = "Hidden"
            }
        }
    }
}

fun createNestedLeaf(): Outer.Middle.Inner.Root = Outer.Middle.Inner.Leaf()

fun createNestedLeafObject(): Outer.Middle.Inner.Root = Outer.Middle.Inner.LeafObject

fun createNestedHidden(): Outer.Middle.Inner.Root = Outer.Middle.Inner.Hidden()

// FILE: split_inheritors.kt

// A root nested two levels down whose inheritors sit at *different* nesting sites: top level, a
// deeper container elsewhere, and a non-exported top-level class.

object Holder {
    interface Nested {
        sealed interface SplitRoot
    }
}

class SplitTopLevel : Holder.Nested.SplitRoot {
    override fun toString(): String = "SplitTopLevel"
}

object OtherHolder {
    class Deeper {
        class SplitDeeper : Holder.Nested.SplitRoot {
            override fun toString(): String = "SplitDeeper"
        }
    }
}

/** Not exported. */
internal class SplitHidden : Holder.Nested.SplitRoot {
    override fun toString(): String = "SplitHidden"
}

fun createSplitTopLevel(): Holder.Nested.SplitRoot = SplitTopLevel()

fun createSplitDeeper(): Holder.Nested.SplitRoot = OtherHolder.Deeper.SplitDeeper()

fun createSplitHidden(): Holder.Nested.SplitRoot = SplitHidden()
