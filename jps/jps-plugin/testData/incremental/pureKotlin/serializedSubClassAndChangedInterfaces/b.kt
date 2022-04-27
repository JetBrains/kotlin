package test

data class B(val a: A) // generates a call to A::hashCode that is a fake override in A
