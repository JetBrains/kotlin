package test.collections

/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

/**
 * Created by claakman on 17.01.14.
 */

import kotlin.test.*

import java.util.*

import org.junit.Test as test

class MutableCollectionTest {

    test fun fromIterable() {

        val data = arrayListOf("foo", "bar") as Iterable<String>

        val collection = ArrayList<String>()
        collection.addAll(data)

        assertTrue {
            data.all { collection.containsItem(it) }
        }
    }

    test fun fromIterator() {
        val list = arrayListOf("foo", "bar")
        val collection = ArrayList<String>()

        collection.addAll(list.iterator())

        println(collection.toString())
        println(list.toString())

        assertTrue {
            collection.containsAll(list)
        }
    }

}