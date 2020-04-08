class UseKotlinSubclassesOfMappedTypes {
    void test() {
        Iterable<String> iterable = new KotlinIterableTraitTest();
        Comparable<Integer> comparable = new KotlinComparableTest();
    }
}