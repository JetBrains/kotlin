// PROBLEM: Modifiers should follow annotations
annotation class Inject
annotation class VisibleForTesting

class Example {
    <caret>public @set:[Inject VisibleForTesting] var x: String = ""
}
