package org.jetbrains.kotlin.maven;

interface Action<T> {
    void run(T param) throws Exception;
}
