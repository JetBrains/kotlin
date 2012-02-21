package std

inline fun Int.times(body : () -> Unit) {
    var count = this;
    while (count > 0) {
       body()
       count--
    }
}

inline fun parseInt(str : String) : Int? {
    try {
        return Integer.parseInt(str).sure();
    }
    catch (e : NumberFormatException) {
        return null;
    }
}
