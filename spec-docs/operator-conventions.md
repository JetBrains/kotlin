# Operator Conventions

Kotlin allows us to provide implementations for a predefined set of operators on our types. These operators have fixed symbolic representation
(like `+` or `*`) and fixed (see grammar). To implement an operator, we provide a member function
or an extension function with a fixed name, for the corresponding type, i.e. left-hand side type for binary operations and argument type for unary ones.

Here we describe the conventions that regulate operator overloading for different operators.

## Unary operations

| Expression | Translated to |
|------------|---------------|
| `+a` | `a.plus()` |
| `-a` | `a.minus()` |
| `!a` | `a.not()` |

This table says that when the compiler processes, for example, an expression `+a`, it performs the following steps:

* Determines the type of `a`, let it be `T`.
* Looks up a function `plus()` with no parameters for the receiver `T`, i.e. a member function or an extension function.
* If the function is absent or ambiguous, it is a compilation error.
* If the function is present and its return type is `R`, the expression `+a` has type `R`.

| Expression | Translated to |
|------------|---------------|
| `a++` | `a.inc()` + see below |
| `a--` | `a.dec()` + see below |

These operations are supposed to change their receiver and (optionally) return a value.

> **`inc()/dec()` shouldn't mutate the receiver object**.<br>
> By "changing the receiver" we mean _the receiver-variable_, not the receiver object.
{:.note}

The compiler performs the following steps for resolution of an operator in the *postfix* form, e.g. `a++`:

* Determines the type of `a`, let it be `T`.
* Looks up a function `inc()` with no parameters, applicable to the receiver of type `T`.
* If the function returns a type `R`, then it must be a subtype of `T`.

The effect of computing the expression is:

* Store the initial value of `a` to a temporary storage `a0`,
* Assign the result of `a.inc()` to `a`,
* Return `a0` as a result of the expression.

For `a--` the steps are completely analogous.

For the *prefix* forms `++a` and `--a` resolution works the same way, and the effect is:

* Assign the result of `a.inc()` to `a`,
* Return the new value of `a` as a result of the expression.

## Binary operations

| Expression | Translated to |
| -----------|-------------- |
| `a + b` | `a.plus(b)` |
| `a - b` | `a.minus(b)` |
| `a * b` | `a.times(b)` |
| `a / b` | `a.div(b)` |
| `a % b` | `a.mod(b)` |
| `a..b ` | `a.rangeTo(b)` |

For the operations in this table, the compiler just resolves the expression in the *Translated to* column.

| Expression | Translated to |
| -----------|-------------- |
| `a in b` | `b.contains(a)` |
| `a !in b` | `!b.contains(a)` |

For `in` and `!in` the procedure is the same, but the order of arguments is reversed.
{:#in}

{:#Equals}

| Expression | Translated to |
|------------|---------------|
| `a == b` | `a?.equals(b) ?: b === null` |
| `a != b` | `!(a?.equals(b) ?: b === null)` |

*Note*: `===` and `!==` (identity checks) are not overloadable, so no conventions exist for them

The `==` operation is special in two ways:

* It is translated to a complex expression that screens for `null`'s, and `null == null` is `true`.
* It looks up a function with a specific _signature_, not just a specific _name_. The function must be declared as

``` kotlin
fun equals(other: Any?): Boolean
```

Or an extension function with the same parameter list and return type.

| Symbol | Translated to |
|--------|---------------|
| `a > b`  | `a.compareTo(b) > 0` |
| `a < b`  | `a.compareTo(b) < 0` |
| `a >= b` | `a.compareTo(b) >= 0` |
| `a <= b` | `a.compareTo(b) <= 0` |

All comparisons are translated into calls to `compareTo`, that is required to return `Int`.

## Indexing and invocations

| Symbol | Translated to |
| -------|-------------- |
| `a[i]`  | `a.get(i)` |
| `a[i, j]`  | `a.get(i, j)` |
| `a[i_1, ...,  i_n]`  | `a.get(i_1, ...,  i_n)` |
| `a[i] = b` | `a.set(i, b)` |
| `a[i, j] = b` | `a.set(i, j, b)` |
| `a[i_1, ...,  i_n] = b` | `a.set(i_1, ..., i_n, b)` |

Square brackets are translated to calls to `get` and `set` with appropriate numbers of arguments.

| Symbol | Translated to |
|--------|---------------|
| `a(i)`  | `a.invoke(i)` |
| `a(i, j)`  | `a.invoke(i, j)` |
| `a(i_1, ...,  i_n)`  | `a.invoke(i_1, ...,  i_n)` |

Parentheses are translated to calls to invoke with appropriate number of arguments.

## Assignments

| Expression | Translated to |
|------------|---------------|
| `a += b` | `a.plusAssign(b)` |
| `a -= b` | `a.minusAssign(b)` |
| `a *= b` | `a.timesAssign(b)` |
| `a /= b` | `a.divAssign(b)` |
| `a %= b` | `a.modAssign(b)` |

For the assignment operations, e.g. `a += b`, the compiler performs the following steps:

* If the function from the right column is available
  * If the left-hand side can be assigned to and the corresponding binary function (i.e. `plus()` for `plusAssign()`) is available, report error (ambiguity).
  * Make sure its return type is `Unit`, and report an error otherwise.
  * Generate code for `a.plusAssign(b)`
* Otherwise, try to generate code for `a = a + b` (this includes a type check: the type of `a + b` must be a subtype of `a`).

*Note*: assignments are *NOT* expressions in Kotlin.

**Discussion of the ambiguity rule**:
We raise an error when both `plus()` and `plusAssign()` are available only if the lhs is assignable. Otherwise, the availability of `plus()`
is irrelevant, because we know that `a = a + b` can not compile. An important concern here is what happens when the lhs *becomes assignable*
after the fact (e.g. the user changes *val* to *var* or provides a `set()` function for indexing convention): in this case, the previously
correct call site may become incorrect, but not the other way around, which is safe, because former calls to `plusAssign()` can not be silently
turned into calls to `plus()`.