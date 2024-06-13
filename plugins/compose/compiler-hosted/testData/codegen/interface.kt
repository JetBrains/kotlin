// DUMP_IR

// MODULE: lib
// FILE: core/data/repo.kt
package core.data

interface UserNewsResourceRepository {
    fun observeAll(): List<Int>
}

// MODULE: main(lib)
// FILE: main.kt
package home

import core.data.UserNewsResourceRepository

class MainActivity {
    lateinit var userNewsResourceRepository: UserNewsResourceRepository
}
