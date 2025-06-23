# Infrastructure for Native GC fuzz testing

_**NOTE**: This is a work-in-progress._

## Quick look

The language which the fuzzer generates is defined in [DSL.kt](./engine/testFixtures/org/jetbrains/kotlin/konan/test/gcfuzzing/dsl/DSL.kt).

Examples of the programs in that language are in [GCFuzzingDSLTest.kt](./engine/tests/org/jetbrains/kotlin/konan/test/gcfuzzing/GCFuzzingDSLTest.kt) and the
expected Kotlin+ObjC sources are [here](./engine/testData/gcFuzzingDSLTest).

## DSL

The fuzzer generates programs in a special language. The language is designed to produce programs
that always compile and successfully run, so any failures can be directly attributed to bugs in GC.

At a glance:
- One can define the following entities:
  - classes (contains only a sequence of fields)
  - globals
  - functions
- Each of these entities is set to target a language. Currently:
  - ObjC to generate a header for cinterop
  - Kotlin to import cinterop module and generate ObjC Export framework
  - ObjC again to implement stuff defined in the header and consume the framework
- Addressing any entity is performed not by name, but by an integer
  - this integer is then interpreted as an index among matching declarations, wrapping around on overflow
  - for example: trying to access field `5` of an object with `3` fields will lead to accessing field `5 mod 3 = 2`
- In function bodies there are the following operations:
  - allocate an object
  - create a new local variable
  - update a global/local variable, optionally traversing through their fields
  - call a function
  - spawn a new thread (immediately starting to execute a function)
- The main goal is to run some code that modifies heap, not make sense, and so:
  - the only failure condition is a crash (or a runtime assertion)
  - timeouts are okay: as long as the program didn't crash, it's okay if we didn't finish it
  - stack overflow (e.g. from infinite recursion) is avoided by tracking the stack size and immediately returning from functions that allocate more
  - in the same way, the maximum amount of concurrently working threads is limited; when the limit is reached, spawn thread instructions are ignored
  - we can get data races by concurrently updating global objects, the GC should survive this
  - these races are not handled by ObjC, so every field and every global is atomically updated (automatically by using `@property`)
  - OOM is avoided by refusing to do new allocations when memory pressure is high

Future directions:
- more entities and operations
  - thread locals
  - primitive types
  - weak references
  - immutable fields/variables
- more execution environments
  - Swift Export
  - C Export
- non-fuzzable parts of DSL with internal intrinsics to replace tests currently written in C++
- fuzzing GC configuration (e.g. the scheduler constants), not just the code