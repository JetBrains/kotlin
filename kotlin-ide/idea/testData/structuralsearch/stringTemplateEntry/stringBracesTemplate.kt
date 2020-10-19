val a = 0

val b = <warning descr="SSR">"Hello world! $a"</warning>

val c = <warning descr="SSR">"Hello world! ${a}"</warning>