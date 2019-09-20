// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.stats.personalization.impl

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.stats.personalization.*
import org.jdom.Element
import java.text.DecimalFormat
import java.util.*

abstract class UserFactorStorageBase : UserFactorStorage, PersistentStateComponent<Element> {
    private companion object {
        val DOUBLE_VALUE_FORMATTER = DecimalFormat().apply {
            maximumFractionDigits = 6
            minimumFractionDigits = 1
            isGroupingUsed = false
        }
    }

    private val state = CollectorState()

    override fun <U : FactorUpdater> getFactorUpdater(description: UserFactorDescription<U, *>): U =
            description.updaterFactory.invoke(getAggregateFactor(description.factorId))

    override fun <R : FactorReader> getFactorReader(description: UserFactorDescription<*, R>): R =
            description.readerFactory.invoke(getAggregateFactor(description.factorId))

    override fun getState(): Element {
        val element = Element("component")
        state.writeState(element)
        return element
    }

    override fun loadState(newState: Element) {
        state.applyState(newState)
    }

    private fun getAggregateFactor(factorId: String): MutableDoubleFactor =
            state.aggregateFactors.computeIfAbsent(factorId, { DailyAggregateFactor() })

    private class CollectorState {
        val aggregateFactors: MutableMap<String, DailyAggregateFactor> = HashMap()

        fun applyState(element: Element) {
            aggregateFactors.clear()
            for (child in element.children) {
                val factorId = child.getAttributeValue("id")
                if (child.name == "factor" && factorId != null && UserFactorDescriptions.isKnownFactor(factorId)) {
                    val factor = DailyAggregateFactor.restore(child)
                    if (factor != null) aggregateFactors[factorId] = factor
                }
            }
        }

        fun writeState(element: Element) {
            for ((id, factor) in aggregateFactors.asSequence().sortedBy { it.key }) {
                val factorElement = Element("factor")
                factorElement.setAttribute("id", id)
                factor.writeState(factorElement)
                element.addContent(factorElement)
            }
        }
    }

    class DailyAggregateFactor private constructor(private val aggregates: SortedMap<Day, DailyData> = sortedMapOf())
        : MutableDoubleFactor {
        constructor() : this(sortedMapOf())

        init {
            ensureLimit()
        }

        companion object {
            private const val DAYS_LIMIT = 10

            fun restore(element: Element): DailyAggregateFactor? {
                val data = sortedMapOf<Day, DailyData>()
                for (child in element.children) {
                    val date = child.getAttributeValue("date")
                    val day = DayImpl.fromString(date)
                    if (child.name == "dailyData" && day != null) {
                        val dailyData = DailyData.restore(child)
                        if (dailyData != null) data.put(day, dailyData)
                    }
                }

                if (data.isEmpty()) return null
                return DailyAggregateFactor(data)
            }
        }

        fun writeState(element: Element) {
            for ((day, data) in aggregates) {
                val dailyDataElement = Element("dailyData")
                dailyDataElement.setAttribute("date", day.toString())
                data.writeState(dailyDataElement)
                element.addContent(dailyDataElement)
            }
        }

        override fun availableDays(): List<Day> = aggregates.keys.toList()

        override fun incrementOnToday(key: String): Boolean {
            return updateOnDate(DateUtil.today()) {
                compute(key, { _, oldValue -> if (oldValue == null) 1.0 else oldValue + 1.0 })
            }
        }

        override fun onDate(date: Day): Map<String, Double>? = aggregates[date]?.data

        override fun updateOnDate(date: Day, updater: MutableMap<String, Double>.() -> Unit): Boolean {
            val old = aggregates[date]
            if (old != null) {
                updater.invoke(old.data)
                return true
            }

            if (aggregates.size < DAYS_LIMIT || aggregates.firstKey() < date) {
                val data = DailyData()
                updater.invoke(data.data)
                aggregates.put(date, data)
                ensureLimit()
                return true
            }

            return false
        }

        private fun ensureLimit() {
            while (aggregates.size > DAYS_LIMIT) {
                aggregates.remove(aggregates.firstKey())
            }
        }
    }

    private class DailyData(val data: MutableMap<String, Double> = HashMap()) {
        companion object {
            fun restore(element: Element): DailyData? {
                val data = mutableMapOf<String, Double>()
                for (child in element.children) {
                    if (child.name == "observation") {
                        val dataKey = child.getAttributeValue("name")
                        val dataValue = child.getAttributeValue("value")

                        // skip all if any observation is inconsistent
                        val value = dataValue.toDoubleOrNull() ?: return null
                        data[dataKey] = value
                    }
                }

                if (data.isEmpty()) return null
                return DailyData(data)
            }
        }

        fun writeState(element: Element) {
            for ((key, value) in data.asSequence().sortedBy { it.key }) {
                val observation = Element("observation")
                observation.setAttribute("name", key)
                observation.setAttribute("value", DOUBLE_VALUE_FORMATTER.format(value))
                element.addContent(observation)
            }
        }
    }
}