fun foo() {}
fun bar() {}

fun box(): String {
    if (::foo !== ::foo) return "fail: ::foo is not equal to itself by reference"
    if (::foo === ::bar) return "fail: ::foo is equal to ::bar by reference"
    return "OK"
}