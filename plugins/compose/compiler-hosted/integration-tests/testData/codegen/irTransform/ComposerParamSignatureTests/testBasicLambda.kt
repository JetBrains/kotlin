val foo = @Composable { x: Int -> print(x)  }
@Composable fun Bar() {
  foo(123)
}

fun used(x: Any?) {}
