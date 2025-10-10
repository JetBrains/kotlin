abstract class S0 : suspend () -> Unit
abstract class S1 : suspend (String) -> String
abstract class S1N : suspend (Int) -> String?
abstract class S0S0 : suspend () -> suspend () -> Any
