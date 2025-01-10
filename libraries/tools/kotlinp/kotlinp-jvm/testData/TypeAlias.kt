typealias F<T, U> = Map<T, (StringBuilder) -> U?>

typealias G<S> = F<List<S>, Set<S>>
