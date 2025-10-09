/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import org.gradle.api.Task
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.event.DefaultListenerManager
import org.gradle.internal.event.ListenerManager
import java.lang.reflect.Field


/**
 * Workaround for 'java.lang.OutOfMemoryError: Metaspace' when using the Worker API with classloader isolation
 * https://github.com/gradle/gradle/issues/18313
 */
fun Task.workaroundWorkerApiMemoryLeak() {
    val listenerManager: ListenerManager = (project as ProjectInternal).services.get(ListenerManager::class.java)

    doLast {
        clearDefaultCrossBuildInMemoryCaches(listenerManager)
    }
}

private fun clearDefaultCrossBuildInMemoryCaches(listenerManager: ListenerManager) {
    /** Store all listeners from all [DefaultListenerManager]s. */
    val allListeners = mutableListOf<Any>()

    var lm: DefaultListenerManager? = listenerManager as DefaultListenerManager
    do {
        /** Access [DefaultListenerManager.allListeners]. */
        val allListenersField: Field = DefaultListenerManager::class.java.getDeclaredField("allListeners")
            .apply { isAccessible = true }

        @Suppress("UNCHECKED_CAST")
        val l = allListenersField.get(lm) as Map<Object, Any>
        allListeners.addAll(l.keys)

        // also check the parent services - DefaultCrossBuildInMemoryCache is stored in the top-most service
        val parentField = DefaultListenerManager::class.java.getDeclaredField("parent")
            .apply { isAccessible = true }
        lm = parentField.get(lm) as? DefaultListenerManager
    } while (lm != null)

    allListeners
        // DefaultCrossBuildInMemoryCache is a private class, so fetch by name
        .filter { it::class.qualifiedName == "org.gradle.cache.internal.DefaultCrossBuildInMemoryCacheFactory.DefaultCrossBuildInMemoryCache" }
        // cast each cache to CrossBuildInMemoryCache, so we can run `clear()`
        .map { it as org.gradle.cache.internal.CrossBuildInMemoryCache<*, *> }
        .forEach { it.clear() }
}
