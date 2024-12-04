val objectA = object : AppInterface {}
val objectADelegate = object : AppInterface by objectA {}

val objectLib = object : LibInterface {}
val objectLibDelegate = object : LibInterface by objectLib {}
