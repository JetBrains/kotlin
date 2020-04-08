package a

class A {
}

operator fun A.get(s: String) {
}

<selection>fun f() {
    A()[""]
}</selection>