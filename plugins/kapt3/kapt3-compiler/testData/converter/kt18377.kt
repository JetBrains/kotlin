import java.util.Date

fun Date(double: Double): Date = Date(double.times(1000).toLong())