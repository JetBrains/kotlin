package test

class ProducerImpl : Producer<String> {
    override fun produce() = "test"
}

class ConsumerImpl : Consumer<String> {
    override fun consume(v: String) = v.length
}

fun bar(producer: Producer<String>, consumer: Consumer<String>): Int = consumer.consume(producer.produce())
