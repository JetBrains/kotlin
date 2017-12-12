/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import ffmpeg.*
import kotlin.system.*
import kotlinx.cinterop.*
import konan.worker.*
import platform.posix.memcpy

// This global variable only set to != null value in the decoding worker.
private var state: DecodeWorkerState? = null

data class OutputInfo(val width: Int, val height: Int, val pixelFormat: AVPixelFormat)
data class VideoInfo(val width: Int, val height: Int, val fps: Double,
                     val sampleRate: Int, val channels: Int)

data class VideoFrame(val buffer: CPointer<AVBufferRef>, val lineSize: Int, val timeStamp: Double) {
    fun unref() {
        av_buffer_unref2(buffer)
    }
}

data class AudioFrame(val buffer: CPointer<AVBufferRef>, var position: Int, val size: Int, val timeStamp: Double) {
    fun unref() {
        av_buffer_unref2(buffer)
    }
}

private fun Int.checkError() {
    if (this != 0) {
        val buffer = ByteArray(1024)
        av_strerror(this, buffer.refTo(0), buffer.size.signExtend())
        throw Error("AVError: ${buffer.stringFromUtf8()}")
    }
}

class DecodeWorkerState(val formatContext: CPointer<AVFormatContext>,
                        val videoStreamIndex: Int,
                        val audioStreamIndex: Int,
                        val videoCodecContext: CPointer<AVCodecContext>?,
                        val audioCodecContext: CPointer<AVCodecContext>?) {
    var videoFrame: CPointer<AVFrame>? = null
    var scaledVideoFrame: CPointer<AVFrame>? = null
    var audioFrame: CPointer<AVFrame>? = null
    var resampledAudioFrame: CPointer<AVFrame>? = null
    var softwareScalingContext: CPointer<SwsContext>? = null
    var resampleContext: CPointer<AVAudioResampleContext>? = null
    val videoQueue = Queue<VideoFrame?>(100, null)
    val audioQueue = Queue<AudioFrame?>(100, null)
    var buffer: ByteArray? = null
    var videoWidth = 0
    var videoHeight = 0
    var windowWidth = 0
    var windowHeight = 0
    var scaledFrameSize = 0
    var noMoreFrames = false
    val minAudioFrames = 2
    val maxAudioFrames = 5
    val minVideoFrames = 5

    fun makeVideoFrame(): VideoFrame {
        // TODO: reuse buffers!
        // Convert the frame from its movie format to window pixel format.
        sws_scale(softwareScalingContext, videoFrame!!.pointed.data,
                  videoFrame!!.pointed.linesize, 0, videoHeight,
                  scaledVideoFrame!!.pointed.data, scaledVideoFrame!!.pointed.linesize)
        val buffer = av_buffer_alloc(scaledFrameSize)!!
        val ts = av_frame_get_best_effort_timestamp(videoFrame)* av_q2d(
                videoCodecContext!!.pointed.time_base.readValue())
        memcpy(buffer.pointed.data, scaledVideoFrame!!.pointed.data[0], scaledFrameSize.signExtend())
        return VideoFrame(buffer, scaledVideoFrame!!.pointed.linesize[0], ts)
    }

    fun makeAudioFrame(): AudioFrame {
        avresample_convert_frame(resampleContext, resampledAudioFrame, audioFrame).checkError()
        val audioFrameSize = av_samples_get_buffer_size(
                null,
                resampledAudioFrame!!.pointed.channels,
                resampledAudioFrame!!.pointed.nb_samples,
                resampledAudioFrame!!.pointed.format,
                1)
        val ts = av_frame_get_best_effort_timestamp(audioFrame) * av_q2d(
                audioCodecContext!!.pointed.time_base.readValue())
        val buffer = av_buffer_alloc(audioFrameSize)!!
        memcpy(buffer.pointed.data, resampledAudioFrame!!.pointed.data[0], audioFrameSize.signExtend())
        return AudioFrame(buffer, 0, audioFrameSize, ts)
    }

    private fun setResampleOpt(name: String, value: Int) =
            av_opt_set_int(resampleContext, name, value.signExtend(), 0)


    fun start(output: OutputInfo) {
        if (videoCodecContext != null) {
            videoWidth = videoCodecContext.pointed.width
            videoHeight = videoCodecContext.pointed.height
            windowWidth = output.width
            windowHeight = output.height
            videoFrame = av_frame_alloc()!!
            scaledVideoFrame = av_frame_alloc()!!
            softwareScalingContext = sws_getContext(
                    videoWidth,
                    videoHeight,
                    videoCodecContext.pointed.pix_fmt,
                    windowWidth, windowHeight, output.pixelFormat,
                    SWS_BILINEAR, null, null, null)!!

            scaledFrameSize = avpicture_get_size(output.pixelFormat, output.width, output.height)
            buffer = ByteArray(scaledFrameSize)

            avpicture_fill(scaledVideoFrame!!.reinterpret(), buffer!!.refTo(0),
                    output.pixelFormat, output.width, output.height)
        }

        if (audioCodecContext != null) {
            audioFrame = av_frame_alloc()!!
            resampledAudioFrame = av_frame_alloc()!!
            resampledAudioFrame!!.pointed.format = AV_SAMPLE_FMT_S16
            resampledAudioFrame!!.pointed.channels = 2
            resampledAudioFrame!!.pointed.channel_layout = AV_CH_LAYOUT_STEREO.signExtend()
            resampledAudioFrame!!.pointed.sample_rate = 44100
            resampleContext = avresample_alloc_context()
            setResampleOpt("in_channel_layout", audioCodecContext.pointed.channel_layout.narrow())
            setResampleOpt("out_channel_layout", AV_CH_LAYOUT_STEREO)
            setResampleOpt("in_sample_rate", audioCodecContext.pointed.sample_rate)
            setResampleOpt("out_sample_rate", 44100)
            setResampleOpt("in_sample_fmt", audioCodecContext.pointed.sample_fmt)
            setResampleOpt("out_sample_fmt", AV_SAMPLE_FMT_S16)
            avresample_open(resampleContext)
        }

        noMoreFrames = false

        decodeIfNeeded()
    }

    fun done() = noMoreFrames && videoQueue.isEmpty() && audioQueue.isEmpty()

    fun stop() {
        while (!videoQueue.isEmpty()) {
            videoQueue.pop()?.unref()
        }
        while (!audioQueue.isEmpty()) {
            audioQueue.pop()?.unref()
        }
        if (videoFrame != null) {
            av_frame_unref(videoFrame)
            videoFrame = null
        }
        if (scaledVideoFrame != null) {
            av_frame_unref(scaledVideoFrame)
            scaledVideoFrame = null
        }
        if (audioFrame != null) {
            av_frame_unref(audioFrame)
            audioFrame = null
        }
        if (resampledAudioFrame != null) {
            av_frame_unref(resampledAudioFrame)
            resampledAudioFrame = null
        }
        if (softwareScalingContext != null) {
            sws_freeContext(softwareScalingContext)
            softwareScalingContext = null
        }
        if (resampleContext != null) {
            avresample_free2(resampleContext)
            resampleContext = null
        }
        if (videoCodecContext != null) {
            avcodec_free_context2(videoCodecContext)
        }
        if (audioCodecContext != null) {
            avcodec_free_context2(audioCodecContext)
        }
        //avformat_free_context2(formatContext)
    }

    fun needMoreBuffers(): Boolean {
        return ((videoStreamIndex != -1) && (videoQueue.size() < minVideoFrames)) ||
                ((audioStreamIndex != -1) && (audioQueue.size() < minAudioFrames))
    }

    fun decodeIfNeeded() {
        if (!needMoreBuffers() || audioQueue.size() > audioQueue.maxSize - 20 ||
                videoQueue.size() > videoQueue.maxSize - 5) return

        memScoped {
            val packet = alloc<AVPacket>()
            val frameFinished = alloc<IntVar>()
            while (needMoreBuffers() && av_read_frame(formatContext, packet.ptr) >= 0) {
                when (packet.stream_index) {
                    videoStreamIndex -> {
                        // Decode video frame.
                        avcodec_decode_video2(videoCodecContext, videoFrame, frameFinished.ptr, packet.ptr)
                        // Did we get a video frame?
                        if (frameFinished.value != 0) {
                            videoQueue.push(makeVideoFrame())
                        }
                    }
                    audioStreamIndex -> {
                        while (packet.size > 0) {
                            val size = avcodec_decode_audio4(
                                    audioCodecContext, audioFrame, frameFinished.ptr, packet.ptr)

                            if (frameFinished.value != 0) {
                                // Put audio frame to decoder's queue.
                                audioQueue.push(makeAudioFrame())
                            }
                            packet.size -= size
                            packet.data += size
                        }
                    }
                }
                av_packet_unref(packet.ptr)
            }
            if (needMoreBuffers()) noMoreFrames = true
        }
    }

    fun nextVideoFrame(): VideoFrame? {
        decodeIfNeeded()

        if (videoQueue.isEmpty()) {
            return null
        }
        val frame = videoQueue.pop()!!
        return frame
    }

    fun nextAudioFrame(size: Int): AudioFrame? {
        decodeIfNeeded()

        if (audioQueue.isEmpty()) {
            return null
        }
        val frame = audioQueue.peek()!!
        var realSize = if (frame.position + size > frame.size) frame.size - frame.position else size
        if (frame.position + realSize == frame.size) {
            return audioQueue.pop()
        } else {
            val result = AudioFrame(av_buffer_ref(frame.buffer)!!, frame.position, frame.size, frame.timeStamp)
            frame.position += realSize
            return result
        }
    }

    fun audioVideoSynced() = (audioQueue.size() < maxAudioFrames) || done()
}

