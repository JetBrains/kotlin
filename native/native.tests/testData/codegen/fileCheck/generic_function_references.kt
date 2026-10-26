// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

import kotlin.reflect.KFunction2

fun <StringifyTP> stringify(collection: StringifyTP, size: (StringifyTP) -> Int, get: StringifyTP.(Int) -> Any?): String {
    var res = "["
    for (i in 0 until size(collection)) {
        if (i > 0) res += ", "
        res += collection.get(i).toString()
    }
    res += "]"
    return res
}

interface I

// CHECK-LABEL: define ptr{{( addrspace\(1\))?}} @"kfun:#stringifyArray(kotlin.Array<0:0>){0\C2\A7<I>}kotlin.String"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}})
fun <StringifyArrayTP : I> stringifyArray(array: Array<StringifyArrayTP>) =
        // CHECK: call ptr{{( addrspace\(1\))?}} @"kfun:#stringify(0:0;kotlin.Function1<0:0,kotlin.Int>;kotlin.Function2<0:0,kotlin.Int,kotlin.Any?>){0\C2\A7<kotlin.Any?>}kotlin.String"
        stringify(
                array,
                { it.size }, // stringifyArray$1
                Array<*>::get // stringifyArray$$FUNCTION_REFERENCE_FOR$get$0
        )

// CHECK-LABEL: define ptr{{( addrspace\(1\))?}} @"kfun:#stringifyIntArray(kotlin.Array<kotlin.Int>){}kotlin.String"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}})
fun stringifyIntArray(array: Array<Int>) =
        // CHECK: call ptr{{( addrspace\(1\))?}} @"kfun:#stringify(0:0;kotlin.Function1<0:0,kotlin.Int>;kotlin.Function2<0:0,kotlin.Int,kotlin.Any?>){0\C2\A7<kotlin.Any?>}kotlin.String"
        stringify(
                array,
                { it.size }, // stringifyIntArray$1
                Array<Int>::get // stringifyIntArray$$FUNCTION_REFERENCE_FOR$get$1
        )

class N(val v: Int) : I {
    override fun toString() = v.toString()
}

@Suppress("UNUSED_PARAMETER")
fun <BazTP0, BazTP1> foo(p1: BazTP0, p2: BazTP1) {}

fun <QuxTP> bar() {
    val ref: KFunction2<QuxTP, QuxTP, Unit> = ::foo // bar$ref$$FUNCTION_REFERENCE_FOR$foo$2
    println(ref)
}

// CHECK-LABEL: define ptr{{( addrspace\(1\))?}} @"kfun:#box(){}kotlin.String"
fun box(): String {
    println(stringifyArray(arrayOf(N(2), N(14))))
    println(stringifyIntArray(arrayOf(1, 2, 3)))

    bar<Int>()
    bar<String>()

    val ref: KFunction2<Int, Int, Unit> = ::foo // box$ref$$FUNCTION_REFERENCE_FOR$foo$3
    println(ref)
    return "OK"
}

// CHECK-LABEL: define internal void @"kfun:stringifyArray$1.<init>#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} {{%[0-9]+}})

// CHECK-LABEL: define internal i32 @"kfun:stringifyArray$1.invoke#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}})


// CHECK-OPT: define internal ptr{{( addrspace\(1\))?}} @"kfun:stringifyArray$1.$<bridge-UNN>invoke(kotlin.Array<1:0>){}kotlin.Int#internal"
// CHECK-DEBUG: define internal ptr{{( addrspace\(1\))?}} @"kfun:stringifyArray$1.$<bridge-UNC>invoke(kotlin.Array<1:0>){}kotlin.Int#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} [[this:%[0-9]+]], ptr{{( addrspace\(1\))?}} [[array:%[0-9]+]], ptr{{( addrspace\(1\))?}} {{%[0-9]+}})
// CHECK-OPT: call i32 @"kfun:stringifyArray$1.invoke#internal"(ptr{{( addrspace\(1\))?}} [[this]], ptr{{( addrspace\(1\))?}} {{%[0-9]+}})
// CHECK-DEBUG: call i32 @"kfun:stringifyArray$1.invoke#internal"(ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:stringifyArray$$FUNCTION_REFERENCE_FOR$get$0.<init>#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} {{%[0-9]+}})

