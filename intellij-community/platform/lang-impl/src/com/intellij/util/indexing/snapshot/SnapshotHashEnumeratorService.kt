// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.snapshot

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.newvfs.persistent.FlushingDaemon
import com.intellij.util.hash.ContentHashEnumerator
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.IndexInfrastructure
import com.intellij.util.io.IOUtil
import gnu.trove.THashSet
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class SnapshotHashEnumeratorService : Closeable {
  val log = logger<SnapshotHashEnumeratorService>()

  companion object {
    @JvmStatic
    fun getInstance(): SnapshotHashEnumeratorService {
      return service()
    }

  }

  private enum class State { OPEN, OPEN_AND_CLEAN, CLOSED }

  interface HashEnumeratorHandle {
    @Throws(IOException::class)
    fun enumerateHash(digest: ByteArray): Int
    fun release()
  }

  @Volatile
  private var state: State = State.CLOSED

  @Volatile
  private var contentHashEnumerator: ContentHashEnumerator? = null

  private val handles: MutableSet<HashEnumeratorHandle> = THashSet()

  private val lock: Lock = ReentrantLock()

  init {
    FlushingDaemon.everyFiveSeconds { flush() }
  }

  @Throws(IOException::class)
  fun initialize(): Boolean {
    lock.withLock {
      if (state == State.CLOSED) {
        val hashEnumeratorFile = File(IndexInfrastructure.getPersistentIndexRoot(), "textContentHashes")
        state = State.OPEN
        contentHashEnumerator =
          IOUtil.openCleanOrResetBroken({ ContentHashEnumerator(hashEnumeratorFile.toPath()) },
                                        {
                                          IOUtil.deleteAllFilesStartingWith(hashEnumeratorFile)
                                          state = State.OPEN_AND_CLEAN
                                        })!!

      }
      log.assertTrue(state != State.CLOSED)
      return state == State.OPEN
    }
  }

  @Throws(IOException::class)
  override fun close() {
    lock.withLock {
      if (state == State.OPEN) {
        contentHashEnumerator!!.close()
        state = State.CLOSED

        log.assertTrue(handles.isEmpty(), "enumerator handles are still held: $handles")
        handles.clear()
      }
    }
  }

  fun flush() {
    lock.withLock {
      if (state == State.OPEN) {
        contentHashEnumerator!!.force()
      }
    }
  }

  fun createHashEnumeratorHandle(requestorIndexId: ID<*, *>): HashEnumeratorHandle {
    val handle = object : HashEnumeratorHandle {
      override fun enumerateHash(digest: ByteArray): Int = contentHashEnumerator!!.enumerate(digest)

      override fun release() {
        lock.withLock {
          handles.remove(this)
          log.assertTrue(state != State.CLOSED, "handle is released for closed enumerator")
        }
      }

      override fun toString(): String {
        return "handle for ${requestorIndexId.name}"
      }
    }

    lock.withLock {
      handles.add(handle)
    }

    return handle
  }
}

