fun foo(x: Int = 91, y: String? = null): Int = 42 + x + if (y != null) y.length else 0
