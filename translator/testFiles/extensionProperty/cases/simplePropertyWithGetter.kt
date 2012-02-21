package foo

val String.size : Int
  get() = length

val Int.quadruple : Int
  get() = this * 4

fun box() : Boolean
{
    if ("1".size != 1) return false;
    if ("11".size != 2) return false;
    if (("121" + "123").size != 6) return false;
    if (1.quadruple != 4) return false;
    if (0.quadruple != 0) return false;
    return true;
}