// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.openapi.project.Project
import com.intellij.ui.IconDeferrer
import com.intellij.util.containers.ObjectLongHashMap
import gnu.trove.THashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.swing.Icon
import kotlin.concurrent.read
import kotlin.concurrent.write

class TimedIconCache {
  private val idToIcon = THashMap<String, Icon>()
  private val idToInvalid = THashMap<String, Boolean>()
  private val iconCheckTimes = ObjectLongHashMap<String>()
  private val iconCalcTime = ObjectLongHashMap<String>()

  private val lock = ReentrantReadWriteLock()

  fun remove(id: String) {
    lock.write {
      idToIcon.remove(id)
      iconCheckTimes.remove(id)
      iconCalcTime.remove(id)
    }
  }

  fun get(id: String, settings: RunnerAndConfigurationSettings, project: Project): Icon {
    return lock.read { idToIcon.get(id) } ?: lock.write {
      idToIcon.get(id)?.let {
        return it
      }
      val icon = IconDeferrer.getInstance().deferAutoUpdatable(settings.configuration.icon, project.hashCode() xor settings.hashCode()) {
        if (project.isDisposed) {
          return@deferAutoUpdatable null
        }

        lock.write {
          iconCalcTime.remove(id)
        }

        val startTime = System.currentTimeMillis()

        val icon2Valid = calcIcon(settings, project)

        lock.write {
          iconCalcTime.put(id, System.currentTimeMillis() - startTime)
          idToInvalid.set(id, icon2Valid.second)
        }
        icon2Valid.first
      }

      set(id, icon)
      icon
    }
  }

  fun isInvalid(id: String) : Boolean {
    idToInvalid.get(id)?.let {return it}
    return false
  }

  private fun calcIcon(settings: RunnerAndConfigurationSettings, project: Project): Pair<Icon, Boolean> {
    try {
      settings.checkSettings()
      return ProgramRunnerUtil.getConfigurationIcon(settings, false).to(false)
    }
    catch (e: IndexNotReadyException) {
      return ProgramRunnerUtil.getConfigurationIcon(settings, false).to(false)
    }
    catch (ignored: RuntimeConfigurationException) {
      val invalid = !DumbService.isDumb(project)
      return ProgramRunnerUtil.getConfigurationIcon(settings, invalid).to(invalid)
    }
  }

  private fun set(id: String, icon: Icon) {
    idToIcon.put(id, icon)
    iconCheckTimes.put(id, System.currentTimeMillis())
  }

  fun clear() {
    lock.write {
      idToIcon.clear()
      iconCheckTimes.clear()
      iconCalcTime.clear()
    }
  }

  fun checkValidity(id: String) {
    lock.read {
      val lastCheckTime = iconCheckTimes.get(id)
      var expired = lastCheckTime == -1L
      if (!expired) {
        var calcTime = iconCalcTime.get(id)
        if (calcTime == -1L || calcTime < 150) {
          calcTime = 150L
        }
        expired = (System.currentTimeMillis() - lastCheckTime) > (calcTime * 10)
      }

      if (expired) {
        lock.write {
          idToIcon.remove(id)
        }
      }
    }
  }
}