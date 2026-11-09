package test

@kotlin.native.concurrent.ThreadLocal
object Counter {
    var n = 0
}
