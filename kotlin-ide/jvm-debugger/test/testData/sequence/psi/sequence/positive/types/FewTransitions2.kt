fun main(args: Array<String>) {
  "jetBrains".asS<caret>equence().map { it.isLowerCase() }
      .flatMap { linkedSetOf(1.2, 3.0).asSequence() }
      .map { it.toString() }
      .count()
}