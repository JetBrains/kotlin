fun foo(o: Any) {
    <caret>val a = 1
}

// INVOCATION_COUNT: 1
// EXIST: { itemText: "read", attributes: "bold" }
// EXIST: { itemText: "write", attributes: "bold" }

// RUNTIME_TYPE: java.util.concurrent.locks.ReentrantReadWriteLock