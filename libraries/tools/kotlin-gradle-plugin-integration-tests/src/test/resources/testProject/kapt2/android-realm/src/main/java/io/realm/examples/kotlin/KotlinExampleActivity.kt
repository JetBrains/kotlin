/*
 * Copyright 2015 Realm Inc.
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

package io.realm.examples.kotlin

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import io.realm.Realm
import io.realm.Sort
import io.realm.RealmConfiguration
import io.realm.examples.kotlin.model.Cat
import io.realm.examples.kotlin.model.Dog
import io.realm.examples.kotlin.model.Person
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import kotlin.properties.Delegates


class KotlinExampleActivity : Activity() {

    companion object {
        val TAG: String = KotlinExampleActivity::class.qualifiedName as String
    }

    private var rootLayout: LinearLayout by Delegates.notNull()
    private var realm: Realm by Delegates.notNull()
    private var realmConfig: RealmConfiguration by Delegates.notNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_realm_basic_example)
        rootLayout = findViewById(R.id.container) as LinearLayout
        rootLayout.removeAllViews()

        // These operations are small enough that
        // we can generally safely run them on the UI thread.

        // Create configuration and reset Realm.
        realmConfig = RealmConfiguration.Builder(this).build()
        Realm.deleteRealm(realmConfig)

        // Open the realm for the UI thread.
        realm = Realm.getInstance(realmConfig)

        basicCRUD(realm)
        basicQuery(realm)
        basicLinkQuery(realm)

        // Delete all persons
        // Using executeTransaction with a lambda reduces code size and makes it impossible
        // to forget to commit the transaction.
        realm.executeTransaction {
            realm.delete(Person::class.java)
        }

        // More complex operations can be executed on another thread, for example using
        // Anko's async extension method.
        doAsync() {
            var info: String
            info = complexReadWrite()
            info += complexQuery()

            uiThread {
                showStatus(info)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close() // Remember to close Realm when done.
    }

    private fun showStatus(txt: String) {
        Log.i(TAG, txt)
        val tv = TextView(this)
        tv.text = txt
        rootLayout.addView(tv)
    }

    private fun basicCRUD(realm: Realm) {
        showStatus("Perform basic Create/Read/Update/Delete (CRUD) operations...")

        // All writes must be wrapped in a transaction to facilitate safe multi threading
        realm.executeTransaction {
            // Add a person
            var person = realm.createObject(Person::class.java)
            person.id = 1
            person.name = "Young Person"
            person.age = 14
        }

        // Find the first person (no query conditions) and read a field
        var person = realm.where(Person::class.java).findFirst()
        showStatus(person.name + ": " + person.age)

        // Update person in a transaction
        realm.executeTransaction {
            person.name = "Senior Person"
            person.age = 99
            showStatus(person.name + " got older: " + person.age)
        }
    }

    private fun basicQuery(realm: Realm) {
        showStatus("\nPerforming basic Query operation...")
        showStatus("Number of persons: ${realm.where(Person::class.java).count()}")

        val results = realm.where(Person::class.java).equalTo("age", 99).findAll()

        showStatus("Size of result set: " + results.size)
    }

    private fun basicLinkQuery(realm: Realm) {
        showStatus("\nPerforming basic Link Query operation...")
        showStatus("Number of persons: ${realm.where(Person::class.java).count()}")

        val results = realm.where(Person::class.java).equalTo("cats.name", "Tiger").findAll()

        showStatus("Size of result set: ${results.size}")
    }

    private fun complexReadWrite(): String {
        var status = "\nPerforming complex Read/Write operation..."

        // Open the default realm. All threads must use it's own reference to the realm.
        // Those can not be transferred across threads.
        val realm = Realm.getInstance(realmConfig)

        // Add ten persons in one transaction
        realm.executeTransaction {
            val fido = realm.createObject(Dog::class.java)
            fido.name = "fido"
            for (i in 0..9) {
                val person = realm.createObject(Person::class.java)
                person.id = i.toLong()
                person.name = "Person no. $i"
                person.age = i
                person.dog = fido

                // The field tempReference is annotated with @Ignore.
                // This means setTempReference sets the Person tempReference
                // field directly. The tempReference is NOT saved as part of
                // the RealmObject:
                person.tempReference = 42

                for (j in 0..i - 1) {
                    val cat = realm.createObject(Cat::class.java)
                    cat.name = "Cat_$j"
                    person.cats.add(cat)
                }
            }
        }

        // Implicit read transactions allow you to access your objects
        status += "\nNumber of persons: ${realm.where(Person::class.java).count()}"

        // Iterate over all objects
        for (person in realm.where(Person::class.java).findAll()) {
            val dogName: String = person?.dog?.name ?: "None"

            status += "\n${person.name}: ${person.age} : $dogName : ${person.cats.size}"

            // The field tempReference is annotated with @Ignore
            // Though we initially set its value to 42, it has
            // not been saved as part of the Person RealmObject:
            check(person.tempReference == 0)
        }

        // Sorting
        val sortedPersons = realm.where(Person::class.java).findAllSorted("age", Sort.DESCENDING);
        check(realm.where(Person::class.java).findAll().last().name == sortedPersons.first().name)
        status += "\nSorting ${sortedPersons.last().name} == ${realm.where(Person::class.java).findAll().first().name}"

        realm.close()
        return status
    }

    private fun complexQuery(): String {
        var status = "\n\nPerforming complex Query operation..."

        // Realm implements the Closable interface, therefore we can make use of Kotlin's built-in
        // extension method 'use' (pun intended).
        Realm.getInstance(realmConfig).use {
            // 'it' is the implicit lambda parameter of type Realm
            status += "\nNumber of persons: ${it.where(Person::class.java).count()}"

            // Find all persons where age between 7 and 9 and name begins with "Person".
            val results = it
                    .where(Person::class.java)
                    .between("age", 7, 9)       // Notice implicit "and" operation
                    .beginsWith("name", "Person")
                    .findAll()

            status += "\nSize of result set: ${results.size}"

        }

        return status
    }


}
