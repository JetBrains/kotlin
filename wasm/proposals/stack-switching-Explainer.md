# Stack-switching

This proposal adds typed stack-switching to WebAssembly, enabling a
single WebAssembly instance to manage multiple execution stacks
concurrently. The primary use-case for stack-switching is to add
direct support for modular compilation of advanced non-local control
flow idioms, e.g. coroutines, async/await, generators, lightweight
threads, and so forth. This document outlines the new instructions and
validation rules to facilitate stack-switching.

## Table of contents
1. [Motivation](#motivation)
1. [Continuations](#continuations)
1. [Introduction to continuation-based stack-switching](#introduction-to-continuation-based-stack-switching)
   1. [Generators](#generators)
   1. [Task scheduling](#task-scheduling)
1. [Instruction set extension](#instruction-set-extension)
   1. [Declaring control tags](#declaring-control-tags)
   1. [Creating continuations](#creating-continuations)
   1. [Invoking continuations](#invoking-continuations)
   1. [Suspending continuations](#suspending-continuations)
   1. [Partial continuation application](#partial-continuation-application)
   1. [Continuation lifetime](#continuation-lifetime)
1. [Further examples](#further-examples)
   1. [Extending the generator](#extending-the-generator)
   1. [Canceling tasks](#canceling-tasks)
1. [Design considerations](#design-considerations)
   1. [Asymmetric switching](#asymmetric-switching)
   1. [Symmetric switching](#symmetric-switching)
   1. [Partial application](#partial-application)
   1. [One-shot continuations](#one-shot-continuations)
1. [Specification changes](#specification-changes)
   1. [Types](#types)
   1. [Tags](#tags)
   1. [Instructions](#instructions)
   1. [Execution](#execution)
   1. [Binary format](#binary-format)

## Motivation

Non-local control flow features provide the ability to suspend the
current execution context and later resume it. Many
industrial-strength programming languages feature a wealth of
non-local control flow features such as async/await, coroutines,
generators/iterators, effect handlers, and so forth. For some
programming languages non-local control flow is central to their
identity, meaning that they rely on non-local control flow for
efficiency, e.g. to support massively scalable concurrency.

Rather than build specific control flow mechanisms for all possible
varieties of non-local control flow, our strategy is to build a single
mechanism, *continuations*, that can be used by language providers to
construct their own language specific features.

## Continuations

A continuation represents a snapshot of execution on a particular
stack. Stack-switching is realised by instructions for suspending and
resuming continuations. Continuations are composable, meaning that
when a suspended continuation is resumed it is spliced onto the
current continuation. This splicing establishes a parent-child
relationship between the current and resumed continuation. In this
respect the design provides a form of *asymmetric switching*.

When suspending, we provide a tag and payload, much like when raising
an exception. Correspondingly, when resuming a suspended continuation
a *handler* is installed which specifies different behaviours for the
different kinds of tag with which the resumed continuation may
subsequently be suspended. Unlike for a normal exception handler the
handler is passed the suspended continuation as well as a payload.

We also offer an alternative to the interface based on suspending and
resuming continuations by way of an instruction for direct switching
between continuations. Direct switching combines suspending the
current continuation with resuming a previously suspended peer
continuation. Direct switching establishes a peer-to-peer relationship
between the current continuation and its peer. In this respect the
design provides a form of *symmetric switching*.

## Introduction to continuation-based stack-switching

We illustrate the proposed stack-switching mechanism using two
examples: generators and task scheduling. The generators example uses
asymmetric stack-switching. The task scheduling example has two
variants: the first variant uses asymmetric stack-switching and the
second variant uses symmetric stack-switching.

### Generators

Our first example illustrates a generator-consumer pattern. Execution
switches back and forth between a generator and a consumer execution
stack. Whenever execution switches from the generator to the consumer
the generator also passes a value to the consumer.

The stack-switching proposal reuses *tags* from the exception handling
proposal. The following tag is used to coordinate between the
generator and the consumer.

```wat
(tag $gen (param i32))
```

To switch execution to the consumer, the generator must suspend
execution. The suspend instruction takes a tag, here `$gen`. The tag
is used at runtime to determine how to continue execution, by
identifying the active *suspend handler* for that tag. In our example,
this handler will be provided by the consumer.

Much like with exceptions parameter types specify the types of values
passed from the suspend site to the corresponding handler. In our
example, the tag's single `i32` parameter value is the value created
by the generator that is passed to the consumer.

The proposal also extends tags with result types (not used in this
example). These allow the types of values passed from a resume site to
a previously suspended continuation when resuming it to be specified.

The overall module implementing our example has the following shape.

```wat
(module $generator
  (type $ft (func))
  ;; Type of continuations used by the generator:
  ;; No need for param or result types: No data passed back to the
  ;; generator when resuming it, and $generator function has no return
  ;; values.
  (type $ct (cont $ft))

  (func $print (import "spectest" "print_i32") (param i32))

  ;; Tag used to coordinate between generator and consumer: The i32 param
  ;; corresponds to the generated values passed to consumer; no values passed
  ;; back from generator to consumer.
  (tag $gen (param i32))

  ;; Simple generator yielding values from 100 down to 1
  (func $generator ...)
  (elem declare func $generator)

  (func $consumer ...)

)
```

The module defines *continuation type* `$ct` based on function type
`$ft`. Suspended continuations of type `(ref $ct)` take no arguments
and return no results. As we shall see, the generator and consumer
manipulate suspended continuations of type `(ref $ct)`.

The generator is defined as follows.

```wat
;; Simple generator yielding values from 100 down to 1.
(func $generator
  (local $i i32)
  (local.set $i (i32.const 100))
  (loop $loop
    ;; Suspend execution, pass current value of $i to consumer.
    (suspend $gen (local.get $i))
    ;; Decrement $i and exit loop once $i reaches 0.
    (local.tee $i (i32.sub (local.get $i) (i32.const 1)))
    (br_if $loop)
  )
)
```

It executes 100 iterations of a loop and returns afterwards. Execution
is suspended on each iteration using the `$gen` tag. The value passed
from the `suspend` instruction to the handler (i.e., the value
produced by the generator) is just the current value of the loop
counter.

The consumer is defined as follows.

 ```wat
(func $consumer
  (local $c (ref $ct))
  ;; Create continuation executing function $generator.
  ;; Execution only starts when resumed for the first time.
  (local.set $c (cont.new $ct (ref.func $generator)))

  (loop $loop
    (block $on_gen (result i32 (ref $ct))
      ;; Resume continuation $c.
      (resume $ct (on $gen $on_gen) (local.get $c))
      ;; $generator returned: no more data.
      (return)
    )
    ;; Generator suspended, stack now contains [i32 (ref $ct)]
    ;; Save continuation to resume it in the next iteration
    (local.set $c)
    ;; Stack now contains the i32 value produced by $generator
    (call $print)

    (br $loop)
  )
)
 ```

It uses `cont.new` to create a continuation executing the
generator. This instruction creates a value of reference type `(ref
$ct)`, saved in `$c`. It then runs a loop, where the `resume`
instruction is used to continue execution of the continuation
currently saved in `$c` on each iteration.

In general, a `resume` instruction may not only take a suspended
continuation as an argument, but also additional values to be passed
to the suspended continuation when it is resumed. These are specified
in the parameters of the continuation's type. In our example, `$ct`
has no parameters, indicating that no data is passed from the consumer
to the generator.

When a suspended continuation is resumed it is spliced onto the
current continuation (which may in fact be the top-level continuation
corresponding to the main stack). This splicing establishes a
parent-child relationship between the current and resumed
continuation. This asymmetric relationship affects execution in two
ways, which we now discuss.

First, in the `resume` instruction the *handler clause* `(on $gen
$on_gen)` installs a suspend handler for that tag while executing the
continuation. This means that if during execution of `$c`, the
continuation executes the instruction `suspend $gen`, execution
continues in the block `$on_gen`. In general, executing an instruction
`suspend $e` for some tag `$e` means that execution continues at the
*innermost* ancestor whose `resume` instruction installed a suspend
handler for `$e`. This behaviour is directly analogous to the search
for a matching exception handler after raising an exception. However,
it is more general in that the handler is also passed the suspended
current continuation. The extent of a suspended continuation captures
execution from the instruction immediately following `suspend $e` up
to the `resume` instruction that handles `$e`. In other words, as well
as resuming a suspended computation and installing a handler, the
`resume` instruction also acts as a *delimiter* for new suspended
continuations created by performing `suspend $e` in the scope of the
resumed continuation.

When the generator executes `suspend $gen`, execution continues in the
`$on_gen` block in `$consumer`. In that case, two values are pushed
onto the Wasm value stack. The topmost value is a new suspended
continuation. It is the continuation of executing the generator
following the `suspend` instruction (up to the handler). The other
value is the `i32` value passed from the generator to the consumer, as
required by the tag's definition. In our example, the consumer simply
prints the generated value and saves the new continuation in `$c` to
be resumed in the next iteration.

Second, the parent-child relationship determines where execution
continues after a continuation returns. Control simply transfers to
the next instruction after the `resume` instruction that resumed the
continuation in the parent, just as a normal function call returns to
the instruction after its call site. For instance, in our example once
the loop counter `$i` reaches 0, the `$generator` function returns and
we have reached the end of the continuation. Execution then continues
at the parent immediately after the `resume` instruction called by the
consumer, and the consumer also returns.

The full definition of this module can be found
[here](examples/generator.wast).

### Task scheduling

Our second example demonstrates how to implement task scheduling with
the stack-switching instructions. Specifically, suppose we want to
schedule a number of tasks, represented by functions `$task_0` to
`$task_n`, to be executed concurrently. Scheduling is cooperative,
meaning that tasks explicitly yield execution so that a scheduler may
pick the next task to run.

One approach is to use the asymmetric stack-switching approach we used
for the generator example. We define a function `$entry` that resumes
the initial task and installs a handler for a `$yield` tag inside an
event loop. In order to yield execution, tasks simply perform
`(suspend $yield)`, transferring control back to the parent
continuation, which is the event loop. The event loop then selects the
next task (if any) from a task queue and resumes it.

This approach is illustrated by the following skeleton code.

```wat
(module $scheduler1
  (type $ft (func))
  ;; Continuation type of all tasks
  (type $ct (cont $ft))

  ;; Tag used to yield execution in one task and resume another one.
  (tag $yield)

  ;; Used by scheduler to manage task continuations
  (table $task_queue 1000 (ref null $ct))

  ;; Entry point, becomes parent of all tasks.
  ;; Also acts as scheduler when tasks yield or finish.
  (func $entry (param $initial_task (ref $ft))
    ;; initialise $task_queue with $initial_task
    ...
    (loop $resume_next
      ;; pick $next_task from queue, or return if no more tasks.
      ...
      (block $on_yield (result (ref $ct))
        (resume $ct (on $yield $on_yield) (local.get $next_task))
        ;; task finished execution
        (br $resume_next)
      )
      ;; task suspended: put continuation in queue, then loop to determine next
      ;; one to resume.
      ...
    )
  )

  (func $task_0
    ...
    ;; To yield execution, simply suspend to scheduling logic in $entry.
    (suspend $yield)
    ...
  )

  ...

  (func $task_n ...)

)
```

Note that `$entry` performs all scheduling; it is responsible for
picking the next task to resume in two different circumstances: a) if
the most recent task suspended itself, and b) if it simply ran to
completion and returned. However, notice that this asymmetric approach
requires two stack switches in order to change execution from one task
to another: first when suspending from the a task to the event loop,
and second when the event loop resumes the next task.

Our proposal provides a mechanism to optimise the particular pattern
shown here, where suspending one continuation is followed by a handler
resuming another continuation `$c`. We can use the `switch`
instruction, which also relies on tags, to transfer control from the
original continuation directly to `$c`, thus avoiding the need for an
intermediate stack switch to the parent.

Concretely, executing `switch $ct $yield (local.get $c)` in our
example behaves equivalently to `(suspend $yield)`, assuming that the
active (ordinary) handler for `$yield` immediately resumes `$c` and
additionally passes the continuation obtained from handling `$yield`
along as an argument to `$c`. However, as mentioned above, using a
`switch` instruction here has the advantage that a Wasm engine can
implement it directly using only a single stack switch.
Each `switch` instruction is annotated with the type of the
continuation switched to.

The key idea is to inline scheduling logic in the tasks themselves in
order to reduce (or avoid altogether) the need to switch stacks to the
event loop in order to implement switching tasks.

This alternative approach is illustrated by the following skeleton
code.

```wat
(module $scheduler2
  (rec
    (type $ft (func (param (ref null $ct))))
    ;; Continuation type of all tasks
    (type $ct (cont $ft))
  )

  ;; Tag used to yield execution in one task and resume another one.
  (tag $yield)

  ;; Used by scheduler to manage task continuations
  (table $task_queue 1000 (ref null $ct))

  ;; Entry point, becomes parent of all tasks.
  ;; Only acts as scheduler when tasks finish.
  (func $entry (param $initial_task (ref $ft))
    ;; initialise $task_queue with $initial_task
    ...
    (loop $resume_next
      ;; pick $next_task from queue, or return if no more tasks.
      ;; Note that there is no suspend handler for $yield
      ...
      (resume $ct (on $yield switch) (ref.null $ct) (local.get $next_task))
      ;; task finished execution: loop to pick next one
      (br $resume_next)
      ...
    )
  )

  (func $task_0 (type $ft)
    ;; If $c is not null, put in task_queue.
    ...
    ;; To yield execution, call $yield_to_next
    (call $yield_to_next)
    ...
  )

  ...

  (func $task_n (type $ft) ...)

  ;; Determines next task to switch to directly.
  (func $yield_to_next
    ;; determine $next_task
    ...
    (block $done
      (br_if $done (ref.is_null (local.get $next_task)))
      ;; Switch to $next_task.
      ;; The switch instruction implicitly passes a reference to the currently
      ;; executing continuation as an argument to $next_task.
      (switch $ct $yield (local.get $next_task))
      ;; If we get here, some other continuation switch-ed directly to us, or
      ;; $entry resumed us.
      ;; In the first case, we receive the continuation that switched to us here
      ;; and we need to enqueue it in the task list.
      ;; In the second case, the passed continuation reference will be null.
      ...
    )
    ;; Just return if no other task in queue, making the $yield_to_next call
    ;; a noop.
  )

)
```

Here, the event loop is still responsible for resuming tasks from the
task queue, starting with some initial task. Thus, it will still be
the parent of all task continuations, as in the previous version.
However, it is no longer responsible for handling suspensions of
tasks. Instead, it only resumes the next task from the queue whenever
the previously running task returns. Yielding execution from one task
that merely wishes to suspend itself to another will be handled by the
tasks themselves, using the `switch` instruction.

The fact that the event loop does not handle suspensions is reflected
by the fact that its `resume` instruction does not install a suspend
handler for the `yield` tag. Instead, the resume instruction installs
a *switch handler* for tag `yield`. In order to yield execution a task
calls a separate function `$yield_to_next`. The scheduling logic picks
the next task `$next_task` and switches directly to it. Here, the
target continuation (i.e., `$next_task` in `$yield_to_next`) receives
the suspended current continuation (i.e., the one that just called
`$switch_to_next`) as an argument. The payload passing mechanism used
for integer values in the generator example is now used to pass
continuation references. The task that we switched to is now
responsible for enqueuing the previous continuation (i.e., the one
received as a payload) in the task list.

As a minor complication, we must encode the fact that the continuation
switched to receives the current continuation as an argument in the
type of the continuations handled by all scheduling logic. This means
that the type `$ct` must be recursive: a continuation of this type
takes a value of type `(ref null $ct)` as a parameter. In order to
give the same type to continuations that have yielded execution (those
created by `switch`) and those continuations that correspond to
beginning the execution of a `$task_i` function (those created by
`cont.new`), we add a `(ref null $ct)` parameter to all of the
`$task_i` functions. Finally, observe that the event loop passes a
null continuation to any continuation it resumes, indicating to the
resumed continuation that there is no previous continuation to enqueue
in the task list.

Note that installing a switch handler for `$yield` in `entry` is
strictly necessary. It acts as a delimiter, determining the extent of
the suspended continuation created when performing `switch` with tag
`$yield`. This form of stack-switching is symmetric in the following
sense. Rather than switching back to the parent (as `suspend` would),
`switch` effectively replaces the continuation under the handler for
`yield` in the event loop with a different continuation.

The proposal also allows passing additional payloads when performing a
`switch` instruction, besides the suspended current continuation. For
simplicity, our example does not make use of this feature, as we can
see from the type `$ct`, which has no further parameters besides the
continuation argument required by `switch`. However, this mechanism
could be used to optimise the implementation of task scheduling
further.

In `$scheduler2`, if a `$task_i` function finishes and therefore
returns, two stack switches are required to continue execution in the
next task in the queue. This is due to the fact that the returning
continuation switches to the parent (i.e., the event loop), which then
resumes the next task. To avoid this additional stack switch, we could
add boilerplate code to all of our task functions. Immediately before
a task function would ordinarily return, it should instead switch to
the next task. When doing so, it should pass a new flag to the target
continuation to indicate that the source continuation should not be
enqueued in the task list, but should instead be cancelled. Cancellation
can be implemented using another instruction, `resume_throw`, which is
described later in the document.

Full versions of `$scheduler1` and  `$scheduler2` can be found
[here](examples/scheduler1.wast) and [here](examples/scheduler2.wast).

## Instruction set extension

Here we give an informal account of the proposed instruction set
extension. In the [specification changes](#specification-changes) we
give a more formal account of the validation rules and changes to the
binary format.

For simplicity we ignore subtyping in this section, but in the
[specification changes](#specification-changes) we take full account
of subtyping.

The proposal adds a new reference type for continuations.

```wast
  (cont $ft)
```

A continuation type is specified in terms of a function type `$ft`,
whose parameter types `t1*` describe the expected stack shape prior to
resuming/starting the continuation, and whose return types `t2*`
describe the stack shape after the continuation has run to completion.

As a shorthand, we will often write the function type inline and write
a continuation type as

```wast
  (cont [t1*] -> [t2*])
```

### Declaring control tags

Control tags generalise exception tags to include result
types. Operationally, a control tag may be thought of as a *resumable*
exception. A tag declaration provides the type signature of a control
tag.

```wast
  (tag $t (param t1*) (result t2*))
```

The `$t` is the symbolic index of the control tag in the index space
of tags. The parameter types `t1*` describe the expected stack layout
prior to invoking the tag, and the result types `t2*` describe the
stack layout following an invocation of the operation.

We will often write `$t : [t1*] -> [t2*]` as shorthand for indicating
that such a declaration is in scope.

### Creating continuations

The following instruction creates a *suspended continuation* from a
function.

```wast
  cont.new $ct : [(ref $ft)] -> [(ref $ct)]
  where:
  - $ft = func [t1*] -> [t2*]
  - $ct = cont $ft
```

It takes a reference to a function of type `[t1*] -> [t2*]` whose body
may perform non-local control flow.

### Invoking continuations

There are three ways to invoke a suspended continuation.

The first way to invoke a continuation is to resume the suspended
continuation under a *handler*. The handler specifies what to do when
control is subsequently suspended again.

```wast
  resume $ct hdl* : [t1* (ref $ct)] -> [t2*]
  where:
  - $ct = cont [t1*] -> [t2*]
```

The `resume` instruction is parameterised by a continuation type and a
handler dispatch table `hdl`. The shape of `hdl` can be either:

1. `(on $e $l)` mapping the control tag `$e` to the label
`$l`. Intercepting `$e` causes a branch to `$l`.

2. `(on $e switch)` allowing a direct switch with control tag `$e`.

The `resume` instruction consumes its continuation argument, meaning
that a continuation may be resumed only once.

The second way to invoke a continuation is to raise an exception at
the control tag invocation site which causes the stack to be unwound.

```wast
  resume_throw $ct $exn hdl* : [te* (ref $ct)])] -> [t2*]
  where:
  - $ct = cont [t1*] -> [t2*]
  - $exn : [te*] -> []
```

The `resume_throw` instruction is parameterised by a continuation
type, the exception to be raised at the control tag invocation site,
and a handler dispatch table. As with `resume`, this instruction also
fully consumes its continuation argument. This instruction raises the
exception `$exn` with parameters of type `te*` at the control tag
invocation point in the context of the supplied continuation. As an
exception is being raised (the continuation is not actually being
supplied a value) the parameter types for the continuation `t1*` are
unconstrained.

The third way to invoke a continuation is to perform a symmetric
switch.

```wast
  switch $ct1 $e : [t1* (ref $ct1)] -> [t2*]
  where:
  - $e : [] -> [t*]
  - $ct1 = cont [t1* (ref $ct2)] -> [t*]
  - $ct2 = cont [t2*] -> [t*]
```

The `switch` instruction is parameterised by the type of the
continuation argument (`$ct1`) and a control tag
(`$e`). It suspends the current continuation (of type `$ct2`), then
performs a direct switch to the suspended peer continuation (of type
`$ct1`), passing in the required parameters (including the just
suspended current continuation, in order to allow the peer to switch
back again). As with `resume` and `resume_throw`, the `switch`
instruction fully consumes its suspended continuation argument.

### Suspending continuations

The current continuation can be suspended.

```wast
  suspend $e : [t1*] -> [t2*]
  where:
  - $e : [t1*] -> [t2*]
```

The `suspend` instruction invokes the control tag `$e` with arguments
of types `t1*`. It suspends the current continuation up to the nearest
enclosing handler for `$e`. This behaviour is similar to how raising
an exception transfers control to the nearest exception handler that
handles the exception. The key difference is that the continuation at
the suspension point expects to be resumed later with arguments of
types `t2*`.

### Partial continuation application

A suspended continuation can be partially applied to a prefix of its
arguments yielding another suspended continuation.

```wast
  cont.bind $ct1 $ct2 : [t1* (ref $ct1)] -> [(ref $ct2)]
  where:
  - $ct1 = cont [t1* t3*] -> [t2*]
  - $ct2 = cont [t3*] -> [t2*]
```

The `cont.bind` instruction binds a prefix of its arguments of type
`t1*` to a suspended continuation of type `$ct1`, yielding a modified
suspended continuation of type `$ct2`. The `cont.bind` instruction
also consumes its continuation argument, and yields a new continuation
that can be supplied to `resume`,`resume_throw`, `switch` or
`cont.bind`.

### Continuation lifetime

#### Producing continuations

There are four different ways in which continuations may be produced
(`cont.new,suspend,cont.bind,switch`). A fresh continuation object
is allocated with `cont.new` and the current continuation is reused
with `suspend`, `cont.bind`, and `switch`.

The `cont.bind` instruction is similar to the `func.bind` instruction
that was initially part of the function references proposal. However,
whereas the latter necessitates the allocation of a new closure, as
continuations are single-shot no allocation is necessary: all
allocation happens when the original continuation is created by
preallocating one slot for each continuation argument.

#### Consuming continuations

There are four different ways in which suspended continuations are
consumed (`resume,resume_throw,switch,cont.bind`). A suspended
continuation may be resumed with a particular handler with `resume`;
aborted with `resume_throw`; directly switched to via `switch`; or
partially applied with `cont.bind`.

In order to ensure that continuations are one-shot, `resume`,
`resume_throw`, `switch`, and `cont.bind` destructively modify the
suspended continuation such that any subsequent use of the same
suspended continuation will result in a trap.

## Further examples

We now illustrate the use of tags with result values and the
instructions `cont.bind` and `resume.throw`, by adapting and extending
the examples of [Section
3](#introduction-to-continuation-based-stack-switching).

### Extending the generator

The `$generator` function in [Section 3](#generators)
produces the values 100 down to 1. It uses the tag `$gen`, defined as
`(tag $gen (param i32))`, to send values to the `$producer` function.

We now adapt the producer to indicate to the generator when to reset
(i.e., start counting down from 100 again). The producer is adapted to
pass a boolean flag to the generator when resuming a continuation.
Correspondingly, the `$gen` tag is adapted to include an `i32` result
type for the flag:

```wat
(tag $gen (param i32) (result i32))
```

In the generator, the instruction `(suspend $gen)` now has type `[i32]
-> [i32]`: the parameter type represents the generated value (as in
the original version of the example) and the result type represents
the flag obtained back from the producer. We adapt the generator to
behave as follows, choosing between resetting or decrementing `$i`:

```wat
  (func $generator
    (local $i i32)
    (local.set $i (i32.const 100))
    (loop $loop
      ;; Suspend execution, pass current value of $i to consumer
      (suspend $gen (local.get $i))
      ;; We now have the flag on the stack given to us by the consumer, telling
      ;; us whether to reset the generator or not.
      (if (result i32)
        (then (i32.const 100))
        (else (i32.sub (local.get $i) (i32.const 1)))
      )
      (local.tee $i)
      (br_if $loop)
    )
  )
```

In the producer, we add logic to select the value of the flag, and
pass it to the generator continuation on `resume`. However, this poses
a challenge: the continuation created with `(cont.new $ct0 (ref.func
$generator)))` has the same type as before: a continuation type with
no parameter or return types. In contrast, the type of the
continuation received in a handler block for tag `$gen` expects an
`i32` due to the result type we added to `$gen`. This means that the
producer must now manipulate two different continuation types:

```wat
(type $ft0 (func))
(type $ft1 (func (param i32)))
;; Types of continuations used by the generator:
;; No param or result types for $ct0: $generator function has no
;; parameters or return values.
(type $ct0 (cont $ft0))
;; One param of type i32 for $ct1: An i32 is passed back to the
;; generator when resuming it, and $generator function has no return
;; values.
(type $ct1 (cont $ft1))
```

In order to avoid making the producer function unnecessarily
complicated, we ensure that there is a single local variable that
contains the next continuation to resume; its type will be `(ref
$ct0)`. We can then use `cont.bind` to turn the continuations received
in the handler block from type `(ref $ct1)` into `(ref $ct0)` by
binding the value of the flag to be passed.

The overall function is then defined as follows:

```wat
(func $consumer (export "consumer")
  ;; The continuation of the generator.
  (local $c0 (ref $ct0))
  ;; For temporarily storing the continuation received in handler.
  (local $c1 (ref $ct1))
  (local $i i32)
  ;; Create continuation executing function $generator.
  ;; Execution only starts when resumed for the first time.
  (local.set $c0 (cont.new $ct0 (ref.func $generator)))
  ;; Just counts how many values we have received so far.
  (local.set $i (i32.const 1))

  (loop $loop
    (block $on_gen (result i32 (ref $ct1))
      ;; Resume continuation $c0
      (resume $ct0 (on $gen $on_gen) (local.get $c0))
      ;; $generator returned: no more data
      (return)
    )
    ;; Generator suspended, stack now contains [i32 (ref $ct0)]
    ;; Save continuation to resume it in next iteration
    (local.set $c1)
    ;; Stack now contains the i32 value yielded by $generator
    (call $print)

    ;; Calculate flag to be passed back to generator:
    ;; Reset after the 42nd iteration
    (i32.eq (local.get $i) (i32.const 42))
    ;; Create continuation of type (ref $ct0) by binding flag value.
    (cont.bind $ct1 $ct0 (local.get $c1))
    (local.set $c0)

    (local.tee $i (i32.add (local.get $i) (i32.const 1)))
    (br_if $loop)
  )
)
```

Here, we set the flag for resetting the generator exactly once (after
it has returned 42 values).

The full version of the extended generator example can be found
[here](examples/generator-extended.wast).

### Canceling tasks

The task scheduling examples from [Section 3](#task-scheduling) yield
either by suspending to the scheduler running in the parent
(asymmetric variant) or by calling a scheduling function that uses
`switch` (symmetric variant).

Suppose we wish to adapt our schedulers to impose a limit on the
number of tasks that can exist at the same time. A simple way to
enforce the limit is to cancel the task at the head of the queue
whenever an attempt is made to add a continuation to a full queue. We
can implement this behaviour with a small modification to our previous
schedulers. Instead of adding tasks to be scheduled directly to a
queue, we call the following function.

```wat
(func $schedule_task (param $c (ref null $ct))
  ;; If the task queue is too long, cancel a task in the queue
  (if (i32.ge_s (call $task_queue-count) (global.get $concurrent_task_limit))
    (then
      (block $exc_handler
        (try_table (catch $abort $exc_handler)
          (resume_throw $ct $abort (call $task_dequeue))
        )
      )
    )
  )
  (call $task_enqueue (local.get $c))
)
```

The `$schedule_task` function checks if the current number of elements
in the queue has already reached the limit. If so, the function takes
an existing continuation from the queue and calls `resume_throw` on
it.

The `resume_throw` instruction is annotated with a newly defined tag,
`$abort`. This tag denotes an exception that will be raised at the
suspension point of the continuation. We then wrap the `resume_throw`
instruction in a `try_table`, which installs an exception handler for
`$abort`. This exception handler simply swallows the exception, which
means that the exception raised at the suspension point cannot escape
the `$schedule_task` function. The old continuation is deallocated and
the function proceeds to enqueue the new continuation.

The full version of the symmetric scheduling example using the
`$schedule_task` function can be found
[here](examples/scheduler2-throw.wast). The changes to the asymmetric
scheduling example are analogous.

## Design considerations

In this section we discuss some key design considerations.

### Asymmetric switching

Resuming a suspended continuation establishes a parent-child
relationship which aligns with the caller-callee relationship for
standard function calls meaning that no special plumbing is needed in
order to compose the non-local control features we define with
built-in non-local control features such as traps, exceptions, and
embedder integration.

### Symmetric switching

Direct switching to a suspended peer continuation is semantically
equivalent to suspending the current continuation with a special
switch tag whose payload is the suspended peer continuation in the
context of a handler which resumes the peer continuation. However,
direct switching can (and should) be optimised to avoid the need to
switch control to the handler before switching control to the peer.

### Partial application

Partial application can be important in practice due to the block and
type structure of Wasm, as in order to return a continuation from a
block all branches within the block must agree on the type of
continuation. Using `cont.bind`, a producer can ensure that the
branches within a block each produce a continuation with the same type.

### One-shot continuations

Continuations in the current proposal are single-shot (aka linear),
meaning that they should be invoked exactly once. A continuation can
be invoked either by resuming it (with `resume`); by aborting it (with
`resume_throw`); or by switching to it (with `switch`). An attempt to
invoke a continuation more than once results in a trap. Some
applications such as backtracking, probabilistic programming, and
process duplication exploit multi-shot continuations, but none of our
critical use-cases requires multi-shot continuations.

## Specification changes

This proposal is based on the [function references proposal](https://github.com/WebAssembly/function-references)
and [exception handling proposal](https://github.com/WebAssembly/exception-handling).

### Types

We extend the structure of composite types and heap types as follows.

- `cont <typeidx>` is a new form of composite type
  - `(cont $ft) ok` iff `$ft ok` and `$ft = [t1*] -> [t2*]`

We add two new continuation heap types and their subtyping hierarchy:
- `heaptypes ::= ... | cont | nocont`
- `nocont ok` and `cont ok` always
- `nocont` is the bottom type of continuation types, whereas `cont` is the top type, i.e. `nocont <: cont`

### Tags

We change the wellformedness condition for tag types to be more liberal, i.e.

- `(tag $t (type $ft)) ok` iff `$ft ok` and `$ft = [t1*] -> [t2*]`

In other words, the return type of tag types is allowed to be non-empty.

### Instructions

The new instructions and their validation rules are as follows. To simplify the presentation, we write this:

```
C.types[$ct] ~~ cont [t1*] -> [t2*]
```

where we really mean this:

```
C.types[$ct] ~~ cont $ft
C.types[$ft] ~~ func [t1*] -> [t2*]
```

This abbreviation will be formalised with an auxiliary function or other means in the spec.

- `cont.new <typeidx>`
  - Create a new continuation from a given typed funcref.
  - `cont.new $ct : [(ref null $ft)] -> [(ref $ct)]`
    - iff `C.types[$ct] ~~ cont [t1*] -> [t2*]`

- `cont.bind <typeidx> <typeidx>`
  - Partially apply a continuation.
  - `cont.bind $ct $ct' : [t3* (ref null $ct)] -> [(ref $ct')]`
    - iff `C.types[$ct] ~~ cont [t3* t1*] -> [t2*]`
    - and `C.types[$ct'] ~~ cont [t1'*] -> [t2'*]`
    - and `[t1*] -> [t2*] <: [t1'*] -> [t2'*]`

- `resume <typeidx> hdl*`
  - Execute a given continuation.
    - If the executed continuation suspends with a control tag `$t`, the corresponding handler `(on $t H)` is executed.
  - `resume $ct hdl* : [t1* (ref null $ct)] -> [t2*]`
    - iff `C.types[$ct] ~~ cont [t1*] -> [t2*]`
    - and `(hdl : t2*)*`

- `resume_throw <typeidx> <exnidx> hdl*`
  - Execute a given continuation, but force it to immediately throw the annotated exception.
  - Used to abort a continuation.
  - `resume_throw $ct $e hdl* : [te* (ref null $ct)] -> [t2*]`
    - iff `C.types[$ct] ~~ cont [t1*] -> [t2*]`
    - and `C.tags[$e] : tag $ft`
    - and `C.types[$ft] ~~ func [te*] -> []`
    - and `(hdl : t2*)*`

- `hdl = (on <tagidx> <labelidx>) | (on <tagidx> switch)`
  - Handlers attached to `resume` and `resume_throw`, handling control tags for `suspend` and `switch`, respectively.
  - `(on $e $l) : t*`
    - iff `C.tags[$e] = tag $ft`
    - and `C.types[$ft] ~~ func [t1*] -> [t2*]`
    - and `C.labels[$l] = [t1'* (ref null? $ct)]`
    - and `t1* <: t1'*`
    - and `C.types[$ct] ~~ cont [t2'*] -> [t'*]`
    - and `[t2*] -> [t*] <: [t2'*] -> [t'*]`
  - `(on $e switch) : t*`
    - iff `C.tags[$e] = tag $ft`
    - and `C.types[$ft] ~~ func [] -> [t*]`

- `suspend <tagidx>`
  - Use a control tag to suspend the current computation.
  - `suspend $t : [t1*] -> [t2*]`
    - iff `C.tags[$t] = tag $ft`
    - and `C.types[$ft] ~~ func [t1*] -> [t2*]`

- `switch <typeidx> <tagidx>`
  - Switch to executing a given continuation directly, suspending the current execution.
  - The suspension and switch are performed from the perspective of a parent `(on $e switch)` handler, determined by the annotated control tag.
  - `switch $ct1 $e : [t1* (ref null $ct1)] -> [t2*]`
    - iff `C.tags[$e] = tag $ft`
    - and `C.types[$ft] ~~ func [] -> [t*]`
    - and `C.types[$ct1] ~~ cont [t1* (ref null? $ct2)] -> [te1*]`
    - and `te1* <: t*`
    - and `C.types[$ct2] ~~ cont [t2*] -> [te2*]`
    - and `t* <: te2*`

### Execution

The same control tag may be used simultaneously by `throw`, `suspend`,
`switch`, and their associated handlers. When searching for a handler
for an event, only handlers for the matching kind of event are
considered, e.g. only `(on $e $l)` handlers can handle `suspend`
events and only `(on $e switch)` handlers can handle `switch`
events. The handler search continues past handlers for the wrong kind
of event, even if they use the correct tag.

### Binary format

We extend the binary format of composite types, heap types, and instructions.

#### Composite types

| Opcode | Type            | Parameters | Note |
| ------ | --------------- | ---------- |------|
| -0x20  | `func t1* t2*`  | `t1* : vec(valtype)` `t2* : vec(valtype)` | from Wasm 1.0 |
| -0x23  | `cont $ft`      | `$ft : s33` | new |

#### Heap Types

The opcode for heap types is encoded as an `s33`.

| Opcode | Type            | Parameters | Note |
| ------ | --------------- | ---------- | ---- |
| i >= 0 | i               |            | from function-references |
| -0x0b  | `nocont`        |            | new  |
| -0x18  | `cont`          |            | new  |

#### Instructions

We use the use the opcode space `0xe0-0xe5` for the seven new instructions.

| Opcode | Instruction              | Immediates |
| ------ | ------------------------ | ---------- |
| 0xe0   | `cont.new $ct`           | `$ct : u32` |
| 0xe1   | `cont.bind $ct $ct'`     | `$ct : u32`, `$ct' : u32` |
| 0xe2   | `suspend $t`             | `$t : u32` |
| 0xe3   | `resume $ct hdl*` | `$ct : u32` (for hdl see below) |
| 0xe4   | `resume_throw $ct $e hdl*` | `$ct : u32`, `$e : u32` (for hdl see below) |
| 0xe5   | `switch $ct1 $e`          | `$ct1 : u32`, `$e : u32` |

In the case of `resume` and `resume_throw` we use a leading byte to
indicate the shape of `hdl` as follows.

| Opcode | On clause shape | Immediates |
| ------ | --------------- | ---------- |
| 0x00   | `(on $t $h)`    | `$t : u32`, `$h : u32` |
| 0x01   | `(on $t switch)` | `$t : u32` |