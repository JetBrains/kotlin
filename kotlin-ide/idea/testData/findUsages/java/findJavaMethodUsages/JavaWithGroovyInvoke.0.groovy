class GroovyClass extends JavaWithGroovyInvoke_0 {
    public def fieldNoType = new JavaWithGroovyInvoke_0.OtherJavaClass()
    public def JavaWithGroovyInvoke_0.OtherJavaClass fieldWithType = fieldNoType

    def methodNoType() {
        new JavaWithGroovyInvoke_0.OtherJavaClass()
    }

    JavaWithGroovyInvoke_0.OtherJavaClass methodWithType() {
        new JavaWithGroovyInvoke_0.OtherJavaClass()
    }
}