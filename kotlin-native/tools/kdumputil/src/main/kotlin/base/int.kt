package base

fun Int.align(alignment: Int) = alignment.dec().let { mask -> this.plus(mask).and(mask.inv()) }
