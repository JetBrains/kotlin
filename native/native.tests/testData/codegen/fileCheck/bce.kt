// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-LABEL: define void @"kfun:#forEachIndicies(){}"()
fun forEachIndicies() {
    val array = Array(10) { 0 }

    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in array.indices) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        array[i] = 6
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#forUntilSize(){}"()
fun forUntilSize() {
    val array = Array(10) { 0L }
    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in 0 until array.size) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        array[i] = 6
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#forRangeUntilSize(){}"()
@ExperimentalStdlibApi
fun forRangeUntilSize() {
    val array = Array(10) { 0L }
    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in 0..<array.size) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        array[i] = 6
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#forDownToSize(){}"()
fun forDownToSize() {
    val array = Array(10) { 0L }

    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in array.size - 1 downTo 0) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        array[i] = 6
    }

    // CHECK: {{^}}do_while_loop{{.*}}:
    for (j in array.size - 3 downTo 0) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        array[j] = 6
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#forRangeToSize(){}"()
fun forRangeToSize() {
    val array = Array(10) { 0L }

    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in 0..array.size - 1) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        array[i] = 6
    }

    val length = array.size - 1

    // CHECK: {{^}}do_while_loop{{.*}}:
    for (j in 0..length) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        array[j] = 6
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#forRangeToWithStep(){}"()
fun forRangeToWithStep() {
    val array = Array(10) { 0L }

    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in 0..array.size - 1 step 2) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        array[i] = 6
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#forUntilWithStep(){}"()
fun forUntilWithStep() {
    val array = CharArray(10) { '0' }
    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in 0 until array.size step 2) {
        // CHECK: {{call|invoke}} void @Kotlin_CharArray_set_without_BoundCheck
        array[i] = '6'
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#forRangeUntilWithStep(){}"()
@ExperimentalStdlibApi
fun forRangeUntilWithStep() {
    val array = CharArray(10) { '0' }
    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in 0..<array.size step 2) {
        // CHECK: {{call|invoke}} void @Kotlin_CharArray_set_without_BoundCheck
        array[i] = '6'
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#forDownToWithStep(){}"()
fun forDownToWithStep() {
    val array = UIntArray(10) { 0U }
    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in array.size - 1 downTo 0 step 2) {
        // CHECK: {{call|invoke}} void @Kotlin_IntArray_set_without_BoundCheck
        array[i] = 6U
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#forIndiciesWithStep(){}"()
fun forIndiciesWithStep() {
    val array = Array(10) { 0L }
    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in array.indices step 2) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        array[i] = 6
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#forWithIndex(){}"()
fun forWithIndex() {
    val array = Array(10) { 100 }

    // CHECK: {{^}}while_loop{{.*}}:
    for ((index, value) in array.withIndex()) {
        // CHECK: {{call|invoke}} %struct.ObjHeader* @Kotlin_Array_get_without_BoundCheck
        array[index] = 6
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#forReversed(){}"()
fun forReversed() {
    val array = Array(10) { 100 }
    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in (0..array.size-1).reversed()) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        array[i] = 6
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#forRangeUntilReversed(){}"()
@ExperimentalStdlibApi
fun forRangeUntilReversed() {
    val array = Array(10) { 100 }
    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in (0..<array.size).reversed()) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        array[i] = 6
    }
}
// CHECK-LABEL: {{^}}epilogue:

fun foo(a: Int, b : Int): Int = a + b * 2

// CHECK-LABEL: define void @"kfun:#forEachCall(){}"()
fun forEachCall() {
    val array = Array(10) { 100 }
    var sum = 0
    // CHECK: {{^}}while_loop{{.*}}:
    array.forEach {
        // CHECK: {{call|invoke}} %struct.ObjHeader* @Kotlin_Array_get_without_BoundCheck
        sum += it
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#forLoop(){}"()
fun forLoop() {
    val array = Array(10) { 100 }
    var sum = 0
    // CHECK: {{^}}while_loop{{.*}}:
    for (it in array) {
        // CHECK: {{call|invoke}} %struct.ObjHeader* @Kotlin_Array_get_without_BoundCheck
        sum += it
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#innerLoop(){}"()
fun innerLoop() {
    val array = Array(10) { 100 }
    val array1 = Array(3) { 0 }

    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in 0 until array.size) {
        // CHECK-DAG: {{call|invoke}} %struct.ObjHeader* @Kotlin_Array_get_without_BoundCheck
        array[i] = 7
        // CHECK-DAG: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        // CHECK-DAG: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        for (j in 0 until array1.size) {
            array1[j] = array[i]
        }
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#argsInFunctionCall(){}"()
fun argsInFunctionCall() {
    val array = Array(10) { 100 }

    val size = array.size - 1
    val size1 = size

    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in 0..size1) {
        // CHECK: {{call|invoke}} %struct.ObjHeader* @Kotlin_Array_get_without_BoundCheck
        // CHECK: {{call|invoke}} %struct.ObjHeader* @Kotlin_Array_get_without_BoundCheck
        // CHECK: {{call|invoke}} i32 @"kfun:#foo(kotlin.Int;kotlin.Int){}kotlin.Int"
        foo(array[i], array[i])
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#smallLoop(){}"()
fun smallLoop() {
    val array = Array(10) { 100 }

    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in 0..array.size - 2) {
        // CHECK: {{call|invoke}} %struct.ObjHeader* @Kotlin_Array_get_without_BoundCheck
        array[i+1] = array[i]
    }
}
// CHECK-LABEL: {{^}}epilogue:

object TopLevelObject {
    val array = Array(10) { 100 }
}

// CHECK-LABEL: define void @"kfun:#topLevelObject(){}"()
fun topLevelObject() {
    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in 0 until TopLevelObject.array.size) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        TopLevelObject.array[i] = 6
    }
}
// CHECK-LABEL: {{^}}epilogue:

val array = Array(10) { 100 }

// CHECK-LABEL: define void @"kfun:#topLevelProperty(){}"()
fun topLevelProperty() {
    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in 0..array.size - 2) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        array[i] = 6
    }
}
// CHECK-LABEL: {{^}}epilogue:

open class Base() {
    open val array = Array(10) { 100 }
}

class Child() : Base()

// CHECK-LABEL: define void @"kfun:#childClassWithFakeOverride(){}"()
fun childClassWithFakeOverride() {
    val child = Child()
    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in 0..child.array.size - 1) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        child.array[i] = 6
    }
}
// CHECK-LABEL: {{^}}epilogue:

class First {
    val child = Child()
}

class Second{
    val first = First()
}

class Third {
    val second = Second()
}

// CHECK-LABEL: define void @"kfun:#chainedReceivers(){}"()
fun chainedReceivers() {
    val obj = Third()
    val obj1 = obj
    val obj2 = obj1

    // CHECK: {{^}}do_while_loop{{.*}}:
    for (i in 0 until obj1.second.first.child.array.size) {
        // CHECK: {{call|invoke}} void @Kotlin_Array_set_without_BoundCheck
        obj2.second.first.child.array[i] = 6
    }
}
// CHECK-LABEL: {{^}}epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
@ExperimentalStdlibApi
fun box(): String {
    forEachIndicies()
    forUntilSize()
    forRangeUntilSize()
    forDownToSize()
    forRangeToSize()
    forRangeToWithStep()
    forUntilWithStep()
    forRangeUntilWithStep()
    forDownToWithStep()
    forIndiciesWithStep()
    forWithIndex()
    forReversed()
    forRangeUntilReversed()
    forEachCall()
    forLoop()
    innerLoop()
    argsInFunctionCall()
    smallLoop()
    topLevelObject()
    topLevelProperty()
    childClassWithFakeOverride()
    chainedReceivers()

    return "OK"
}