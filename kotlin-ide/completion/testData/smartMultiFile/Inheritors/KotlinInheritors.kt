package p2

interface KotlinTrait

open class KotlinInheritor1 : KotlinTrait

class KotlinInheritor2(s: String) : KotlinInheritor1()

abstract class KotlinInheritor3 : KotlinTrait

class C {
    private class PrivateInheritor : KotlinTrait
}

object ObjectInheritor : KotlinTrait

// ALLOW_AST_ACCESS
