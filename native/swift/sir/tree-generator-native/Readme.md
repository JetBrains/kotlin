# Swift IR tree generator

This directory contains a code generator for generating Swift IR classes.
We reuse the infrastructure for tree generators that is also used for generating [IR](../../../../compiler/ir/ir.tree/tree-generator) and
[FIR](../../../../compiler/fir/tree/tree-generator) trees.

- All tree elements are declared in [`SwiftIrTree.kt`](src/org/jetbrains/kotlin/sir/tree/generator/SwiftIrTree.kt)
- Types commonly used in configuration are listed in [`Types.kt`](src/org/jetbrains/kotlin/sir/tree/generator/Types.kt)
- If an element has no inheritors, then it will have a default implementation.
  Otherwise, you should declare an implementation that you want in
  [`ImplementationConfigurator.kt`](src/org/jetbrains/kotlin/sir/tree/generator/ImplementationConfigurator.kt).
- The same is true for builders. For each leaf element in the hierarchy, there is a corresponding builder class.
- Builders are configured in [`BuilderConfigurator.kt`](src/org/jetbrains/kotlin/sir/tree/generator/BuilderConfigurator.kt).
