namespace foo


  class A(t : Int) : Comparable<A> {
    var i = t
    override fun compareTo(other : A) = (this.i - other.i)
  }

fun box() : Boolean{
 return (A(3) > A(2)) && (A(2) >= A(2)) && (A(1) >= A(0)) && (A(2) <= A(2)) && (A(3) <= A(4)) && (A(0) < A(100))
}



fun main(args : Array<String>) {
 System.out?.println(box())
}