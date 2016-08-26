package foo

fun box(): String {
    testTrue { "num" in bar }
    testTrue { "str" in bar }
    testTrue { "obj" in bar }
    testTrue { 0 in bar }
    testTrue { 5 in bar }
    testTrue { 1 in bar }
    testTrue { "name" in bar.obj }
    testTrue { "name" in baz }
    testTrue { "length" in baz }
    testTrue { "0" in arr }
    testTrue { 0 in arr }
    testTrue { 2 in arr }
    testTrue { 3 in arr }
    testTrue { "length" in arr }

    testFalse { "num" !in bar }
    testFalse { "str" !in bar }
    testFalse { "obj" !in bar }
    testFalse { 0 !in bar }
    testFalse { 5 !in bar }
    testFalse { 1 !in bar }
    testFalse { "name" !in bar.obj }
    testFalse { "name" !in baz }
    testFalse { "length" !in baz }
    testFalse { "0" !in arr }
    testFalse { 0 !in arr }
    testFalse { 2 !in arr }
    testFalse { 3 !in arr }
    testFalse { "length" !in arr }

    testFalse { "num0" in bar }
    testFalse { "STR" in bar }
    testFalse { 2 in bar }
    testFalse { "foo" in bar.obj }
    testFalse { 3 in baz }
    testFalse { "function" in baz }
    testFalse { "6" in arr }
    testFalse { 7 in arr }
    testFalse { 10 in arr }
    testFalse { "name" in arr }

    testTrue { "num0" !in bar }
    testTrue { "STR" !in bar }
    testTrue { 2 !in bar }
    testTrue { "foo" !in bar.obj }
    testTrue { 3 !in baz }
    testTrue { "function" !in baz }
    testTrue { "6" !in arr }
    testTrue { 7 !in arr }
    testTrue { 10 !in arr }
    testTrue { "name" !in arr }

    return "OK"
}