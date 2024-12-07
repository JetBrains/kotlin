# Module kotlin-stdlib

## Kotlin Standard Library

The Kotlin Standard Library provides living essentials for everyday work with Kotlin.
These include:
  - Higher-order functions implementing idiomatic patterns ([let][kotlin.let], [apply][kotlin.apply], [use][kotlin.io.use], [synchronized][kotlin.synchronized], etc).
  - Extension functions providing querying operations for collections (eager) and sequences (lazy).
  - Various utilities for working with strings and char sequences.
  - Extensions for JDK classes making it convenient to work with files, IO, and threading.

# Package kotlin

Core functions and types, available on all supported platforms.

# Package kotlin.annotation

Library support for the Kotlin annotation facility.

# Package kotlin.browser

Access to top-level properties (`document`, `window` etc.) in the browser environment.

# Package kotlin.collections

Collection types, such as [Iterable][kotlin.collections.Iterable], [Collection][kotlin.collections.Collection],
[List][kotlin.collections.List], [Set][kotlin.collections.Set], [Map][kotlin.collections.Map] and related top-level and extension functions.

# Package kotlin.comparisons

Helper functions for creating [Comparator][kotlin.Comparator] instances.

# Package kotlin.concurrent

Low-level building blocks and utility functions for concurrent programming.

# Package kotlin.contracts

Experimental DSL for declaring custom function contracts.

# Package kotlin.coroutines

Basic primitives for creating and suspending coroutines: [Continuation][kotlin.coroutines.Continuation], 
[CoroutineContext][kotlin.coroutines.CoroutineContext] interfaces, coroutine creation and suspension top-level functions.

# Package kotlin.coroutines.intrinsics

Low-level building blocks for libraries that provide coroutine-based APIs.

# Package kotlin.coroutines.cancellation

Provides exceptions arising on suspended coroutine cancellation. 

# Package kotlin.dom

Utility functions for working with the browser DOM.

# Package kotlin.enums

Utilities for working with Kotlin enum classes.

# Package kotlin.experimental

Experimental APIs, subject to change in future versions of Kotlin.

# Package kotlin.io

IO API for working with files and streams.

# Package kotlin.io.path

Convenient extensions for working with file system using [java.nio.file.Path][java.nio.file.Path].

# Package kotlin.io.encoding

API for encoding and decoding data using various encoding schemes, such as [Base64][kotlin.io.encoding.Base64].

# Package kotlin.js

Functions and other APIs specific to the JavaScript platform.

# Package kotlin.js.collections

JavaScript-specific collection types, such as [JsArray][kotlin.js.collections.JsArray] and [JsReadonlyMap][kotlin.js.collections.JsReadonlyMap].

# Package kotlin.jvm

Functions and annotations specific to the Java platform.

# Package kotlin.jvm.optionals

Convenience extension functions for `java.util.Optional` to simplify Kotlin-Java interop.

# Package kotlin.math

Mathematical functions and constants. 

The functions include trigonometric, hyperbolic, exponentiation and power, logarithmic, rounding, sign and absolute value.

# Package kotlin.native

Provides functions and types specific to Kotlin/Native, as well as means for interoperability with an underlying platform
and other languages.

# Package kotlin.native.concurrent

Kotlin/Native-specific concurrency primitives and utility functions for concurrent programming.

# Package kotlin.native.ref

API for object life-cycle and references management.

# Package kotlin.native.runtime

Allows to query information from and interact with the Kotlin/Native runtime. 

# Package kotlin.properties