class DecodeWorker {
    // This class must have no other state, but this worker object.
    // All the real state must be stored on the worker's side.
    private val decodeWorker: Worker

    constructor() {
        decodeWorker = konan.worker.startWorker()
    }

    constructor(id: WorkerId) {
        decodeWorker = Worker(id)
    }

    fun workerId() = decodeWorker.id

    fun renderPixelFormat(pixelFormat: PixelFormat) = when (pixelFormat) {
        PixelFormat.RGB24 -> AV_PIX_FMT_RGB24
        PixelFormat.ARGB32 -> AV_PIX_FMT_RGB32
        PixelFormat.INVALID -> AV_PIX_FMT_NONE
    }

    private fun findStream(useStream: Boolean, formatContext: CPointer<AVFormatContext>, streamIndex: Int, tag: String):
            Pair<CPointer<AVStream>?, CPointer<AVCodecContext>?> {
        if (streamIndex < 0 || !useStream) return null to null
        val stream = formatContext.pointed.streams!!.get(streamIndex)!!
        // Get codec context for the video stream.
        val codecContext = stream.pointed.codec!!
        val codec = avcodec_find_decoder(codecContext.pointed.codec_id)
        if (codec == null)
            throw Error("Unsupported $tag codec...")
        // Open codec.
        if (avcodec_open2(codecContext, codec, null) < 0)
            throw Error("Couldn't open $tag codec")
        return stream to codecContext
    }

