fun main(args: Array<String>) {
  listO<caret>f(20, 30).asSequence().map { if(it == 20) null else it }.count()
}