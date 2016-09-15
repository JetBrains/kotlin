// FQNAME: Test

class Test {
    @kotlin.jvm.Transient
    val f: String = ""
    
    // explicitly
    @org.jetbrains.annotations.NotNull
    fun a() {}
    
    @kotlin.jvm.Synchronized
    fun b() {}
    
    @kotlin.jvm.JvmOverloads
    fun c(a: Int = 3, b: String  = "") {}
    
    @java.lang.Deprecated
    fun d() {}
    
    @kotlin.Deprecated("")
    fun e() {}
}