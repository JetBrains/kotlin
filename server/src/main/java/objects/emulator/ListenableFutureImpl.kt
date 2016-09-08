package objects.emulator

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.asynchttpclient.ListenableFuture
import org.asynchttpclient.Response
import org.asynchttpclient.netty.EagerResponseBodyPart
import org.asynchttpclient.netty.NettyResponse
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class ListenableFutureImpl(bytes: ByteArray) : ListenableFuture<Response> {

    val response = NettyResponse(null, null, listOf(EagerResponseBodyPart(Unpooled.copiedBuffer(bytes), true)))

    override fun done() {

    }

    override fun cancel(p0: Boolean): Boolean {
        return false
    }

    override fun touch() {
    }

    override fun get(): Response {
        return response
    }

    override fun get(p0: Long, p1: TimeUnit): Response {
        return response
    }

    override fun addListener(listener: Runnable?, exec: Executor?): ListenableFuture<Response> {
        return this
    }

    override fun abort(t: Throwable?) {
    }

    override fun isDone(): Boolean {
        return true
    }

    override fun toCompletableFuture(): CompletableFuture<Response> {
        throw UnsupportedOperationException("not implemented")
    }

    override fun isCancelled(): Boolean {
        return false
    }
}