package kotlin

public inline fun Int.times(body : () -> Unit) {
    var count = this;
    while (count > 0) {
       body()
       count--
    }
}
