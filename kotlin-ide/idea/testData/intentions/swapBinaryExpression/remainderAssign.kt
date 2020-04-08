// IS_APPLICABLE: false
fun new(x: Int, y: Int): Int {
    var c = 5
    c %= <caret>10
    return 1
}