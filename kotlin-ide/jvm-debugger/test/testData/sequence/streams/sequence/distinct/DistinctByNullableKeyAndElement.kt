package streams.sequence.distinct

fun main(args: Array<String>) {
  //Breakpoint!
  sequenceOf(1, 2, null, null, 3, 1).distinctBy { if (it == null) 2 else if (it == 3) null else it }.count()
}