// CHECK-LABEL: define internal ptr{{( addrspace\(1\))?}} @"kfun:stringifyArray$$FUNCTION_REFERENCE_FOR$get$0.invoke#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} [[this:%[0-9]+]], ptr{{( addrspace\(1\))?}} [[array:%[0-9]+]], i32 [[index:%[0-9]+]], ptr{{( addrspace\(1\))?}} [[ret:%[0-9]+]])
// The shadow stack scheme forwards the result slot of a call. The delta-main scheme has no result slot: callers pass null.
// CHECK-SHADOW_STACK-OPT: call ptr{{( addrspace\(1\))?}} @Kotlin_Array_get(ptr{{( addrspace\(1\))?}} [[array]], i32 [[index]], ptr{{( addrspace\(1\))?}} [[ret]])
// CHECK-DELTA_MAIN-OPT: call ptr{{( addrspace\(1\))?}} @Kotlin_Array_get(ptr{{( addrspace\(1\))?}} [[array]], i32 [[index]], ptr null)
// CHECK-SHADOW_STACK-DEBUG: call ptr{{( addrspace\(1\))?}} @Kotlin_Array_get(ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, i32 {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}})
// CHECK-DELTA_MAIN-DEBUG: call ptr{{( addrspace\(1\))?}} @Kotlin_Array_get(ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, i32 {{%[0-9]+}}, ptr null)

// CHECK-OPT: define internal ptr{{( addrspace\(1\))?}} @"kfun:stringifyArray$$FUNCTION_REFERENCE_FOR$get$0.$<bridge-NNNU>invoke(kotlin.Array<*>;kotlin.Int){}kotlin.Any?#internal"
// CHECK-DEBUG: define internal ptr{{( addrspace\(1\))?}} @"kfun:stringifyArray$$FUNCTION_REFERENCE_FOR$get$0.$<bridge-NNCU>invoke(kotlin.Array<*>;kotlin.Int){}kotlin.Any?#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} [[this:%[0-9]+]], ptr{{( addrspace\(1\))?}} [[array:%[0-9]+]], ptr{{( addrspace\(1\))?}} [[boxedIndex:%[0-9]+]], ptr{{( addrspace\(1\))?}} [[ret:%[0-9]+]])
// CHECK-SHADOW_STACK-OPT: call ptr{{( addrspace\(1\))?}} @"kfun:stringifyArray$$FUNCTION_REFERENCE_FOR$get$0.invoke#internal"(ptr{{( addrspace\(1\))?}} [[this]], ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, i32 {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} [[ret]])
// CHECK-DELTA_MAIN-OPT: call ptr{{( addrspace\(1\))?}} @"kfun:stringifyArray$$FUNCTION_REFERENCE_FOR$get$0.invoke#internal"(ptr{{( addrspace\(1\))?}} [[this]], ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, i32 {{%[0-9]+}}, ptr null)
// CHECK-SHADOW_STACK-DEBUG: call ptr{{( addrspace\(1\))?}} @"kfun:stringifyArray$$FUNCTION_REFERENCE_FOR$get$0.invoke#internal"(ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, i32 {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}})
// CHECK-DELTA_MAIN-DEBUG: call ptr{{( addrspace\(1\))?}} @"kfun:stringifyArray$$FUNCTION_REFERENCE_FOR$get$0.invoke#internal"(ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, i32 {{%[0-9]+}}, ptr null)

// CHECK-LABEL: define internal void @"kfun:stringifyIntArray$1.<init>#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} {{%[0-9]+}})

// CHECK-LABEL: define internal i32 @"kfun:stringifyIntArray$1.invoke#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}})

// CHECK-OPT: define internal ptr{{( addrspace\(1\))?}} @"kfun:stringifyIntArray$1.$<bridge-UNN>invoke(kotlin.Array<kotlin.Int>){}kotlin.Int#internal"
// CHECK-DEBUG: define internal ptr{{( addrspace\(1\))?}} @"kfun:stringifyIntArray$1.$<bridge-UNC>invoke(kotlin.Array<kotlin.Int>){}kotlin.Int#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} [[this:%[0-9]+]], ptr{{( addrspace\(1\))?}} [[array:%[0-9]+]], ptr{{( addrspace\(1\))?}} {{%[0-9]+}})
// CHECK-OPT: call i32 @"kfun:stringifyIntArray$1.invoke#internal"(ptr{{( addrspace\(1\))?}} [[this]], ptr{{( addrspace\(1\))?}} {{%[0-9]+}})
// CHECK-DEBUG: call i32 @"kfun:stringifyIntArray$1.invoke#internal"(ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:stringifyIntArray$$FUNCTION_REFERENCE_FOR$get$1.<init>#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} {{%[0-9]+}})

// CHECK-LABEL: define internal i32 @"kfun:stringifyIntArray$$FUNCTION_REFERENCE_FOR$get$1.invoke#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, i32 {{%[0-9]+}})


