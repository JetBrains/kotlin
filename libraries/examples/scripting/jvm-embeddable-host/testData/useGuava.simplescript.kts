
// assuming to be executed on proguarded compiler in which `newArrayList` is present, but `asList` ist removed
// first pulling `newArrayList`, so with non-embeddable (not shaded) host/compiler the class `Lists` will be pulled from
// the guava embedded into compiler, then using `asList`, which should not be present in this version of the class due to proguarding
// So, the compilation should only succeed if shaded compiler is used and `Lists` are loaded from the non-embedded guava jar

val arr = com.google.common.collect.Lists.newArrayList<Int>()
val lst = listOf("Hello", "Guava")
val glist = com.google.common.collect.Lists.asList(lst.first(), lst[1], emptyArray())

println(glist.joinToString(", ", "", "!"))
