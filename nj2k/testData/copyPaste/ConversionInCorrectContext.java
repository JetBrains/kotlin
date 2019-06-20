class C {
<selection>    double foo(Dependency p) {
        return p.getInt(); // explicit conversion to Double must be added on conversion (if type Dependency) is correctly resolved
    }
</selection>}