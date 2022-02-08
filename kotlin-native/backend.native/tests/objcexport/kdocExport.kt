/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kdocExport

/**
 * Summary class [KDocExport].
 *
 * @property xyzzy Doc for property xyzzy
 * @property zzz See below.
 */

// Expected: this comment shall not affect KDoc (i.e. kdoc above is still OK)
class KDocExport() {
    /**
     * @param xyzzy is documented.
     *
     * This is multi-line KDoc. See a blank line above.
     */
    val xyzzy: String = "String example"

    /** Non-primary ctor KDoc: */
    constructor(name: String) : this() {
        println(name)
    }

            /** @property xyzzy KDoc for foo? */
    val foo = "foo"
    /** @property foo KDoc for yxxyz? */
    var yxxyz = 0;
}


/**
 * Useless function [whatever]
 *
 * This kdoc has some additional formatting.
 * @param a keep intact and return
 * @return value of [a]
 * Check for additional comment (note) below
 */
@Throws(IllegalArgumentException::class)
fun whatever(a:String) = a

public abstract class SomeClassWithProperty
{
    /**
     * Returns dispatcher that executes coroutines immediately when it is already in the right context
     * (e.g. current looper is the same as this handler's looper) without an additional [re-dispatch][CoroutineDispatcher.dispatch].
     *
     * Immediate dispatcher is safe from stack overflows and in case of nested invocations forms event-loop similar to [Dispatchers.Unconfined].
     * The event loop is an advanced topic and its implications can be found in [Dispatchers.Unconfined] documentation.
     * The formed event-loop is shared with [Unconfined] and other immediate dispatchers, potentially overlapping tasks between them.
     *
     * Example of usage:
     * ```
     * suspend fun updateUiElement(val text: String) {
     *   /*
     *    * If it is known that updateUiElement can be invoked both from the Main thread and from other threads,
     *    * `immediate` dispatcher is used as a performance optimization to avoid unnecessary dispatch.
     *    *
     *    * In that case, when `updateUiElement` is invoked from the Main thread, `uiElement.text` will be
     *    * invoked immediately without any dispatching, otherwise, the `Dispatchers.Main` dispatch cycle will be triggered.
     *    */
     *   withContext(Dispatchers.Main.immediate) {
     *     uiElement.text = text
     *   }
     *   // Do context-independent logic such as logging
     * }
     * ```
     *
     * Method may throw [UnsupportedOperationException] if immediate dispatching is not supported by current dispatcher,
     * please refer to specific dispatcher documentation.
     *
     * [Dispatchers.Main] supports immediate execution for Android, JavaFx and Swing platforms.
     */
    public abstract val heavyFormattedKDocFoo: SomeClassWithProperty
}