// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in foo, value-parameter b: kotlin.Int defined in foo
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int

inline fun foo(a: Int, b: Int, f: (Int) -> Int) = f(<selection>a + b</selection>)