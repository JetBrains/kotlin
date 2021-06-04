# Applier target inferencing

Composition produces a tree of nodes and uses an applier to build and make changes to the tree
implied by composition. If a composable function directly or indirectly calls `ComposeNode` then it
is tied to an applier that can handle the type of node being emitted by the
`ComposeNode`. `ComposeNode` contains a runtime check to ensure that the applier is of the expected
type. However, currently, there is no warning or error generated when a composition function is
called that expects a different applier than is provided.

For Compose UI 1.0 this was not as important as there are only two appliers used by Compose UI, the
`UIApplier` and the `VectorApplier` and vector composable are rarely used so calling the incorrect
composable function is rare. However, as appliers proliferate, such as Wear projections,  or menu
trees for Compose Desktop, the likelihood of calling an incorrect composable grows larger and
detecting these statically is more important.

Requiring to specify the applier seems clumsy and inappropriate since, in 1.0, it wasn't necessary,
and especially since the inference rules are fairly simple. There is only one parameter that needs
to be determined, the type of the applier of the implied `$composer` parameter. From now on I will
be referring to this as the type of the applier when it technically is the type of the applier
instance used by the composer. In most cases the applier type is simply the applier type required
by composition functions it calls. For example, if a composable function calls `Text` it must be a
`UiApplier` composable because `Text` requires a `UIApplier`.

## Sketch of the algorithm

The following Prolog program demonstrates how type type of the applier can be inferred from the
content of the function (https://swish.swi-prolog.org/p/Composer%20Inference.swinb):

```
% An empty list can have any applier.
applier([], _).

% A list elements must have identical appliers.
applier([H|T], A) :- applier(H, A), applier(T, A).

% A layout has a uiApplier
applier(layout, uiApplier).

% A layout with content has a uiApplier and its content must have a uiApplier.
applier(layout(C), uiApplier) :- applier(C, uiApplier).

% A vector has a vector Applier.
applier(vector, vectorApplier).

% A vector with content has a vector applier and its content must have a vector applier
applier(vector(C), vectorApplier) :- applier(C, vectorApplier).
```


The above corresponds to calling `ComposeNode` (from Layout.kt and Vector.kt) and can easily be
derived from the body of the call. Taking advantage of of Prolog's unification algorithm, this can
also express open composition functions like the `CompositionLocalProvider`,

```
% provider provides information to its content for all appliers.
applier(provider(C), A) :- applier(C, A).
```

This predicate binds `A` to whatever applier the content `C` requires. In other words, `A` is an
open applier bound by the lambda passed into `provider`.

The above allows the validation that the composition function represented by, for example,

```
program(
           row([
               drawing([
                    provider([
                             circle,
                             square
                    ])
               ])
            ])
).
```


will not generate an applier runtime error (demonstrated in the link above).

## Declarations

Inferring the applier type is translated into inferring one of two attributes for every composable
function or composable lambda type, `ComposableTarget` and `ComposableOpenTarget`.

```
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class ComposableTarget(val applier: String)
```

```
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.TYPE,
    AnnotationTarget.TYPE_PARAMETER,
)
annotation class ComposableOpenTarget(val index: Int)
```

## Inferring appliers

Every composable function has an applier scheme determined by the composable functions it calls or
by explicit declarations. The scheme for a function is the applier token or open token variable for
each `@Composable` function type in the description including the description itself and
corresponds to the applier required by the respective composable function.

For the purposes of this document, the scheme is represented by a Prolog-like list with identifier
symbols being bound and [deBruijn](https://en.wikipedia.org/wiki/De_Bruijn_index) indices (written
as a backslash followed by the index such as `\1`) are unbound variables and unbound variables with
the same index bind together. A scheme is a list where the first element is the variable or token
for the `$composer` of the function followed by the schemes for each composable parameter (which
might also contain schemes for its parameters).

For example, a composable function declared as,

```
@Composable
@ComposableTarget("UI")
fun Text(value: String) { … }
```

has the scheme `[UI]`.

Open appliers, such as,

```
@Composable
@ComposableOpenTarget(0)
fun Providers(
  providers: vararg values: ProvidedValue<*>,
  content: @ComposableOpenTarget(0) @Composable () -> Unit
) {
  …
  content()
  …
}
```

Has the scheme `[\0, [\0]]` meaning the applier for `Providers` and the `content` lambda must be
the same but they can be any applier. Using these schemes an algorithm similar to the Prolog
algorithm can be implemented. That is, when type variables are bound, the variables are unified
using a normal unify algorithm. As backtracking is not needed so a simple mutating unify algorithm
can be used as once unification fails the failure is reported, the requested binding is ignored,
and the algorithm continues without backtracking.

When inferring an applier scheme,

1. The applier schemes are determined for each composable call directly in the body of the
   composable function.  If the function is inferred recursively while its scheme is being inferred
   then a scheme with all unique deBruijn indices matching the shape of the expected scheme is used
   instead.
2. A scheme is recursively inferred for all nested lambdas literals.
3. Applier variables are created for the function and each composable lambda parameter of the
   function.
4. For each call, fresh applier variables are created and are bound as defined by the scheme
   of the call; the symbols are bound and type variables with the same index in the scheme are
   bound together. Then,
    1. the first fresh variable is bound to the function's variable
    2. the composable lambda parameters are bound to the lambda literals or variable references
    3. if a lambda expression is passed as a parameter it is treated as if it was called at the
       point of the parameter expression evaluation using the scheme declared or inferred for the
       type of the formal lambda parameter. If the expression is a lambda literal the variables are
       bound to both the scheme inferred for the lambda literal and the scheme of the type.
5. The scheme is created by resolving each applier variable for the function. If it is bound to a
   token then the token is placed in the scheme. If it is bound to a still open variable then the
   variable group is given a number (if it doesn't have one already) and the corresponding deBruijn
   index is produced. A variable's group is the set of type variables that are bound together with
   the type variable. An open type parameter can only be a member of one group as, once it is bound
   to another variable, it either is bound to a token or the variable groups are merged together.

For each function or type with an inferred scheme the declaration is augmented with attributes
where `CompositionTarget` is produced for tokens and `CompositionOpenTarget` is produced with the
deBruijn index. This records the information necessary to infer appliers across module boundaries.

If any of the bindings fails (that is if it tries to unify to two different tokens) a diagnostic is
produced and the binding is ignored. Each variable can contain the location (e.g. PsiElement) that
it was created for. When the binding fails, the locations associated with each variable in the
binding group can be reported as being in error.

# Implementation Notes

## `ComposableInferredTarget`

Due to a limitation in how annotations are currentl handled in the Kotlin compiler, a plugin
cannot add annotation to types, specifically the lambda types in a composable function. To work
around this the plugin will infer a `ComposableInferredTarget` on the composable function which
contains the scheme, as described above, instead of adding annotations to the composable lambda
types in the function. For the target type checking described here `ComposableInferredTarget`
takes presedence over any other attriutes present on the function. The implementation dropped
the leading `\` from the deBruijn indexes, to save space, as they were unnecessary.

## Unification

Because backtracking is not required, the unify algorithm implemented takes advantage of this
by using a circular-linked-list to track the binding variables (two circular-linked lists can
be concatenated together in `O(1)` time by swapping next pointer of just one element in each).
The bindings can be reversed (e.g. swapping the next pointers back), but the code is not
included to do so, so if backtracking is later required, `Bindings` would need to be updated
accordingly. Details are provided in the class.