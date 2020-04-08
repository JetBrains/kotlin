package p

interface I1<T>
interface I2

interface KotlinTrait<T>

class KotlinInheritor<T> : KotlinTrait<I1<T>>

// ALLOW_AST_ACCESS
