/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.stats.personalization.impl

import com.intellij.openapi.diagnostic.Logger
import com.intellij.stats.personalization.Day
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Vitaliy.Bibaev
 */
class DayImpl(date: Date) : Day {
    override val dayOfMonth: Int
    override val month: Int
    override val year: Int

    companion object {
        private val LOG = Logger.getInstance(DayImpl::class.java)
        private val DATE_FORMAT = ThreadLocal.withInitial { SimpleDateFormat("dd-MM-yyyy") }

        fun fromString(str: String): Day? {
            val position = ParsePosition(0)

            val date: Date
            try {
                date = DATE_FORMAT.get().parse(str, position)
            }
            catch (e: NumberFormatException) {
                LOG.error("Could not parse a date from string: $str. Collected data for the day will be skipped.", e)
                return null
            }
            if (position.index == 0) return null
            return DayImpl(date)
        }
    }

    init {
        val calendar = Calendar.getInstance()
        calendar.time = date
        dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        // a month is a zero-based property for some reason
        // see details: https://stackoverflow.com/questions/344380/why-is-january-month-0-in-java-calendar
        month = calendar.get(Calendar.MONTH) + 1
        year = calendar.get(Calendar.YEAR)
    }

    override fun compareTo(other: Day): Int {
        if (year == other.year) {
            if (month == other.month) {
                return dayOfMonth.compareTo(other.dayOfMonth)
            }
            return month.compareTo(other.month)
        }
        return year.compareTo(other.year)
    }

    override fun hashCode(): Int {
        return Objects.hash(year, month, dayOfMonth)
    }

    override fun equals(other: Any?): Boolean {
        if (other != null && other is Day) return compareTo(other) == 0
        return false
    }

    override fun toString(): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, dayOfMonth)
        return DATE_FORMAT.get().format(calendar.time)
    }
}
