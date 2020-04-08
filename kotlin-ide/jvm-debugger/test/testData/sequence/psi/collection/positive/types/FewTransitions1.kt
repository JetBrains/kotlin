fun main(args: Array<String>) {
  byteArray<caret>Of(10, 20)
      .map { it.toString() }
      .map { if (it == "10") null else 10 }
      .map { 10 }
      .contains(200)
}