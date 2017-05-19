# Module kotlin.test

## kotlin.test

kotlin.test provides a set of utility functions for performing assertions in tests, independently of the test framework
being used. The functions are defined as  [top-level functions in the package](kotlin.test/index.html#functions).

The test framework is abstracted through the [Asserter] class. A basic [Asserter] implementation is provided out of the
box. The `kotlin-test-junit` module provides an implementation of [Asserter] on top of JUnit. Note that the class is not intended
to be used directly from tests.