    fun initDecode(file: String, useVideo: Boolean = true, useAudio: Boolean = true): VideoInfo {
        memScoped {
            try {
                val formatContextPtr = alloc<CPointerVar<AVFormatContext>>()
                if (avformat_open_input(formatContextPtr.ptr, file, null, null) != 0)
                    throw Error("Cannot open video file")
                val formatContext = formatContextPtr.value!!
                if (avformat_find_stream_info(formatContext, null) < 0)
                    throw Error("Couldn't find stream information")
                av_dump_format(formatContext, 0, file, 0)

                // Find the first video/audio streams.
                var videoStreamIndex = -1
                var audioStreamIndex = -1
                for (i in 0 until formatContext.pointed.nb_streams) {
                    val stream = formatContext.pointed.streams!!.get(i)
                    val codec = stream!!.pointed.codec!!.pointed
                    if (codec.codec_type == AVMEDIA_TYPE_VIDEO && videoStreamIndex == -1) {
                        videoStreamIndex = i
                    }
                    if (codec.codec_type == AVMEDIA_TYPE_AUDIO && audioStreamIndex == -1) {
                        audioStreamIndex = i
                    }
                }

                val (videoStream, videoCodecContext) = findStream(useVideo, formatContext, videoStreamIndex, "video")
                val (_, audioCodecContext) = findStream(useAudio, formatContext, audioStreamIndex, "audio")

                // Extract video info.
                val (videoWidth, videoHeight, fps) = if (videoCodecContext != null) {
                    Triple(videoCodecContext.pointed.width, videoCodecContext.pointed.height,
                            av_q2d(av_stream_get_r_frame_rate(videoStream)))
                } else {
                    Triple(-1, -1, 0.0)
                }
                val (sampleRate, channels) = if (audioCodecContext != null) {
                    Pair(audioCodecContext.pointed.sample_rate, audioCodecContext.pointed.channels)
                } else {
                    Pair(0, 0)
                }

                // Pack all inited state and pass it to the worker.
                decodeWorker.schedule(TransferMode.CHECKED, {
                    DecodeWorkerState(formatContext,
                            videoStreamIndex, audioStreamIndex,
                            videoCodecContext, audioCodecContext)
                }) { input ->
                    state = input
                    null
                }
                return VideoInfo(videoWidth, videoHeight, fps, sampleRate, channels)
            } finally {
                // TODO: clean up whatever we allocated.
            }
        }
    }

    fun init() {
    }

    fun deinit() {
        decodeWorker.requestTermination().result()
    }

    fun start(width: Int, height: Int, pixelFormat: PixelFormat) {
        decodeWorker.schedule(TransferMode.CHECKED, {
            OutputInfo(width, height, renderPixelFormat(pixelFormat))
        }) { input -> state!!.start(input) }
    }

    fun stop() {
        decodeWorker.schedule(
                TransferMode.CHECKED,
                { null }) { _ ->
                    state?.stop()
                    state = null
                }.result()
    }

    // TODO: we manually box returned primitive value,
    // fix by autoboxing schedule()'s result in the compiler.
    fun done() = decodeWorker.schedule(TransferMode.CHECKED,
            { null }) { _ -> (state == null || state!!.done()) as Boolean?
    }.consume { it -> it!! }


    fun requestDecodeChunk() = decodeWorker.schedule(
            TransferMode.CHECKED,
            { null }) { _ -> state!!.decodeIfNeeded() }

    fun nextVideoFrame(): VideoFrame? = decodeWorker.schedule(
            TransferMode.CHECKED,
            { null }) { _ -> state!!.nextVideoFrame() }.result()

    fun nextAudioFrame(size: Int) = decodeWorker.schedule(
            TransferMode.CHECKED,
            { size }) { input -> state!!.nextAudioFrame(input) }.result()

    fun audioVideoSynced() = decodeWorker.schedule(
            TransferMode.CHECKED,
            { null }) { _ -> state!!.audioVideoSynced() }.result()
}
