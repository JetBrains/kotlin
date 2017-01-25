package com.example.genclassesnotfound

import dagger.Module
import dagger.ObjectGraph
import dagger.Provides
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import javax.inject.Inject

class ExampleUnitTest {

    lateinit var graph: ObjectGraph
    lateinit @Inject var config: Config

    @Before
    fun setUp() {
        graph = ObjectGraph.create(TestModule())
        graph.inject(this)
    }

    @Test
    fun addition_isCorrect() {
        assertEquals(4 + config.magicNumber, 9)
    }

    @Module(
            injects = arrayOf(ExampleUnitTest::class),
            complete = true,
            library = true

    )
    class TestModule {
        @Provides fun providesDependency(): Config {
            return Config(5)
        }
    }

    data class Config(val magicNumber : Int)
}