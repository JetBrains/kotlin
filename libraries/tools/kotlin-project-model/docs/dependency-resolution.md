# Dependency Resolution in the Project Model

Dependency resolution is not a single task, it is a complex pipeline where the requested dependencies, considered an input, are transformed
to mutliple possible kinds of results. To produce one kind of the results, another sort of dependency resolution result may  be needed. 
Therefore, one may think of the process of resolving dependencies as of a pipeline that may produce results of different shapes when 
requested. 

Some use cases only require a dependency resolution result of one kind (for example, 'get all other fragments that a given 
fragment can see'), and for them, it might be convenient to have a single facade returning just that kind of result.

This document describes all the granular tasks that one might put before dependency resolution implementations that work with the Kotlin
Project Model, without attaching them to specific use cases.

## Internal dependency expansion

> We may remove this section if it turns out that we are going to use modules for test-on-production dependencies and the like

We allow declaring only some of the fragment dependencies between fragments inside a single module, and the fragment dependencies for the 
depending fragment's refines-children are inferred automatically. Also, declaring such a dependency implies that all refines-parents of the
dependency fragment should be visible, too, as well as everything it sees via its own expanded module fragment dependencies (as well as 
transitive expansion results of the first-level expansion results, and so on).

The interface for this is `InternalDependencyExpansion` with a function that takes a `fragment` and returns all other fragments of the same
module that the fragment should see. For ease of explanation and diagnostics, the results are grouped by the actual declared fragment 
dependency that led to each particular fragment having been added to the result.

Example:

```
commonMain             commonTestFixtures < - - - - -commonTest
|                      |                             |
jvmAndJsMain < - - - - jvmAndJsTestFixtures          jvmAndJsTest
|                      |                             |
jvmMain                jvmTestFixtures               jvmTest
```

Here, dashed arrows denote declared fragment dependencies. When we ask for fragments that `jvmAndJsTest` sees, we get:
`commonTestFixtures` and `jvmAndJsTestFixtures` because `commonTest` depends on `commonTestFixtures, and then also `commonMain` and 
`jvmAndJsMain` because `jvmAndJsTestFixtures` depends on `jvmAndJsMain`.

Currently, the `DefaultInternalDependencyExpansion` implementation takes a function that matches variants of the module as if they take
part in variant-aware dependency resolution. This function may actually perform matching of the variants or may rely on additional 
information (until attributes matching is implemented, it may search for explicit dependencies added between the variants).

## Dependency discovery

Given that dependency resolution may bring transtive dependencies, it is not enough to know what *declared dependencies* a fragment has to
be able to properly inspect the resolution results. You With just those, you won't be able to ask 
'what fragments does this fragment sees from module `foo`?' if the module `foo` is only a transitive dependency. 

This defines a task of *discovering module dependencies*, that is, finding which modules a fragment actually depends on, including the 
modules brought in as transitive dependencies. 

With the granular modules of the Kotlin Project Model, this not a trivial task. A module may reference another module as a dependency onl in
some of its fragments. Therefore, to decide whether a particular module `m` should be transitively included in the resolution results for 
one of our fragments `f` we have to first find out whether `f` sees any other fragment that declares a dependency on `m`. This is another
dependency resolution task that is covered below.

## Module resolution

Given a module dependency, we should be able to build a Kotlin module for that dependency, as we will have to decompose the dependency on
the whole module to dependencies on its granular parts. These granular parts are the variants and fragments. So it is reasonable to build
a Kotlin module for a resolved dependency and reason about it in the same terms as we use for locally built modules.

This task in dependency resolution takes a module dependency and returns a Kotlin module. This may in fact be a Kotlin module that is built 
locally (for dependencies that point to it directly).

## Variant resolution

This is a simple task that is very much like Gradle's variant aware dependency resolution: given a variant (not just a fragment) of a 
consuming module and another dependency module, we should tell which of the dependency module's variants is the best match. 

When we look at a consumer's variant, it is important to be able to pick a dependency's variant (and not as set of fragments), because 
variants produce final binaries, and for the consumer to produce a final binary from its variant, it needs a guarantee that the producer
also could generate a complete binary (with all `expect`s covered by `actual`s). The confirmation of this is exactly the producer's variant.

## Fragment resolution

This is the task of dependency resolution that we ultimately need to find out which declarations a module fragment sees. Namely, given a
module dependency, we have to determine which of the dependency's fragments the consumer's fragment may see.

To do that, we have to perform variant resolution for each of the variants that the depending variant participates in, and then intersect
the refines-closures of the resolved variants, this will be the safe set of fragments whose declarations we may use because they are 
available in any variant that we will compile the fragment for.