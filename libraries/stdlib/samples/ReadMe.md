## Samples for the Standard Library

This project contains samples for the standard library functions. 
They are located in the [test](test) source root and each sample is written like a small unit test.
Then these samples are referenced in the documentation of the standard library functions 
with the [`@sample`](https://kotlinlang.org/docs/reference/kotlin-doc.html#block-tags) tag and are embedded in the generated documentation as runnable samples.


### Guides for sample authoring

Note that samples, even though they are written as unit tests, are not conceptually the same as unit tests. 
Unlike a unit test, which usually explores corner cases of a function being tested, 
the goal of a sample is to show a common use case of a function.

Please see the existing samples for an inspiration on authoring new ones.

- Do not add `Test` postfix or prefix to the name of a sample container class or sample method.

- There's no hard restriction that each function should get its own sample. Several closely related functions can be illustrated with one sample, 
for example one sample can show the usage of `nullsFirst` and `nullsLast` comparators.
  
- For the functions that are generated from a template (usually they are placed in the `generated` directory) the sample reference should be placed
in the template, and then all specializations should be regenerated. See [the standard library generator](https://github.com/JetBrains/kotlin/tree/master/libraries/tools/kotlin-stdlib-gen) for details.
 
- It's possible to provide a single sample for all primitive specializations of a function in case if its usage doesn't change significantly
depending on the specialization. 

- Each sample should be self contained, but you can introduce local classes and functions in it.
Do not use external references, other than the Standard Library itself and JDK.

- Use only the following subset of assertions:

    - `assertPrints` to show any printable value,
    - `assertTrue`/`assertFalse` to show a boolean value,
    - `assertFails` / `assertFailsWith` to show that some invocation will fail.
  
  When a sample is compiled and run during the build these assertions work as usual test assertions.
  When the sample is transformed to be embedded in docs, these assertions are either replaced with `println` with the comment showing its 
  expected output, or commented out with `//` — this is used for `assertFails` / `assertFailsWith` to prevent execution of its failing block 
  of code. 
