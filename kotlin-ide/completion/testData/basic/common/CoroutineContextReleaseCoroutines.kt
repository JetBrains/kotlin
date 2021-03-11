suspend fun foo() {
    corou<caret>
}

// ABSENT: {"lookupString":"coroutineContext","tailText":" (kotlin.coroutines.experimental)","typeText":"CoroutineContext","attributes":"","allLookupStrings":"coroutineContext, getCoroutineContext","itemText":"coroutineContext"}
// EXIST: {"lookupString":"coroutineContext","tailText":" (kotlin.coroutines)","typeText":"CoroutineContext","attributes":"","allLookupStrings":"coroutineContext, getCoroutineContext","itemText":"coroutineContext"}
// ABSENT: {"lookupString":"coroutineContext","tailText":" (kotlin.coroutines.experimental.intrinsics)","typeText":"CoroutineContext","attributes":"","allLookupStrings":"coroutineContext, getCoroutineContext","itemText":"coroutineContext"}
