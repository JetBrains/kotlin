import groovy.transform.CompileStatic

@CompileStatic
trait MyTrait {
    private transient boolean somePrivateField = false
    List<String> someField

    def foo() {
        return 1
    }
}
