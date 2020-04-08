typealias TA = () -> Unit
// CONTAINS (key="Function0", value="TA")

typealias TB = () -> String
// CONTAINS (key="Function0", value="TB")

typealias TC = (String) -> Unit
// CONTAINS (key="Function1", value="TC")

typealias TD = String.() -> Unit
// CONTAINS (key="Function1", value="TD")

typealias TE = (String, String) -> Unit
// CONTAINS (key="Function2", value="TE")

typealias TF = String.(String) -> Unit
// CONTAINS (key="Function2", value="TF")