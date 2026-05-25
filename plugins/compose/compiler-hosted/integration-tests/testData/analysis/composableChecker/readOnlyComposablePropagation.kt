// RUN_PIPELINE_TILL: FRONTEND

import androidx.compose.runtime.*

@Composable @ReadOnlyComposable
fun readOnly(): Int = 10
val readonlyVal: Int
    @Composable @ReadOnlyComposable get() = 10

@Composable
fun normal(): Int = 10
val normalVal: Int
    @Composable get() = 10

@Composable
fun test1() {
    print(readOnly())
    print(readonlyVal)
}

@Composable @ReadOnlyComposable
fun test2() {
    print(readOnly())
    print(readonlyVal)
}

@Composable
fun test3() {
    print(readOnly())
    print(normal())
    print(readonlyVal)
    print(normalVal)
}

@Composable @ReadOnlyComposable
fun test4() {
    print(readOnly())
    print(<!NONREADONLY_CALL_IN_READONLY_COMPOSABLE!>normal<!>())
    print(readonlyVal)
    print(<!NONREADONLY_CALL_IN_READONLY_COMPOSABLE!>normalVal<!>)
}

val test5: Int
    @Composable
    get() {
        print(readOnly())
        print(readonlyVal)
        return 10
    }

val test6: Int
    @Composable @ReadOnlyComposable
    get() {
        print(readOnly())
        print(readonlyVal)
        return 10
    }

val test7: Int
    @Composable
    get() {
        print(readOnly())
        print(normal())
        print(readonlyVal)
        print(normalVal)
        return 10
    }

val test8: Int
    @Composable @ReadOnlyComposable
    get() {
        print(readOnly())
        print(<!NONREADONLY_CALL_IN_READONLY_COMPOSABLE!>normal<!>())
        print(readonlyVal)
        print(<!NONREADONLY_CALL_IN_READONLY_COMPOSABLE!>normalVal<!>)
        return 10
    }
