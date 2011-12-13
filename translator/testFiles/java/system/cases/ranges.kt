import java.util.ArrayList;

fun main(args : Array<String>) {
    val x = Integer.parseInt(args[0])
    //Check if a number lies within a range:
    val y = 10
    if (x in 1..y-1)
        System.out?.println("OK")

    //Iterate over a range:
    for (a in 1..5)
    System.out?.print(" ${a}")

    //Check if a number is out of range:
    System.out?.println()
    val array = ArrayList<String>();

    array.add("aaa")
    array.add("bbb")
    array.add("ccc")

    if (x !in 0..array.size())
        System.out?.println("Out: array has only ${array.size()} elements. x = ${x}")

    //Check if a collection contains an object:
    if ("aaa" in array) // collection.contains(obj) is called
        System.out?.println("Yes: array contains aaa")

    if ("ddd" in array) // collection.contains(obj) is called
        System.out?.println("Yes: array contains ddd")
    else
        System.out?.println("No: array doesn't contains ddd")
}

 
