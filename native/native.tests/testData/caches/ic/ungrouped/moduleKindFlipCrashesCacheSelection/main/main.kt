import kotlin.test.*

@Test
fun runTest() {
    // The program cannot reference mixed's API: it is a Kotlin module at step 0 and a
    // cinterop module at step 1, and main is recompiled at every step. Nothing needs to be
    // asserted -- the link itself is the test. A correct IC selects or builds the right cache
    // kind for the flipped library and the binary builds and runs at both steps (the clean
    // and non-IC links do). Today the step-1 IC link crashes during cache selection with
    // "No 'ir' component in library .../klibs/mixed.klib" before this test could run.
}
