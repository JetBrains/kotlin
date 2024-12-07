# SIR: Swift Intermediate Representation

Swift IR or SIR is an intermediate representation that we use in Swift Export to represent the structure of Swift declarations.

SIR:
* Represents only Swift declarations. It knows nothing about function bodies.
* Does not represent all possible Swift declarations at the moment. We add new nodes when we need them.
* Is not intended to be a source-accurate representation of Swift sources like `swift-syntax`. 
  * First and foremost, it is a tool for Kotlin -> Swift translation, and designed in a most suitable way for it. 
  * We don't develop it as a general-purpose tool.

## Adding new nodes

New declaration interfaces and classes are generated automatically from DSL. See [`tree-generator`](tree-generator) for more.
