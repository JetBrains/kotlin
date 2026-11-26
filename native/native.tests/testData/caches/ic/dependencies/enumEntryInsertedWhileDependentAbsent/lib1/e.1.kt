package lib1

// NEW is inserted BEFORE B, so B's ordinal changes 0 -> 1 and the when dispatch baked into
// lib2's step-0 cache answers from the wrong branch.
enum class E {
    NEW, B, C
}
