# SIR Light Classes

Lazy implementation of SIR nodes, heavily inspired by Kotlin/JVM light classes. Laziness is a great thing for intermediate representations
when it comes to IDE use-cases. It forces a so-called pull-based approach when computations like "what children nodes contain this declaration container" 
performed only when needed, keeping CPU and memory usage low.

Of course, this approach comes with a price in terms of usability. For example, Swift and Kotlin have different rules about what container
can contain what like it is possible to declare classes inside protocols in Kotlin 2.0, but it is not possible to nest class inside protocol
in Swift 5.10. Thus, when translating such Kotlin constructs to Swift, we have to pull class declaration to the parent scope, which means that
parent SIR scope has to be changed after the first traversal.