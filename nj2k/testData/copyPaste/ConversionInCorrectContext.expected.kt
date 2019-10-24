fun foo(p: Dependency): Double {
    return p.getInt().toDouble() // explicit conversion to Double must be added on conversion (if type Dependency) is correctly resolved
}
