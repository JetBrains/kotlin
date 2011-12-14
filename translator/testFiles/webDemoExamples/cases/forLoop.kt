fun main(args : Array<String>) {
  for (arg in args)
    System.out?.println(arg)

  // or
  System.out?.println()
  for (i in args.indices)
    System.out?.println(args[i])
}
