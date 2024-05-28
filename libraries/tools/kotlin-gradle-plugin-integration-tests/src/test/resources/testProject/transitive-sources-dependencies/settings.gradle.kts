
val isConsumer = false

if (isConsumer) {
    include(":consumer")
} else {
    include(":lib_with_sources")
    include(":lib_without_sources")
}

