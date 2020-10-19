val a = true
val b = <warning descr="SSR">!a</warning>
val c = <warning descr="SSR">a.not()</warning>