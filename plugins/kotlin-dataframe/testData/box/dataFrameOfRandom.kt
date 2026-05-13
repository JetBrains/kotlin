import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.annotations.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*

fun box(): String {
    dataFrameOf("a").randomInt(10).let {
        it.compareSchemas(strict = true)
    }

    dataFrameOf("a", "b", "c").randomInt(10).let {
        it.compareSchemas(strict = true)
    }

    dataFrameOf("a").randomInt(10, 0..100).let {
        it.compareSchemas(strict = true)
    }

    dataFrameOf("a").randomDouble(10).let {
        it.compareSchemas(strict = true)
    }

    dataFrameOf("a").randomDouble(10, 0.0..1.0).let {
        it.compareSchemas(strict = true)
    }

    dataFrameOf("a").randomFloat(10).let {
        it.compareSchemas(strict = true)
    }

    dataFrameOf("a").randomLong(10).let {
        it.compareSchemas(strict = true)
    }

    dataFrameOf("a").randomLong(10, 0L..100L).let {
        it.compareSchemas(strict = true)
    }

    dataFrameOf("a").randomBoolean(10).let {
        it.compareSchemas(strict = true)
    }
    return "OK"
}
