internal class C {
    fun bar(o: Any?, collection: Collection<String?>?): Boolean {
        return o is Collection<*> && collection is List<*>
    }
}
