package foo

val String.prop : Int
  get() = length

val Int.quadruple : Int
  get() = this * 4

fun box() : Boolean
{
    if ("1".prop != 1) return false;
    if ("11".prop != 2) return false;
    if (("121" + "123").prop != 6) return false;
    if (1.quadruple != 4) return false;
    if (0.quadruple != 0) return false;
    return true;
}