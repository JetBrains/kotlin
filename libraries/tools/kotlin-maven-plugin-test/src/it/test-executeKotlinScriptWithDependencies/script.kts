import org.junit.Assert

println("Dependency jar is: ${Assert::class.java.getProtectionDomain().getCodeSource().getLocation().getPath().replace(Regex(".*/"), "")}")