Standard implementations of delegates for [delegated properties](https://kotlinlang.org/docs/delegated-properties.html)
and helper functions for implementing custom delegates.

# Package kotlin.random

Provides the default generator of pseudo-random values, the repeatable generator, and a base class for other RNG implementations.

# Package kotlin.ranges

[Ranges](https://kotlinlang.org/docs/ranges.html), Progressions and related top-level and extension functions.

# Package kotlin.reflect

Runtime API for [Kotlin reflection](https://kotlinlang.org/docs/reflection.html)

# Package kotlin.reflect.full

Extensions for [Kotlin reflection](https://kotlinlang.org/docs/reflection.html) provided by `kotlin-reflect` library.

# Package kotlin.reflect.jvm

Runtime API for interoperability between [Kotlin reflection](https://kotlinlang.org/docs/reflection.html) and
Java reflection provided by `kotlin-reflect` library.

# Package kotlin.sequences

[Sequence][kotlin.sequences.Sequence] type that represents lazily evaluated collections. Top-level functions for instantiating sequences
and extension functions for sequences.

## Classification of sequences

The sequence operations can be classified into the following groups regarding their state requirements:

 - _stateless_ – operations which require no state and process each element independently like [kotlin.sequences.Sequence.map], [kotlin.sequences.Sequence.filter],
   or require a small constant amount of state to process an element, for example [kotlin.sequences.Sequence.take] or [kotlin.sequences.Sequence.drop];
 - _stateful_ – operations which require a significant amount of state, usually proportional to the number of elements in a sequence.
 
If the sequence operation returns another sequence, which is produced lazily, it's called _intermediate_, and otherwise the operation is _terminal_.
Examples of terminal operations are [kotlin.sequences.Sequence.toList], [kotlin.sequences.Sequence.max].

Sequences can be iterated multiple times, however some sequence implementations might constrain themselves
to be iterated only once. That is mentioned specifically in their documentation (e.g. [kotlin.sequences.generateSequence] overload).
The latter sequences throw an exception on an attempt to iterate them the second time.
 
# Package kotlin.streams

Utility functions for working with Java 8 [streams](https://docs.oracle.com/javase/8/docs/api/java/util/stream/package-summary.html).

# Package kotlin.system

System-related utility functions.

# Package kotlin.test

Functions for writing test assertions.

# Package kotlin.text

Functions for working with text and regular expressions.

# Package kotlin.time

API for measuring time intervals and calculating durations.

For more information, see our [Time measurement](https://kotlinlang.org/docs/time-measurement.html) guide.

# Package kotlin.uuid

Multiplatform `Uuid` class and utility functions for working with UUIDs.

# Package kotlinx.cinterop

Experimental API for working with unmanaged and foreign memory, as well as for interoperability with other languages,
such as Objective-C and C.

# Package org.khronos.webgl

Kotlin JavaScript wrappers for the WebGL API.

# Package org.w3c.dom

Kotlin JavaScript wrappers for the DOM API.

# Package org.w3c.dom.css

Kotlin JavaScript wrappers for the DOM CSS API.

# Package org.w3c.dom.events

Kotlin JavaScript wrappers for the DOM events API.

# Package org.w3c.dom.parsing

Kotlin JavaScript wrappers for the DOM parsing API.

# Package org.w3c.dom.svg

Kotlin JavaScript wrappers for the DOM SVG API.

# Package org.w3c.dom.url

Kotlin JavaScript wrappers for the DOM URL API.

# Package org.w3c.fetch

Kotlin JavaScript wrappers for the [W3C fetch API](https://fetch.spec.whatwg.org).

# Package org.w3c.files

Kotlin JavaScript wrappers for the [W3C file API](https://www.w3.org/TR/FileAPI/).

# Package org.w3c.notifications

Kotlin JavaScript wrappers for the [Web Notifications API](https://www.w3.org/TR/notifications/).

# Package org.w3c.performance

Kotlin JavaScript wrappers for the [Navigation Timing API](https://www.w3.org/TR/navigation-timing/).

# Package org.w3c.workers

Kotlin JavaScript wrappers for the [Web Workers API](https://www.w3.org/TR/workers/).

# Package org.w3c.xhr

Kotlin JavaScript wrappers for the [XMLHttpRequest API](https://www.w3.org/TR/XMLHttpRequest/).

