import apt.Inspect

// Compiled without Valhalla value classes, so these are identity classes in their class files
@Inspect
value class NoValhalla(val x: Int) : NoValhallaBase()

@Inspect
abstract value class NoValhallaBase
