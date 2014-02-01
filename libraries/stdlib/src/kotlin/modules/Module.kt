package kotlin.modules

public trait Module {
    public fun getModuleName(): String

    public fun getOutputDirectory(): String

    public fun getSourceFiles(): List<String>

    public fun getClasspathRoots(): List<String>

    public fun getAnnotationsRoots(): List<String>
}
