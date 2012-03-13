package foo

var c0 = 0
var c1 = 0
var c2 = 0

class A() {
 var p = 0
 fun get(i : Int) : Int {
    c1++
    return 0
 }

 fun set(i : Int, value : Int) {
    c2++
 }

}

val a : A = A()
get() {
  c0++
  return $a 
}

fun box() : String {

    var d = a[1]
    if (c0 != 1) {
        return "1"
    }
     if (c1 != 1) {
        return "2"
    }
    ++a[1]
    if (c0 != 2) {
        return "3"
    }
    if (c1 != 3) {
    return "4"
    }
    if (c2 != 1) {
        return "5"
    }
   --a[1]
    if (c0 != 3) {
        return "6"
    }
  if (c1 != 5) {
    return "7"
  }
  if (c2 != 2) {
    return "8"
  }
  return "OK"
}