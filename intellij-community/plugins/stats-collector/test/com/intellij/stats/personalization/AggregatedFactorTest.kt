package com.intellij.stats.personalization

import com.intellij.stats.personalization.impl.*
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.junit.Assert
import java.util.*

/**
 * @author Vitaliy.Bibaev
 */
class AggregatedFactorTest : UsefulTestCase() {
    private companion object {
        val DATE_1 = DateUtil.byDate(Calendar.Builder().setDate(2010, 0, 1).build().time)
        val DATE_2 = DATE_1.update(1)
        val DATE_3 = DATE_1.update(2)
        val DATE_4 = DATE_1.update(3)

        fun Day.update(count: Int): Day {
            return Calendar.getInstance().let {
                it.set(year, month - 1, dayOfMonth)
                it.add(Calendar.DATE, count)
                DateUtil.byDate(it.time)
            }
        }
    }

    fun `test min is correct`() {
        val aggregateFactor: MutableDoubleFactor = UserFactorStorageBase.DailyAggregateFactor()

        aggregateFactor.setOnDate(DATE_1, "count", 10.0)
        aggregateFactor.setOnDate(DATE_3, "count", 20.0)
        aggregateFactor.setOnDate(DATE_4, "delay", 1000.0)

        val mins = createFactorForTests().aggregateMin()
        TestCase.assertEquals(2, mins.size)
        UsefulTestCase.assertEquals(10.0, mins["count"])
        UsefulTestCase.assertEquals(1000.0, mins["delay"])
    }

    fun `test max is correct`() {
        val maximums = createFactorForTests().aggregateMax()
        TestCase.assertEquals(2, maximums.size)
        UsefulTestCase.assertEquals(20.0, maximums["count"])
        UsefulTestCase.assertEquals(1000.0, maximums["delay"])
    }

    fun `test sum is correct`() {
        val maximums = createFactorForTests().aggregateSum()
        TestCase.assertEquals(2, maximums.size)
        UsefulTestCase.assertEquals(30.0, maximums["count"])
        UsefulTestCase.assertEquals(1000.0, maximums["delay"])
    }

    fun `test average is only on present`() {
        val maximums = createFactorForTests().aggregateAverage()
        TestCase.assertEquals(2, maximums.size)
        UsefulTestCase.assertEquals(15.0, maximums["count"]!!, 1e-10)
        UsefulTestCase.assertEquals(1000.0, maximums["delay"]!!, 1e-10)
    }

    fun `test average does not lose precision`() {
        val factor: MutableDoubleFactor = UserFactorStorageBase.DailyAggregateFactor()
        factor.setOnDate(DATE_1, "key1", Double.MAX_VALUE)
        factor.setOnDate(DATE_2, "key1", Double.MAX_VALUE)

        val avg = factor.aggregateAverage()
        TestCase.assertEquals(1, avg.size)
        Assert.assertNotEquals(Double.POSITIVE_INFINITY, avg["key1"]!!)
    }

    fun `test factor stores information only for the last 10 days`() {
        val fieldName = "count"
        val factor: MutableDoubleFactor = UserFactorStorageBase.DailyAggregateFactor()
        for (i in 0 until 10) {
            TestCase.assertTrue(factor.updateOnDate(DATE_1.update(i)) {
                put(fieldName, i.toDouble())
            })
        }

        TestCase.assertEquals(45.0, factor.aggregateSum()[fieldName]!!, 1e-10)
        TestCase.assertNotNull(factor.onDate(DATE_1))
        TestCase.assertFalse(factor.updateOnDate(DATE_1.update(-100)) { put(fieldName, 100.0) })
        TestCase.assertEquals(9.0, factor.aggregateMax()[fieldName]!!, 1e-10)

        TestCase.assertTrue(factor.updateOnDate(DATE_1.update(100)) { put(fieldName, 1.0) })
        TestCase.assertEquals(46.0, factor.aggregateSum()[fieldName]!!, 1e-10)
    }

    private fun createFactorForTests(): DailyAggregatedDoubleFactor {
        val aggregateFactor: MutableDoubleFactor = UserFactorStorageBase.DailyAggregateFactor()

        aggregateFactor.setOnDate(DATE_1, "count", 10.0)
        aggregateFactor.setOnDate(DATE_3, "count", 20.0)
        aggregateFactor.setOnDate(DATE_4, "delay", 1000.0)

        return aggregateFactor
    }
}