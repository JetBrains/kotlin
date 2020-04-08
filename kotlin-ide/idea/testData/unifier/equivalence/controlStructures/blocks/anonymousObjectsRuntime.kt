fun foo() {
    when {
        true -> <selection>{
            val a = object: Function0<Int> {
                override fun invoke(): Int = 1
            }
            val b = object: Function0<Int> {
                override fun invoke(): Int = a() + 1
            }
            println(a() - b())
        }</selection>

        true -> {
            val b = object: Function0<Int> {
                override fun invoke(): Int = 1
            }
            val a = object: Function0<Int> {
                override fun invoke(): Int = b() + 1
            }
            println(b() - a())
        }

        true -> {
            val b = object: Function0<Int> {
                override fun invoke(): Int = 1
            }
            val a = object: Function0<Int> {
                override fun invoke(): Int = b() + 1
            }
            println(a() - b())
        }
    }
}