// CHECK-OPT: define internal ptr{{( addrspace\(1\))?}} @"kfun:stringifyIntArray$$FUNCTION_REFERENCE_FOR$get$1.$<bridge-UNNU>invoke(kotlin.Array<kotlin.Int>;kotlin.Int){}kotlin.Int#internal"
// CHECK-DEBUG: define internal ptr{{( addrspace\(1\))?}} @"kfun:stringifyIntArray$$FUNCTION_REFERENCE_FOR$get$1.$<bridge-UNCU>invoke(kotlin.Array<kotlin.Int>;kotlin.Int){}kotlin.Int#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} [[this:%[0-9]+]], ptr{{( addrspace\(1\))?}} [[array:%[0-9]+]], ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}})
// CHECK-OPT: call i32 @"kfun:stringifyIntArray$$FUNCTION_REFERENCE_FOR$get$1.invoke#internal"(ptr{{( addrspace\(1\))?}} [[this]], ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, i32 {{%[0-9]+}})
// CHECK-DEBUG: call i32 @"kfun:stringifyIntArray$$FUNCTION_REFERENCE_FOR$get$1.invoke#internal"(ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, i32 {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:bar$ref$$FUNCTION_REFERENCE_FOR$foo$2.<init>#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:bar$ref$$FUNCTION_REFERENCE_FOR$foo$2.invoke#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} [[p1:%[0-9]+]], ptr{{( addrspace\(1\))?}} [[p2:%[0-9]+]])
// CHECK-OPT: call void @"kfun:#foo(0:0;0:1){0\C2\A7<kotlin.Any?>;1\C2\A7<kotlin.Any?>}"(ptr{{( addrspace\(1\))?}} [[p1]], ptr{{( addrspace\(1\))?}} [[p2]])
// CHECK-DEBUG: call void @"kfun:#foo(0:0;0:1){0\C2\A7<kotlin.Any?>;1\C2\A7<kotlin.Any?>}"(ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}})


// CHECK-LABEL: define internal ptr{{( addrspace\(1\))?}} @"kfun:bar$ref$$FUNCTION_REFERENCE_FOR$foo$2.$<bridge-DNNN>invoke(1:0;1:0){}#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} [[this:%[0-9]+]], ptr{{( addrspace\(1\))?}} [[p1:%[0-9]+]], ptr{{( addrspace\(1\))?}} [[p2:%[0-9]+]], ptr{{( addrspace\(1\))?}} {{%[0-9]+}})
// CHECK-OPT: call void @"kfun:bar$ref$$FUNCTION_REFERENCE_FOR$foo$2.invoke#internal"(ptr{{( addrspace\(1\))?}} [[this]], ptr{{( addrspace\(1\))?}} [[p1]], ptr{{( addrspace\(1\))?}} [[p2]])
// CHECK-DEBUG: call void @"kfun:bar$ref$$FUNCTION_REFERENCE_FOR$foo$2.invoke#internal"(ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:box$ref$$FUNCTION_REFERENCE_FOR$foo$3.<init>#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} {{%[0-9]+}})

// CHECK-LABEL: define internal void @"kfun:box$ref$$FUNCTION_REFERENCE_FOR$foo$3.invoke#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, i32 {{%[0-9]+}}, i32 {{%[0-9]+}})
// CHECK: call void @"kfun:#foo(0:0;0:1){0\C2\A7<kotlin.Any?>;1\C2\A7<kotlin.Any?>}"(ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}})

// CHECK-LABEL: define internal ptr{{( addrspace\(1\))?}} @"kfun:box$ref$$FUNCTION_REFERENCE_FOR$foo$3.$<bridge-DNUU>invoke(kotlin.Int;kotlin.Int){}#internal"
// CHECK-SAME: (ptr{{( addrspace\(1\))?}} [[this:%[0-9]+]], ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, ptr{{( addrspace\(1\))?}} {{%[0-9]+}})
// CHECK-OPT: call void @"kfun:box$ref$$FUNCTION_REFERENCE_FOR$foo$3.invoke#internal"(ptr{{( addrspace\(1\))?}} [[this]], i32 {{%[0-9]+}}, i32 {{%[0-9]+}})
// CHECK-DEBUG: call void @"kfun:box$ref$$FUNCTION_REFERENCE_FOR$foo$3.invoke#internal"(ptr{{( addrspace\(1\))?}} {{%[0-9]+}}, i32 {{%[0-9]+}}, i32 {{%[0-9]+}})
