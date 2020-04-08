fun foo(klass: Class<*>) {
    klass.<caret>
}

fun <T> Class<T>.extFun(): Class<in T> = TODO()

// EXIST: simpleName
// ABSENT: getSimpleName
// EXIST: enclosingClass
// ABSENT: getEnclosingClass
// EXIST: annotations
// ABSENT: getAnnotations
// EXIST: superclass
// ABSENT: getSuperclass
// EXIST: extFun
