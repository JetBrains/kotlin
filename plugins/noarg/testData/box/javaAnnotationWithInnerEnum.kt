// WITH_STDLIB
// ISSUE: KT-55887

// MODULE: lib
// FILE: NoArg.kt
annotation class NoArg

// FILE: Api.java
@NoArg
public @interface Api {
    Status status();

    enum Status {
        Ok, Error;
    }
}

// FILE: ExtendWith.java
@Api(status = Api.Status.Ok)
public @interface ExtendWith {}

// MODULE: main(lib)
// FILE: main.kt
@ExtendWith
class Test(val x: Int)

fun box(): String {
    Test::class.java.newInstance()
    return "OK"
}
