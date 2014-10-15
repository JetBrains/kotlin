package language

import org.junit.Test as test
import kotlin.test.*

class Product(val name: String, val price: Double) {
}

class Customer(val name: String, val products: List<Product>) {
}


fun customerTemplate(customer: Customer) = """
<html>
<body>
<h1>Hello ${customer.name}</h1>
<ul>
${customer.products.map{ productSnippet(it) }.join("\n")}
</ul>
<p>lets do some kool stuff</p>
</body>
"""

fun productSnippet(product: Product) = "<li>${product.name}. Price : ${product.price}</li>"

// TODO support number formatting methods?
// fun productSnippet(product: Product) = "<li>${product.name}. Price : ${product.price.format('## ###,00')} </li>"

val EXPECTED = """
<html>
<body>
<h1>Hello James</h1>
<ul>
<li>Beer. Price : 1.99</li>
<li>Wine. Price : 5.99</li>
</ul>
<p>lets do some kool stuff</p>
</body>
"""

class StringExpressionExampleTest {
    val customer = Customer("James", arrayListOf(Product("Beer", 1.99), Product("Wine", 5.99)))

    test fun testExpressions(): Unit {
        assertEquals(EXPECTED, customerTemplate(customer))
    }
}