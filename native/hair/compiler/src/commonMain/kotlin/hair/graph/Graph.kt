package hair.graph

interface DiGraph<N> {
    fun preds(n: N): Sequence<N>
    fun succs(n: N): Sequence<N>
}

interface FlowGraph<N> : DiGraph<N> {
    val root: N
}

//interface GraphBuilder<N> {
//    fun addEdge(from: N, to: N)
//
//}

//class DiGraphDSL<N>(val builder: GraphBuilder<N>) {
//    interface Fragment<N> {
//        val sources: List<N>
//        val sinks: List<N>
//    }
//    interface SingleSource<N> : Fragment<N> {
//        val source: N
//        override val sources: List<N> get() = listOf(source)
//    }
//    interface SingleSink<N> : Fragment<N> {
//        val sink: N
//        override val sinks: List<N> get() = listOf(sink)
//    }
//    data class Hammock<N>(override val source: N, override val sink: N) : SingleSource<N>, SingleSink<N>
//    data class Fountain<N>(override val source: N, override val sinks: List<N>): SingleSource<N>
//    data class Funnel<N>(override val sources: List<N>, override val sink: N) : SingleSink<N>
//    data class Mess<N>(override val sources: List<N>, override val sinks: List<N>) : Fragment<N>
//
//    infix fun N.to(next: N): Hammock<N> {
//        builder.addEdge(this, next)
//        return Hammock(this, next)
//    }
//
//    infix fun Hammock<N>.to(next: N): Hammock<N> {
//        builder.addEdge(this.sink, next)
//        return Hammock(this.source, next)
//    }
//
//    infix fun Funnel<N>.to(next: N): Funnel<N> {
//        builder.addEdge(this.sink, next)
//        return Funnel(this.sources, next)
//    }
//
//    fun branch()
//}
//
///*
//A to B to C to branch(D to E, F) to G
//A to B to C to (D to E || F) to G
//A to B to C to (D to E || F) to G
// */
