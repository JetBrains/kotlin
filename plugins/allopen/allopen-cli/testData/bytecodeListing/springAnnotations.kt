// FILE: component.kt
package org.springframework.stereotype
annotation class Component

// FILE: transactional.kt
package org.springframework.transaction.annotation
annotation class Transactional

// FILE: async.kt
package org.springframework.scheduling.annotation
annotation class Async

// FILE: cacheable.kt
package org.springframework.cache.annotation
annotation class Cacheable

// FILE: springBootTest.kt
package org.springframework.boot.test.context
annotation class SpringBootTest

// FILE: main.kt
package test

import org.springframework.stereotype.*
import org.springframework.transaction.annotation.*
import org.springframework.scheduling.annotation.*
import org.springframework.cache.annotation.*
import org.springframework.boot.test.context.*

@Component
class TestComponent

@Transactional
class TestTransactional

@Async
class TestAsync

@Cacheable
class TestCacheable

@SpringBootTest
class TestSpringBootTest

class NoAnno