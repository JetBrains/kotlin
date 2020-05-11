package org.khronos.webgl

@kotlin.internal.InlineOnly public inline fun WebGLContextAttributes(/*0*/ alpha: kotlin.Boolean? = ..., /*1*/ depth: kotlin.Boolean? = ..., /*2*/ stencil: kotlin.Boolean? = ..., /*3*/ antialias: kotlin.Boolean? = ..., /*4*/ premultipliedAlpha: kotlin.Boolean? = ..., /*5*/ preserveDrawingBuffer: kotlin.Boolean? = ..., /*6*/ preferLowPowerToHighPerformance: kotlin.Boolean? = ..., /*7*/ failIfMajorPerformanceCaveat: kotlin.Boolean? = ...): org.khronos.webgl.WebGLContextAttributes
@kotlin.internal.InlineOnly public inline fun WebGLContextEventInit(/*0*/ statusMessage: kotlin.String? = ..., /*1*/ bubbles: kotlin.Boolean? = ..., /*2*/ cancelable: kotlin.Boolean? = ..., /*3*/ composed: kotlin.Boolean? = ...): org.khronos.webgl.WebGLContextEventInit
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Float32Array.get(/*0*/ index: kotlin.Int): kotlin.Float
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Float64Array.get(/*0*/ index: kotlin.Int): kotlin.Double
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Int16Array.get(/*0*/ index: kotlin.Int): kotlin.Short
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Int32Array.get(/*0*/ index: kotlin.Int): kotlin.Int
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Int8Array.get(/*0*/ index: kotlin.Int): kotlin.Byte
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Uint16Array.get(/*0*/ index: kotlin.Int): kotlin.Short
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Uint32Array.get(/*0*/ index: kotlin.Int): kotlin.Int
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Uint8Array.get(/*0*/ index: kotlin.Int): kotlin.Byte
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Uint8ClampedArray.get(/*0*/ index: kotlin.Int): kotlin.Byte
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Float32Array.set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Float): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Float64Array.set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Double): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Int16Array.set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Short): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Int32Array.set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Int): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Int8Array.set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Byte): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Uint16Array.set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Short): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Uint32Array.set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Int): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Uint8Array.set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Byte): kotlin.Unit
@kotlin.internal.InlineOnly public inline operator fun org.khronos.webgl.Uint8ClampedArray.set(/*0*/ index: kotlin.Int, /*1*/ value: kotlin.Byte): kotlin.Unit

public open external class ArrayBuffer : org.khronos.webgl.BufferDataSource {
    /*primary*/ public constructor ArrayBuffer(/*0*/ length: kotlin.Int)
    public open val byteLength: kotlin.Int
        public open fun <get-byteLength>(): kotlin.Int
    public final fun slice(/*0*/ begin: kotlin.Int, /*1*/ end: kotlin.Int = ...): org.khronos.webgl.ArrayBuffer

    public companion object Companion {
        public final fun isView(/*0*/ value: kotlin.Any?): kotlin.Boolean
    }
}

public external interface ArrayBufferView : org.khronos.webgl.BufferDataSource {
    public abstract val buffer: org.khronos.webgl.ArrayBuffer
        public abstract fun <get-buffer>(): org.khronos.webgl.ArrayBuffer
    public abstract val byteLength: kotlin.Int
        public abstract fun <get-byteLength>(): kotlin.Int
    public abstract val byteOffset: kotlin.Int
        public abstract fun <get-byteOffset>(): kotlin.Int
}

public external interface BufferDataSource {
}

public open external class DataView : org.khronos.webgl.ArrayBufferView {
    /*primary*/ public constructor DataView(/*0*/ buffer: org.khronos.webgl.ArrayBuffer, /*1*/ byteOffset: kotlin.Int = ..., /*2*/ byteLength: kotlin.Int = ...)
    public open override /*1*/ val buffer: org.khronos.webgl.ArrayBuffer
        public open override /*1*/ fun <get-buffer>(): org.khronos.webgl.ArrayBuffer
    public open override /*1*/ val byteLength: kotlin.Int
        public open override /*1*/ fun <get-byteLength>(): kotlin.Int
    public open override /*1*/ val byteOffset: kotlin.Int
        public open override /*1*/ fun <get-byteOffset>(): kotlin.Int
    public final fun getFloat32(/*0*/ byteOffset: kotlin.Int, /*1*/ littleEndian: kotlin.Boolean = ...): kotlin.Float
    public final fun getFloat64(/*0*/ byteOffset: kotlin.Int, /*1*/ littleEndian: kotlin.Boolean = ...): kotlin.Double
    public final fun getInt16(/*0*/ byteOffset: kotlin.Int, /*1*/ littleEndian: kotlin.Boolean = ...): kotlin.Short
    public final fun getInt32(/*0*/ byteOffset: kotlin.Int, /*1*/ littleEndian: kotlin.Boolean = ...): kotlin.Int
    public final fun getInt8(/*0*/ byteOffset: kotlin.Int): kotlin.Byte
    public final fun getUint16(/*0*/ byteOffset: kotlin.Int, /*1*/ littleEndian: kotlin.Boolean = ...): kotlin.Short
    public final fun getUint32(/*0*/ byteOffset: kotlin.Int, /*1*/ littleEndian: kotlin.Boolean = ...): kotlin.Int
    public final fun getUint8(/*0*/ byteOffset: kotlin.Int): kotlin.Byte
    public final fun setFloat32(/*0*/ byteOffset: kotlin.Int, /*1*/ value: kotlin.Float, /*2*/ littleEndian: kotlin.Boolean = ...): kotlin.Unit
    public final fun setFloat64(/*0*/ byteOffset: kotlin.Int, /*1*/ value: kotlin.Double, /*2*/ littleEndian: kotlin.Boolean = ...): kotlin.Unit
    public final fun setInt16(/*0*/ byteOffset: kotlin.Int, /*1*/ value: kotlin.Short, /*2*/ littleEndian: kotlin.Boolean = ...): kotlin.Unit
    public final fun setInt32(/*0*/ byteOffset: kotlin.Int, /*1*/ value: kotlin.Int, /*2*/ littleEndian: kotlin.Boolean = ...): kotlin.Unit
    public final fun setInt8(/*0*/ byteOffset: kotlin.Int, /*1*/ value: kotlin.Byte): kotlin.Unit
    public final fun setUint16(/*0*/ byteOffset: kotlin.Int, /*1*/ value: kotlin.Short, /*2*/ littleEndian: kotlin.Boolean = ...): kotlin.Unit
    public final fun setUint32(/*0*/ byteOffset: kotlin.Int, /*1*/ value: kotlin.Int, /*2*/ littleEndian: kotlin.Boolean = ...): kotlin.Unit
    public final fun setUint8(/*0*/ byteOffset: kotlin.Int, /*1*/ value: kotlin.Byte): kotlin.Unit
}

public open external class Float32Array : org.khronos.webgl.ArrayBufferView {
    public constructor Float32Array(/*0*/ array: kotlin.Array<kotlin.Float>)
    public constructor Float32Array(/*0*/ length: kotlin.Int)
    public constructor Float32Array(/*0*/ buffer: org.khronos.webgl.ArrayBuffer, /*1*/ byteOffset: kotlin.Int = ..., /*2*/ length: kotlin.Int = ...)
    public constructor Float32Array(/*0*/ array: org.khronos.webgl.Float32Array)
    public open override /*1*/ val buffer: org.khronos.webgl.ArrayBuffer
        public open override /*1*/ fun <get-buffer>(): org.khronos.webgl.ArrayBuffer
    public open override /*1*/ val byteLength: kotlin.Int
        public open override /*1*/ fun <get-byteLength>(): kotlin.Int
    public open override /*1*/ val byteOffset: kotlin.Int
        public open override /*1*/ fun <get-byteOffset>(): kotlin.Int
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun set(/*0*/ array: kotlin.Array<kotlin.Float>, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun set(/*0*/ array: org.khronos.webgl.Float32Array, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun subarray(/*0*/ start: kotlin.Int, /*1*/ end: kotlin.Int): org.khronos.webgl.Float32Array

    public companion object Companion {
        public final val BYTES_PER_ELEMENT: kotlin.Int
            public final fun <get-BYTES_PER_ELEMENT>(): kotlin.Int
    }
}

public open external class Float64Array : org.khronos.webgl.ArrayBufferView {
    public constructor Float64Array(/*0*/ array: kotlin.Array<kotlin.Double>)
    public constructor Float64Array(/*0*/ length: kotlin.Int)
    public constructor Float64Array(/*0*/ buffer: org.khronos.webgl.ArrayBuffer, /*1*/ byteOffset: kotlin.Int = ..., /*2*/ length: kotlin.Int = ...)
    public constructor Float64Array(/*0*/ array: org.khronos.webgl.Float64Array)
    public open override /*1*/ val buffer: org.khronos.webgl.ArrayBuffer
        public open override /*1*/ fun <get-buffer>(): org.khronos.webgl.ArrayBuffer
    public open override /*1*/ val byteLength: kotlin.Int
        public open override /*1*/ fun <get-byteLength>(): kotlin.Int
    public open override /*1*/ val byteOffset: kotlin.Int
        public open override /*1*/ fun <get-byteOffset>(): kotlin.Int
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun set(/*0*/ array: kotlin.Array<kotlin.Double>, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun set(/*0*/ array: org.khronos.webgl.Float64Array, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun subarray(/*0*/ start: kotlin.Int, /*1*/ end: kotlin.Int): org.khronos.webgl.Float64Array

    public companion object Companion {
        public final val BYTES_PER_ELEMENT: kotlin.Int
            public final fun <get-BYTES_PER_ELEMENT>(): kotlin.Int
    }
}

public open external class Int16Array : org.khronos.webgl.ArrayBufferView {
    public constructor Int16Array(/*0*/ array: kotlin.Array<kotlin.Short>)
    public constructor Int16Array(/*0*/ length: kotlin.Int)
    public constructor Int16Array(/*0*/ buffer: org.khronos.webgl.ArrayBuffer, /*1*/ byteOffset: kotlin.Int = ..., /*2*/ length: kotlin.Int = ...)
    public constructor Int16Array(/*0*/ array: org.khronos.webgl.Int16Array)
    public open override /*1*/ val buffer: org.khronos.webgl.ArrayBuffer
        public open override /*1*/ fun <get-buffer>(): org.khronos.webgl.ArrayBuffer
    public open override /*1*/ val byteLength: kotlin.Int
        public open override /*1*/ fun <get-byteLength>(): kotlin.Int
    public open override /*1*/ val byteOffset: kotlin.Int
        public open override /*1*/ fun <get-byteOffset>(): kotlin.Int
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun set(/*0*/ array: kotlin.Array<kotlin.Short>, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun set(/*0*/ array: org.khronos.webgl.Int16Array, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun subarray(/*0*/ start: kotlin.Int, /*1*/ end: kotlin.Int): org.khronos.webgl.Int16Array

    public companion object Companion {
        public final val BYTES_PER_ELEMENT: kotlin.Int
            public final fun <get-BYTES_PER_ELEMENT>(): kotlin.Int
    }
}

public open external class Int32Array : org.khronos.webgl.ArrayBufferView {
    public constructor Int32Array(/*0*/ array: kotlin.Array<kotlin.Int>)
    public constructor Int32Array(/*0*/ length: kotlin.Int)
    public constructor Int32Array(/*0*/ buffer: org.khronos.webgl.ArrayBuffer, /*1*/ byteOffset: kotlin.Int = ..., /*2*/ length: kotlin.Int = ...)
    public constructor Int32Array(/*0*/ array: org.khronos.webgl.Int32Array)
    public open override /*1*/ val buffer: org.khronos.webgl.ArrayBuffer
        public open override /*1*/ fun <get-buffer>(): org.khronos.webgl.ArrayBuffer
    public open override /*1*/ val byteLength: kotlin.Int
        public open override /*1*/ fun <get-byteLength>(): kotlin.Int
    public open override /*1*/ val byteOffset: kotlin.Int
        public open override /*1*/ fun <get-byteOffset>(): kotlin.Int
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun set(/*0*/ array: kotlin.Array<kotlin.Int>, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun set(/*0*/ array: org.khronos.webgl.Int32Array, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun subarray(/*0*/ start: kotlin.Int, /*1*/ end: kotlin.Int): org.khronos.webgl.Int32Array

    public companion object Companion {
        public final val BYTES_PER_ELEMENT: kotlin.Int
            public final fun <get-BYTES_PER_ELEMENT>(): kotlin.Int
    }
}

public open external class Int8Array : org.khronos.webgl.ArrayBufferView {
    public constructor Int8Array(/*0*/ array: kotlin.Array<kotlin.Byte>)
    public constructor Int8Array(/*0*/ length: kotlin.Int)
    public constructor Int8Array(/*0*/ buffer: org.khronos.webgl.ArrayBuffer, /*1*/ byteOffset: kotlin.Int = ..., /*2*/ length: kotlin.Int = ...)
    public constructor Int8Array(/*0*/ array: org.khronos.webgl.Int8Array)
    public open override /*1*/ val buffer: org.khronos.webgl.ArrayBuffer
        public open override /*1*/ fun <get-buffer>(): org.khronos.webgl.ArrayBuffer
    public open override /*1*/ val byteLength: kotlin.Int
        public open override /*1*/ fun <get-byteLength>(): kotlin.Int
    public open override /*1*/ val byteOffset: kotlin.Int
        public open override /*1*/ fun <get-byteOffset>(): kotlin.Int
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun set(/*0*/ array: kotlin.Array<kotlin.Byte>, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun set(/*0*/ array: org.khronos.webgl.Int8Array, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun subarray(/*0*/ start: kotlin.Int, /*1*/ end: kotlin.Int): org.khronos.webgl.Int8Array

    public companion object Companion {
        public final val BYTES_PER_ELEMENT: kotlin.Int
            public final fun <get-BYTES_PER_ELEMENT>(): kotlin.Int
    }
}

public external interface TexImageSource {
}

public open external class Uint16Array : org.khronos.webgl.ArrayBufferView {
    public constructor Uint16Array(/*0*/ array: kotlin.Array<kotlin.Short>)
    public constructor Uint16Array(/*0*/ length: kotlin.Int)
    public constructor Uint16Array(/*0*/ buffer: org.khronos.webgl.ArrayBuffer, /*1*/ byteOffset: kotlin.Int = ..., /*2*/ length: kotlin.Int = ...)
    public constructor Uint16Array(/*0*/ array: org.khronos.webgl.Uint16Array)
    public open override /*1*/ val buffer: org.khronos.webgl.ArrayBuffer
        public open override /*1*/ fun <get-buffer>(): org.khronos.webgl.ArrayBuffer
    public open override /*1*/ val byteLength: kotlin.Int
        public open override /*1*/ fun <get-byteLength>(): kotlin.Int
    public open override /*1*/ val byteOffset: kotlin.Int
        public open override /*1*/ fun <get-byteOffset>(): kotlin.Int
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun set(/*0*/ array: kotlin.Array<kotlin.Short>, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun set(/*0*/ array: org.khronos.webgl.Uint16Array, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun subarray(/*0*/ start: kotlin.Int, /*1*/ end: kotlin.Int): org.khronos.webgl.Uint16Array

    public companion object Companion {
        public final val BYTES_PER_ELEMENT: kotlin.Int
            public final fun <get-BYTES_PER_ELEMENT>(): kotlin.Int
    }
}

public open external class Uint32Array : org.khronos.webgl.ArrayBufferView {
    public constructor Uint32Array(/*0*/ array: kotlin.Array<kotlin.Int>)
    public constructor Uint32Array(/*0*/ length: kotlin.Int)
    public constructor Uint32Array(/*0*/ buffer: org.khronos.webgl.ArrayBuffer, /*1*/ byteOffset: kotlin.Int = ..., /*2*/ length: kotlin.Int = ...)
    public constructor Uint32Array(/*0*/ array: org.khronos.webgl.Uint32Array)
    public open override /*1*/ val buffer: org.khronos.webgl.ArrayBuffer
        public open override /*1*/ fun <get-buffer>(): org.khronos.webgl.ArrayBuffer
    public open override /*1*/ val byteLength: kotlin.Int
        public open override /*1*/ fun <get-byteLength>(): kotlin.Int
    public open override /*1*/ val byteOffset: kotlin.Int
        public open override /*1*/ fun <get-byteOffset>(): kotlin.Int
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun set(/*0*/ array: kotlin.Array<kotlin.Int>, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun set(/*0*/ array: org.khronos.webgl.Uint32Array, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun subarray(/*0*/ start: kotlin.Int, /*1*/ end: kotlin.Int): org.khronos.webgl.Uint32Array

    public companion object Companion {
        public final val BYTES_PER_ELEMENT: kotlin.Int
            public final fun <get-BYTES_PER_ELEMENT>(): kotlin.Int
    }
}

public open external class Uint8Array : org.khronos.webgl.ArrayBufferView {
    public constructor Uint8Array(/*0*/ array: kotlin.Array<kotlin.Byte>)
    public constructor Uint8Array(/*0*/ length: kotlin.Int)
    public constructor Uint8Array(/*0*/ buffer: org.khronos.webgl.ArrayBuffer, /*1*/ byteOffset: kotlin.Int = ..., /*2*/ length: kotlin.Int = ...)
    public constructor Uint8Array(/*0*/ array: org.khronos.webgl.Uint8Array)
    public open override /*1*/ val buffer: org.khronos.webgl.ArrayBuffer
        public open override /*1*/ fun <get-buffer>(): org.khronos.webgl.ArrayBuffer
    public open override /*1*/ val byteLength: kotlin.Int
        public open override /*1*/ fun <get-byteLength>(): kotlin.Int
    public open override /*1*/ val byteOffset: kotlin.Int
        public open override /*1*/ fun <get-byteOffset>(): kotlin.Int
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun set(/*0*/ array: kotlin.Array<kotlin.Byte>, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun set(/*0*/ array: org.khronos.webgl.Uint8Array, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun subarray(/*0*/ start: kotlin.Int, /*1*/ end: kotlin.Int): org.khronos.webgl.Uint8Array

    public companion object Companion {
        public final val BYTES_PER_ELEMENT: kotlin.Int
            public final fun <get-BYTES_PER_ELEMENT>(): kotlin.Int
    }
}

public open external class Uint8ClampedArray : org.khronos.webgl.ArrayBufferView {
    public constructor Uint8ClampedArray(/*0*/ array: kotlin.Array<kotlin.Byte>)
    public constructor Uint8ClampedArray(/*0*/ length: kotlin.Int)
    public constructor Uint8ClampedArray(/*0*/ buffer: org.khronos.webgl.ArrayBuffer, /*1*/ byteOffset: kotlin.Int = ..., /*2*/ length: kotlin.Int = ...)
    public constructor Uint8ClampedArray(/*0*/ array: org.khronos.webgl.Uint8ClampedArray)
    public open override /*1*/ val buffer: org.khronos.webgl.ArrayBuffer
        public open override /*1*/ fun <get-buffer>(): org.khronos.webgl.ArrayBuffer
    public open override /*1*/ val byteLength: kotlin.Int
        public open override /*1*/ fun <get-byteLength>(): kotlin.Int
    public open override /*1*/ val byteOffset: kotlin.Int
        public open override /*1*/ fun <get-byteOffset>(): kotlin.Int
    public open val length: kotlin.Int
        public open fun <get-length>(): kotlin.Int
    public final fun set(/*0*/ array: kotlin.Array<kotlin.Byte>, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun set(/*0*/ array: org.khronos.webgl.Uint8ClampedArray, /*1*/ offset: kotlin.Int = ...): kotlin.Unit
    public final fun subarray(/*0*/ start: kotlin.Int, /*1*/ end: kotlin.Int): org.khronos.webgl.Uint8ClampedArray

    public companion object Companion {
        public final val BYTES_PER_ELEMENT: kotlin.Int
            public final fun <get-BYTES_PER_ELEMENT>(): kotlin.Int
    }
}

public abstract external class WebGLActiveInfo {
    /*primary*/ public constructor WebGLActiveInfo()
    public open val name: kotlin.String
        public open fun <get-name>(): kotlin.String
    public open val size: kotlin.Int
        public open fun <get-size>(): kotlin.Int
    public open val type: kotlin.Int
        public open fun <get-type>(): kotlin.Int
}

public abstract external class WebGLBuffer : org.khronos.webgl.WebGLObject {
    /*primary*/ public constructor WebGLBuffer()
}

public external interface WebGLContextAttributes {
    public open var alpha: kotlin.Boolean?
        public open fun <get-alpha>(): kotlin.Boolean?
        public open fun <set-alpha>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var antialias: kotlin.Boolean?
        public open fun <get-antialias>(): kotlin.Boolean?
        public open fun <set-antialias>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var depth: kotlin.Boolean?
        public open fun <get-depth>(): kotlin.Boolean?
        public open fun <set-depth>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var failIfMajorPerformanceCaveat: kotlin.Boolean?
        public open fun <get-failIfMajorPerformanceCaveat>(): kotlin.Boolean?
        public open fun <set-failIfMajorPerformanceCaveat>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var preferLowPowerToHighPerformance: kotlin.Boolean?
        public open fun <get-preferLowPowerToHighPerformance>(): kotlin.Boolean?
        public open fun <set-preferLowPowerToHighPerformance>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var premultipliedAlpha: kotlin.Boolean?
        public open fun <get-premultipliedAlpha>(): kotlin.Boolean?
        public open fun <set-premultipliedAlpha>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var preserveDrawingBuffer: kotlin.Boolean?
        public open fun <get-preserveDrawingBuffer>(): kotlin.Boolean?
        public open fun <set-preserveDrawingBuffer>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
    public open var stencil: kotlin.Boolean?
        public open fun <get-stencil>(): kotlin.Boolean?
        public open fun <set-stencil>(/*0*/ value: kotlin.Boolean?): kotlin.Unit
}

public open external class WebGLContextEvent : org.w3c.dom.events.Event {
    /*primary*/ public constructor WebGLContextEvent(/*0*/ type: kotlin.String, /*1*/ eventInit: org.khronos.webgl.WebGLContextEventInit = ...)
    public open val statusMessage: kotlin.String
        public open fun <get-statusMessage>(): kotlin.String

    public companion object Companion {
        public final val AT_TARGET: kotlin.Short
            public final fun <get-AT_TARGET>(): kotlin.Short
        public final val BUBBLING_PHASE: kotlin.Short
            public final fun <get-BUBBLING_PHASE>(): kotlin.Short
        public final val CAPTURING_PHASE: kotlin.Short
            public final fun <get-CAPTURING_PHASE>(): kotlin.Short
        public final val NONE: kotlin.Short
            public final fun <get-NONE>(): kotlin.Short
    }
}

public external interface WebGLContextEventInit : org.w3c.dom.EventInit {
    public open var statusMessage: kotlin.String?
        public open fun <get-statusMessage>(): kotlin.String?
        public open fun <set-statusMessage>(/*0*/ value: kotlin.String?): kotlin.Unit
}

public abstract external class WebGLFramebuffer : org.khronos.webgl.WebGLObject {
    /*primary*/ public constructor WebGLFramebuffer()
}

public abstract external class WebGLObject {
    /*primary*/ public constructor WebGLObject()
}

public abstract external class WebGLProgram : org.khronos.webgl.WebGLObject {
    /*primary*/ public constructor WebGLProgram()
}

public abstract external class WebGLRenderbuffer : org.khronos.webgl.WebGLObject {
    /*primary*/ public constructor WebGLRenderbuffer()
}

public abstract external class WebGLRenderingContext : org.khronos.webgl.WebGLRenderingContextBase, org.w3c.dom.RenderingContext {
    /*primary*/ public constructor WebGLRenderingContext()

    public companion object Companion {
        public final val ACTIVE_ATTRIBUTES: kotlin.Int
            public final fun <get-ACTIVE_ATTRIBUTES>(): kotlin.Int
        public final val ACTIVE_TEXTURE: kotlin.Int
            public final fun <get-ACTIVE_TEXTURE>(): kotlin.Int
        public final val ACTIVE_UNIFORMS: kotlin.Int
            public final fun <get-ACTIVE_UNIFORMS>(): kotlin.Int
        public final val ALIASED_LINE_WIDTH_RANGE: kotlin.Int
            public final fun <get-ALIASED_LINE_WIDTH_RANGE>(): kotlin.Int
        public final val ALIASED_POINT_SIZE_RANGE: kotlin.Int
            public final fun <get-ALIASED_POINT_SIZE_RANGE>(): kotlin.Int
        public final val ALPHA: kotlin.Int
            public final fun <get-ALPHA>(): kotlin.Int
        public final val ALPHA_BITS: kotlin.Int
            public final fun <get-ALPHA_BITS>(): kotlin.Int
        public final val ALWAYS: kotlin.Int
            public final fun <get-ALWAYS>(): kotlin.Int
        public final val ARRAY_BUFFER: kotlin.Int
            public final fun <get-ARRAY_BUFFER>(): kotlin.Int
        public final val ARRAY_BUFFER_BINDING: kotlin.Int
            public final fun <get-ARRAY_BUFFER_BINDING>(): kotlin.Int
        public final val ATTACHED_SHADERS: kotlin.Int
            public final fun <get-ATTACHED_SHADERS>(): kotlin.Int
        public final val BACK: kotlin.Int
            public final fun <get-BACK>(): kotlin.Int
        public final val BLEND: kotlin.Int
            public final fun <get-BLEND>(): kotlin.Int
        public final val BLEND_COLOR: kotlin.Int
            public final fun <get-BLEND_COLOR>(): kotlin.Int
        public final val BLEND_DST_ALPHA: kotlin.Int
            public final fun <get-BLEND_DST_ALPHA>(): kotlin.Int
        public final val BLEND_DST_RGB: kotlin.Int
            public final fun <get-BLEND_DST_RGB>(): kotlin.Int
        public final val BLEND_EQUATION: kotlin.Int
            public final fun <get-BLEND_EQUATION>(): kotlin.Int
        public final val BLEND_EQUATION_ALPHA: kotlin.Int
            public final fun <get-BLEND_EQUATION_ALPHA>(): kotlin.Int
        public final val BLEND_EQUATION_RGB: kotlin.Int
            public final fun <get-BLEND_EQUATION_RGB>(): kotlin.Int
        public final val BLEND_SRC_ALPHA: kotlin.Int
            public final fun <get-BLEND_SRC_ALPHA>(): kotlin.Int
        public final val BLEND_SRC_RGB: kotlin.Int
            public final fun <get-BLEND_SRC_RGB>(): kotlin.Int
        public final val BLUE_BITS: kotlin.Int
            public final fun <get-BLUE_BITS>(): kotlin.Int
        public final val BOOL: kotlin.Int
            public final fun <get-BOOL>(): kotlin.Int
        public final val BOOL_VEC2: kotlin.Int
            public final fun <get-BOOL_VEC2>(): kotlin.Int
        public final val BOOL_VEC3: kotlin.Int
            public final fun <get-BOOL_VEC3>(): kotlin.Int
        public final val BOOL_VEC4: kotlin.Int
            public final fun <get-BOOL_VEC4>(): kotlin.Int
        public final val BROWSER_DEFAULT_WEBGL: kotlin.Int
            public final fun <get-BROWSER_DEFAULT_WEBGL>(): kotlin.Int
        public final val BUFFER_SIZE: kotlin.Int
            public final fun <get-BUFFER_SIZE>(): kotlin.Int
        public final val BUFFER_USAGE: kotlin.Int
            public final fun <get-BUFFER_USAGE>(): kotlin.Int
        public final val BYTE: kotlin.Int
            public final fun <get-BYTE>(): kotlin.Int
        public final val CCW: kotlin.Int
            public final fun <get-CCW>(): kotlin.Int
        public final val CLAMP_TO_EDGE: kotlin.Int
            public final fun <get-CLAMP_TO_EDGE>(): kotlin.Int
        public final val COLOR_ATTACHMENT0: kotlin.Int
            public final fun <get-COLOR_ATTACHMENT0>(): kotlin.Int
        public final val COLOR_BUFFER_BIT: kotlin.Int
            public final fun <get-COLOR_BUFFER_BIT>(): kotlin.Int
        public final val COLOR_CLEAR_VALUE: kotlin.Int
            public final fun <get-COLOR_CLEAR_VALUE>(): kotlin.Int
        public final val COLOR_WRITEMASK: kotlin.Int
            public final fun <get-COLOR_WRITEMASK>(): kotlin.Int
        public final val COMPILE_STATUS: kotlin.Int
            public final fun <get-COMPILE_STATUS>(): kotlin.Int
        public final val COMPRESSED_TEXTURE_FORMATS: kotlin.Int
            public final fun <get-COMPRESSED_TEXTURE_FORMATS>(): kotlin.Int
        public final val CONSTANT_ALPHA: kotlin.Int
            public final fun <get-CONSTANT_ALPHA>(): kotlin.Int
        public final val CONSTANT_COLOR: kotlin.Int
            public final fun <get-CONSTANT_COLOR>(): kotlin.Int
        public final val CONTEXT_LOST_WEBGL: kotlin.Int
            public final fun <get-CONTEXT_LOST_WEBGL>(): kotlin.Int
        public final val CULL_FACE: kotlin.Int
            public final fun <get-CULL_FACE>(): kotlin.Int
        public final val CULL_FACE_MODE: kotlin.Int
            public final fun <get-CULL_FACE_MODE>(): kotlin.Int
        public final val CURRENT_PROGRAM: kotlin.Int
            public final fun <get-CURRENT_PROGRAM>(): kotlin.Int
        public final val CURRENT_VERTEX_ATTRIB: kotlin.Int
            public final fun <get-CURRENT_VERTEX_ATTRIB>(): kotlin.Int
        public final val CW: kotlin.Int
            public final fun <get-CW>(): kotlin.Int
        public final val DECR: kotlin.Int
            public final fun <get-DECR>(): kotlin.Int
        public final val DECR_WRAP: kotlin.Int
            public final fun <get-DECR_WRAP>(): kotlin.Int
        public final val DELETE_STATUS: kotlin.Int
            public final fun <get-DELETE_STATUS>(): kotlin.Int
        public final val DEPTH_ATTACHMENT: kotlin.Int
            public final fun <get-DEPTH_ATTACHMENT>(): kotlin.Int
        public final val DEPTH_BITS: kotlin.Int
            public final fun <get-DEPTH_BITS>(): kotlin.Int
        public final val DEPTH_BUFFER_BIT: kotlin.Int
            public final fun <get-DEPTH_BUFFER_BIT>(): kotlin.Int
        public final val DEPTH_CLEAR_VALUE: kotlin.Int
            public final fun <get-DEPTH_CLEAR_VALUE>(): kotlin.Int
        public final val DEPTH_COMPONENT: kotlin.Int
            public final fun <get-DEPTH_COMPONENT>(): kotlin.Int
        public final val DEPTH_COMPONENT16: kotlin.Int
            public final fun <get-DEPTH_COMPONENT16>(): kotlin.Int
        public final val DEPTH_FUNC: kotlin.Int
            public final fun <get-DEPTH_FUNC>(): kotlin.Int
        public final val DEPTH_RANGE: kotlin.Int
            public final fun <get-DEPTH_RANGE>(): kotlin.Int
        public final val DEPTH_STENCIL: kotlin.Int
            public final fun <get-DEPTH_STENCIL>(): kotlin.Int
        public final val DEPTH_STENCIL_ATTACHMENT: kotlin.Int
            public final fun <get-DEPTH_STENCIL_ATTACHMENT>(): kotlin.Int
        public final val DEPTH_TEST: kotlin.Int
            public final fun <get-DEPTH_TEST>(): kotlin.Int
        public final val DEPTH_WRITEMASK: kotlin.Int
            public final fun <get-DEPTH_WRITEMASK>(): kotlin.Int
        public final val DITHER: kotlin.Int
            public final fun <get-DITHER>(): kotlin.Int
        public final val DONT_CARE: kotlin.Int
            public final fun <get-DONT_CARE>(): kotlin.Int
        public final val DST_ALPHA: kotlin.Int
            public final fun <get-DST_ALPHA>(): kotlin.Int
        public final val DST_COLOR: kotlin.Int
            public final fun <get-DST_COLOR>(): kotlin.Int
        public final val DYNAMIC_DRAW: kotlin.Int
            public final fun <get-DYNAMIC_DRAW>(): kotlin.Int
        public final val ELEMENT_ARRAY_BUFFER: kotlin.Int
            public final fun <get-ELEMENT_ARRAY_BUFFER>(): kotlin.Int
        public final val ELEMENT_ARRAY_BUFFER_BINDING: kotlin.Int
            public final fun <get-ELEMENT_ARRAY_BUFFER_BINDING>(): kotlin.Int
        public final val EQUAL: kotlin.Int
            public final fun <get-EQUAL>(): kotlin.Int
        public final val FASTEST: kotlin.Int
            public final fun <get-FASTEST>(): kotlin.Int
        public final val FLOAT: kotlin.Int
            public final fun <get-FLOAT>(): kotlin.Int
        public final val FLOAT_MAT2: kotlin.Int
            public final fun <get-FLOAT_MAT2>(): kotlin.Int
        public final val FLOAT_MAT3: kotlin.Int
            public final fun <get-FLOAT_MAT3>(): kotlin.Int
        public final val FLOAT_MAT4: kotlin.Int
            public final fun <get-FLOAT_MAT4>(): kotlin.Int
        public final val FLOAT_VEC2: kotlin.Int
            public final fun <get-FLOAT_VEC2>(): kotlin.Int
        public final val FLOAT_VEC3: kotlin.Int
            public final fun <get-FLOAT_VEC3>(): kotlin.Int
        public final val FLOAT_VEC4: kotlin.Int
            public final fun <get-FLOAT_VEC4>(): kotlin.Int
        public final val FRAGMENT_SHADER: kotlin.Int
            public final fun <get-FRAGMENT_SHADER>(): kotlin.Int
        public final val FRAMEBUFFER: kotlin.Int
            public final fun <get-FRAMEBUFFER>(): kotlin.Int
        public final val FRAMEBUFFER_ATTACHMENT_OBJECT_NAME: kotlin.Int
            public final fun <get-FRAMEBUFFER_ATTACHMENT_OBJECT_NAME>(): kotlin.Int
        public final val FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE: kotlin.Int
            public final fun <get-FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE>(): kotlin.Int
        public final val FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE: kotlin.Int
            public final fun <get-FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE>(): kotlin.Int
        public final val FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL: kotlin.Int
            public final fun <get-FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL>(): kotlin.Int
        public final val FRAMEBUFFER_BINDING: kotlin.Int
            public final fun <get-FRAMEBUFFER_BINDING>(): kotlin.Int
        public final val FRAMEBUFFER_COMPLETE: kotlin.Int
            public final fun <get-FRAMEBUFFER_COMPLETE>(): kotlin.Int
        public final val FRAMEBUFFER_INCOMPLETE_ATTACHMENT: kotlin.Int
            public final fun <get-FRAMEBUFFER_INCOMPLETE_ATTACHMENT>(): kotlin.Int
        public final val FRAMEBUFFER_INCOMPLETE_DIMENSIONS: kotlin.Int
            public final fun <get-FRAMEBUFFER_INCOMPLETE_DIMENSIONS>(): kotlin.Int
        public final val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: kotlin.Int
            public final fun <get-FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT>(): kotlin.Int
        public final val FRAMEBUFFER_UNSUPPORTED: kotlin.Int
            public final fun <get-FRAMEBUFFER_UNSUPPORTED>(): kotlin.Int
        public final val FRONT: kotlin.Int
            public final fun <get-FRONT>(): kotlin.Int
        public final val FRONT_AND_BACK: kotlin.Int
            public final fun <get-FRONT_AND_BACK>(): kotlin.Int
        public final val FRONT_FACE: kotlin.Int
            public final fun <get-FRONT_FACE>(): kotlin.Int
        public final val FUNC_ADD: kotlin.Int
            public final fun <get-FUNC_ADD>(): kotlin.Int
        public final val FUNC_REVERSE_SUBTRACT: kotlin.Int
            public final fun <get-FUNC_REVERSE_SUBTRACT>(): kotlin.Int
        public final val FUNC_SUBTRACT: kotlin.Int
            public final fun <get-FUNC_SUBTRACT>(): kotlin.Int
        public final val GENERATE_MIPMAP_HINT: kotlin.Int
            public final fun <get-GENERATE_MIPMAP_HINT>(): kotlin.Int
        public final val GEQUAL: kotlin.Int
            public final fun <get-GEQUAL>(): kotlin.Int
        public final val GREATER: kotlin.Int
            public final fun <get-GREATER>(): kotlin.Int
        public final val GREEN_BITS: kotlin.Int
            public final fun <get-GREEN_BITS>(): kotlin.Int
        public final val HIGH_FLOAT: kotlin.Int
            public final fun <get-HIGH_FLOAT>(): kotlin.Int
        public final val HIGH_INT: kotlin.Int
            public final fun <get-HIGH_INT>(): kotlin.Int
        public final val IMPLEMENTATION_COLOR_READ_FORMAT: kotlin.Int
            public final fun <get-IMPLEMENTATION_COLOR_READ_FORMAT>(): kotlin.Int
        public final val IMPLEMENTATION_COLOR_READ_TYPE: kotlin.Int
            public final fun <get-IMPLEMENTATION_COLOR_READ_TYPE>(): kotlin.Int
        public final val INCR: kotlin.Int
            public final fun <get-INCR>(): kotlin.Int
        public final val INCR_WRAP: kotlin.Int
            public final fun <get-INCR_WRAP>(): kotlin.Int
        public final val INT: kotlin.Int
            public final fun <get-INT>(): kotlin.Int
        public final val INT_VEC2: kotlin.Int
            public final fun <get-INT_VEC2>(): kotlin.Int
        public final val INT_VEC3: kotlin.Int
            public final fun <get-INT_VEC3>(): kotlin.Int
        public final val INT_VEC4: kotlin.Int
            public final fun <get-INT_VEC4>(): kotlin.Int
        public final val INVALID_ENUM: kotlin.Int
            public final fun <get-INVALID_ENUM>(): kotlin.Int
        public final val INVALID_FRAMEBUFFER_OPERATION: kotlin.Int
            public final fun <get-INVALID_FRAMEBUFFER_OPERATION>(): kotlin.Int
        public final val INVALID_OPERATION: kotlin.Int
            public final fun <get-INVALID_OPERATION>(): kotlin.Int
        public final val INVALID_VALUE: kotlin.Int
            public final fun <get-INVALID_VALUE>(): kotlin.Int
        public final val INVERT: kotlin.Int
            public final fun <get-INVERT>(): kotlin.Int
        public final val KEEP: kotlin.Int
            public final fun <get-KEEP>(): kotlin.Int
        public final val LEQUAL: kotlin.Int
            public final fun <get-LEQUAL>(): kotlin.Int
        public final val LESS: kotlin.Int
            public final fun <get-LESS>(): kotlin.Int
        public final val LINEAR: kotlin.Int
            public final fun <get-LINEAR>(): kotlin.Int
        public final val LINEAR_MIPMAP_LINEAR: kotlin.Int
            public final fun <get-LINEAR_MIPMAP_LINEAR>(): kotlin.Int
        public final val LINEAR_MIPMAP_NEAREST: kotlin.Int
            public final fun <get-LINEAR_MIPMAP_NEAREST>(): kotlin.Int
        public final val LINES: kotlin.Int
            public final fun <get-LINES>(): kotlin.Int
        public final val LINE_LOOP: kotlin.Int
            public final fun <get-LINE_LOOP>(): kotlin.Int
        public final val LINE_STRIP: kotlin.Int
            public final fun <get-LINE_STRIP>(): kotlin.Int
        public final val LINE_WIDTH: kotlin.Int
            public final fun <get-LINE_WIDTH>(): kotlin.Int
        public final val LINK_STATUS: kotlin.Int
            public final fun <get-LINK_STATUS>(): kotlin.Int
        public final val LOW_FLOAT: kotlin.Int
            public final fun <get-LOW_FLOAT>(): kotlin.Int
        public final val LOW_INT: kotlin.Int
            public final fun <get-LOW_INT>(): kotlin.Int
        public final val LUMINANCE: kotlin.Int
            public final fun <get-LUMINANCE>(): kotlin.Int
        public final val LUMINANCE_ALPHA: kotlin.Int
            public final fun <get-LUMINANCE_ALPHA>(): kotlin.Int
        public final val MAX_COMBINED_TEXTURE_IMAGE_UNITS: kotlin.Int
            public final fun <get-MAX_COMBINED_TEXTURE_IMAGE_UNITS>(): kotlin.Int
        public final val MAX_CUBE_MAP_TEXTURE_SIZE: kotlin.Int
            public final fun <get-MAX_CUBE_MAP_TEXTURE_SIZE>(): kotlin.Int
        public final val MAX_FRAGMENT_UNIFORM_VECTORS: kotlin.Int
            public final fun <get-MAX_FRAGMENT_UNIFORM_VECTORS>(): kotlin.Int
        public final val MAX_RENDERBUFFER_SIZE: kotlin.Int
            public final fun <get-MAX_RENDERBUFFER_SIZE>(): kotlin.Int
        public final val MAX_TEXTURE_IMAGE_UNITS: kotlin.Int
            public final fun <get-MAX_TEXTURE_IMAGE_UNITS>(): kotlin.Int
        public final val MAX_TEXTURE_SIZE: kotlin.Int
            public final fun <get-MAX_TEXTURE_SIZE>(): kotlin.Int
        public final val MAX_VARYING_VECTORS: kotlin.Int
            public final fun <get-MAX_VARYING_VECTORS>(): kotlin.Int
        public final val MAX_VERTEX_ATTRIBS: kotlin.Int
            public final fun <get-MAX_VERTEX_ATTRIBS>(): kotlin.Int
        public final val MAX_VERTEX_TEXTURE_IMAGE_UNITS: kotlin.Int
            public final fun <get-MAX_VERTEX_TEXTURE_IMAGE_UNITS>(): kotlin.Int
        public final val MAX_VERTEX_UNIFORM_VECTORS: kotlin.Int
            public final fun <get-MAX_VERTEX_UNIFORM_VECTORS>(): kotlin.Int
        public final val MAX_VIEWPORT_DIMS: kotlin.Int
            public final fun <get-MAX_VIEWPORT_DIMS>(): kotlin.Int
        public final val MEDIUM_FLOAT: kotlin.Int
            public final fun <get-MEDIUM_FLOAT>(): kotlin.Int
        public final val MEDIUM_INT: kotlin.Int
            public final fun <get-MEDIUM_INT>(): kotlin.Int
        public final val MIRRORED_REPEAT: kotlin.Int
            public final fun <get-MIRRORED_REPEAT>(): kotlin.Int
        public final val NEAREST: kotlin.Int
            public final fun <get-NEAREST>(): kotlin.Int
        public final val NEAREST_MIPMAP_LINEAR: kotlin.Int
            public final fun <get-NEAREST_MIPMAP_LINEAR>(): kotlin.Int
        public final val NEAREST_MIPMAP_NEAREST: kotlin.Int
            public final fun <get-NEAREST_MIPMAP_NEAREST>(): kotlin.Int
        public final val NEVER: kotlin.Int
            public final fun <get-NEVER>(): kotlin.Int
        public final val NICEST: kotlin.Int
            public final fun <get-NICEST>(): kotlin.Int
        public final val NONE: kotlin.Int
            public final fun <get-NONE>(): kotlin.Int
        public final val NOTEQUAL: kotlin.Int
            public final fun <get-NOTEQUAL>(): kotlin.Int
        public final val NO_ERROR: kotlin.Int
            public final fun <get-NO_ERROR>(): kotlin.Int
        public final val ONE: kotlin.Int
            public final fun <get-ONE>(): kotlin.Int
        public final val ONE_MINUS_CONSTANT_ALPHA: kotlin.Int
            public final fun <get-ONE_MINUS_CONSTANT_ALPHA>(): kotlin.Int
        public final val ONE_MINUS_CONSTANT_COLOR: kotlin.Int
            public final fun <get-ONE_MINUS_CONSTANT_COLOR>(): kotlin.Int
        public final val ONE_MINUS_DST_ALPHA: kotlin.Int
            public final fun <get-ONE_MINUS_DST_ALPHA>(): kotlin.Int
        public final val ONE_MINUS_DST_COLOR: kotlin.Int
            public final fun <get-ONE_MINUS_DST_COLOR>(): kotlin.Int
        public final val ONE_MINUS_SRC_ALPHA: kotlin.Int
            public final fun <get-ONE_MINUS_SRC_ALPHA>(): kotlin.Int
        public final val ONE_MINUS_SRC_COLOR: kotlin.Int
            public final fun <get-ONE_MINUS_SRC_COLOR>(): kotlin.Int
        public final val OUT_OF_MEMORY: kotlin.Int
            public final fun <get-OUT_OF_MEMORY>(): kotlin.Int
        public final val PACK_ALIGNMENT: kotlin.Int
            public final fun <get-PACK_ALIGNMENT>(): kotlin.Int
        public final val POINTS: kotlin.Int
            public final fun <get-POINTS>(): kotlin.Int
        public final val POLYGON_OFFSET_FACTOR: kotlin.Int
            public final fun <get-POLYGON_OFFSET_FACTOR>(): kotlin.Int
        public final val POLYGON_OFFSET_FILL: kotlin.Int
            public final fun <get-POLYGON_OFFSET_FILL>(): kotlin.Int
        public final val POLYGON_OFFSET_UNITS: kotlin.Int
            public final fun <get-POLYGON_OFFSET_UNITS>(): kotlin.Int
        public final val RED_BITS: kotlin.Int
            public final fun <get-RED_BITS>(): kotlin.Int
        public final val RENDERBUFFER: kotlin.Int
            public final fun <get-RENDERBUFFER>(): kotlin.Int
        public final val RENDERBUFFER_ALPHA_SIZE: kotlin.Int
            public final fun <get-RENDERBUFFER_ALPHA_SIZE>(): kotlin.Int
        public final val RENDERBUFFER_BINDING: kotlin.Int
            public final fun <get-RENDERBUFFER_BINDING>(): kotlin.Int
        public final val RENDERBUFFER_BLUE_SIZE: kotlin.Int
            public final fun <get-RENDERBUFFER_BLUE_SIZE>(): kotlin.Int
        public final val RENDERBUFFER_DEPTH_SIZE: kotlin.Int
            public final fun <get-RENDERBUFFER_DEPTH_SIZE>(): kotlin.Int
        public final val RENDERBUFFER_GREEN_SIZE: kotlin.Int
            public final fun <get-RENDERBUFFER_GREEN_SIZE>(): kotlin.Int
        public final val RENDERBUFFER_HEIGHT: kotlin.Int
            public final fun <get-RENDERBUFFER_HEIGHT>(): kotlin.Int
        public final val RENDERBUFFER_INTERNAL_FORMAT: kotlin.Int
            public final fun <get-RENDERBUFFER_INTERNAL_FORMAT>(): kotlin.Int
        public final val RENDERBUFFER_RED_SIZE: kotlin.Int
            public final fun <get-RENDERBUFFER_RED_SIZE>(): kotlin.Int
        public final val RENDERBUFFER_STENCIL_SIZE: kotlin.Int
            public final fun <get-RENDERBUFFER_STENCIL_SIZE>(): kotlin.Int
        public final val RENDERBUFFER_WIDTH: kotlin.Int
            public final fun <get-RENDERBUFFER_WIDTH>(): kotlin.Int
        public final val RENDERER: kotlin.Int
            public final fun <get-RENDERER>(): kotlin.Int
        public final val REPEAT: kotlin.Int
            public final fun <get-REPEAT>(): kotlin.Int
        public final val REPLACE: kotlin.Int
            public final fun <get-REPLACE>(): kotlin.Int
        public final val RGB: kotlin.Int
            public final fun <get-RGB>(): kotlin.Int
        public final val RGB565: kotlin.Int
            public final fun <get-RGB565>(): kotlin.Int
        public final val RGB5_A1: kotlin.Int
            public final fun <get-RGB5_A1>(): kotlin.Int
        public final val RGBA: kotlin.Int
            public final fun <get-RGBA>(): kotlin.Int
        public final val RGBA4: kotlin.Int
            public final fun <get-RGBA4>(): kotlin.Int
        public final val SAMPLER_2D: kotlin.Int
            public final fun <get-SAMPLER_2D>(): kotlin.Int
        public final val SAMPLER_CUBE: kotlin.Int
            public final fun <get-SAMPLER_CUBE>(): kotlin.Int
        public final val SAMPLES: kotlin.Int
            public final fun <get-SAMPLES>(): kotlin.Int
        public final val SAMPLE_ALPHA_TO_COVERAGE: kotlin.Int
            public final fun <get-SAMPLE_ALPHA_TO_COVERAGE>(): kotlin.Int
        public final val SAMPLE_BUFFERS: kotlin.Int
            public final fun <get-SAMPLE_BUFFERS>(): kotlin.Int
        public final val SAMPLE_COVERAGE: kotlin.Int
            public final fun <get-SAMPLE_COVERAGE>(): kotlin.Int
        public final val SAMPLE_COVERAGE_INVERT: kotlin.Int
            public final fun <get-SAMPLE_COVERAGE_INVERT>(): kotlin.Int
        public final val SAMPLE_COVERAGE_VALUE: kotlin.Int
            public final fun <get-SAMPLE_COVERAGE_VALUE>(): kotlin.Int
        public final val SCISSOR_BOX: kotlin.Int
            public final fun <get-SCISSOR_BOX>(): kotlin.Int
        public final val SCISSOR_TEST: kotlin.Int
            public final fun <get-SCISSOR_TEST>(): kotlin.Int
        public final val SHADER_TYPE: kotlin.Int
            public final fun <get-SHADER_TYPE>(): kotlin.Int
        public final val SHADING_LANGUAGE_VERSION: kotlin.Int
            public final fun <get-SHADING_LANGUAGE_VERSION>(): kotlin.Int
        public final val SHORT: kotlin.Int
            public final fun <get-SHORT>(): kotlin.Int
        public final val SRC_ALPHA: kotlin.Int
            public final fun <get-SRC_ALPHA>(): kotlin.Int
        public final val SRC_ALPHA_SATURATE: kotlin.Int
            public final fun <get-SRC_ALPHA_SATURATE>(): kotlin.Int
        public final val SRC_COLOR: kotlin.Int
            public final fun <get-SRC_COLOR>(): kotlin.Int
        public final val STATIC_DRAW: kotlin.Int
            public final fun <get-STATIC_DRAW>(): kotlin.Int
        public final val STENCIL_ATTACHMENT: kotlin.Int
            public final fun <get-STENCIL_ATTACHMENT>(): kotlin.Int
        public final val STENCIL_BACK_FAIL: kotlin.Int
            public final fun <get-STENCIL_BACK_FAIL>(): kotlin.Int
        public final val STENCIL_BACK_FUNC: kotlin.Int
            public final fun <get-STENCIL_BACK_FUNC>(): kotlin.Int
        public final val STENCIL_BACK_PASS_DEPTH_FAIL: kotlin.Int
            public final fun <get-STENCIL_BACK_PASS_DEPTH_FAIL>(): kotlin.Int
        public final val STENCIL_BACK_PASS_DEPTH_PASS: kotlin.Int
            public final fun <get-STENCIL_BACK_PASS_DEPTH_PASS>(): kotlin.Int
        public final val STENCIL_BACK_REF: kotlin.Int
            public final fun <get-STENCIL_BACK_REF>(): kotlin.Int
        public final val STENCIL_BACK_VALUE_MASK: kotlin.Int
            public final fun <get-STENCIL_BACK_VALUE_MASK>(): kotlin.Int
        public final val STENCIL_BACK_WRITEMASK: kotlin.Int
            public final fun <get-STENCIL_BACK_WRITEMASK>(): kotlin.Int
        public final val STENCIL_BITS: kotlin.Int
            public final fun <get-STENCIL_BITS>(): kotlin.Int
        public final val STENCIL_BUFFER_BIT: kotlin.Int
            public final fun <get-STENCIL_BUFFER_BIT>(): kotlin.Int
        public final val STENCIL_CLEAR_VALUE: kotlin.Int
            public final fun <get-STENCIL_CLEAR_VALUE>(): kotlin.Int
        public final val STENCIL_FAIL: kotlin.Int
            public final fun <get-STENCIL_FAIL>(): kotlin.Int
        public final val STENCIL_FUNC: kotlin.Int
            public final fun <get-STENCIL_FUNC>(): kotlin.Int
        public final val STENCIL_INDEX: kotlin.Int
            public final fun <get-STENCIL_INDEX>(): kotlin.Int
        public final val STENCIL_INDEX8: kotlin.Int
            public final fun <get-STENCIL_INDEX8>(): kotlin.Int
        public final val STENCIL_PASS_DEPTH_FAIL: kotlin.Int
            public final fun <get-STENCIL_PASS_DEPTH_FAIL>(): kotlin.Int
        public final val STENCIL_PASS_DEPTH_PASS: kotlin.Int
            public final fun <get-STENCIL_PASS_DEPTH_PASS>(): kotlin.Int
        public final val STENCIL_REF: kotlin.Int
            public final fun <get-STENCIL_REF>(): kotlin.Int
        public final val STENCIL_TEST: kotlin.Int
            public final fun <get-STENCIL_TEST>(): kotlin.Int
        public final val STENCIL_VALUE_MASK: kotlin.Int
            public final fun <get-STENCIL_VALUE_MASK>(): kotlin.Int
        public final val STENCIL_WRITEMASK: kotlin.Int
            public final fun <get-STENCIL_WRITEMASK>(): kotlin.Int
        public final val STREAM_DRAW: kotlin.Int
            public final fun <get-STREAM_DRAW>(): kotlin.Int
        public final val SUBPIXEL_BITS: kotlin.Int
            public final fun <get-SUBPIXEL_BITS>(): kotlin.Int
        public final val TEXTURE: kotlin.Int
            public final fun <get-TEXTURE>(): kotlin.Int
        public final val TEXTURE0: kotlin.Int
            public final fun <get-TEXTURE0>(): kotlin.Int
        public final val TEXTURE1: kotlin.Int
            public final fun <get-TEXTURE1>(): kotlin.Int
        public final val TEXTURE10: kotlin.Int
            public final fun <get-TEXTURE10>(): kotlin.Int
        public final val TEXTURE11: kotlin.Int
            public final fun <get-TEXTURE11>(): kotlin.Int
        public final val TEXTURE12: kotlin.Int
            public final fun <get-TEXTURE12>(): kotlin.Int
        public final val TEXTURE13: kotlin.Int
            public final fun <get-TEXTURE13>(): kotlin.Int
        public final val TEXTURE14: kotlin.Int
            public final fun <get-TEXTURE14>(): kotlin.Int
        public final val TEXTURE15: kotlin.Int
            public final fun <get-TEXTURE15>(): kotlin.Int
        public final val TEXTURE16: kotlin.Int
            public final fun <get-TEXTURE16>(): kotlin.Int
        public final val TEXTURE17: kotlin.Int
            public final fun <get-TEXTURE17>(): kotlin.Int
        public final val TEXTURE18: kotlin.Int
            public final fun <get-TEXTURE18>(): kotlin.Int
        public final val TEXTURE19: kotlin.Int
            public final fun <get-TEXTURE19>(): kotlin.Int
        public final val TEXTURE2: kotlin.Int
            public final fun <get-TEXTURE2>(): kotlin.Int
        public final val TEXTURE20: kotlin.Int
            public final fun <get-TEXTURE20>(): kotlin.Int
        public final val TEXTURE21: kotlin.Int
            public final fun <get-TEXTURE21>(): kotlin.Int
        public final val TEXTURE22: kotlin.Int
            public final fun <get-TEXTURE22>(): kotlin.Int
        public final val TEXTURE23: kotlin.Int
            public final fun <get-TEXTURE23>(): kotlin.Int
        public final val TEXTURE24: kotlin.Int
            public final fun <get-TEXTURE24>(): kotlin.Int
        public final val TEXTURE25: kotlin.Int
            public final fun <get-TEXTURE25>(): kotlin.Int
        public final val TEXTURE26: kotlin.Int
            public final fun <get-TEXTURE26>(): kotlin.Int
        public final val TEXTURE27: kotlin.Int
            public final fun <get-TEXTURE27>(): kotlin.Int
        public final val TEXTURE28: kotlin.Int
            public final fun <get-TEXTURE28>(): kotlin.Int
        public final val TEXTURE29: kotlin.Int
            public final fun <get-TEXTURE29>(): kotlin.Int
        public final val TEXTURE3: kotlin.Int
            public final fun <get-TEXTURE3>(): kotlin.Int
        public final val TEXTURE30: kotlin.Int
            public final fun <get-TEXTURE30>(): kotlin.Int
        public final val TEXTURE31: kotlin.Int
            public final fun <get-TEXTURE31>(): kotlin.Int
        public final val TEXTURE4: kotlin.Int
            public final fun <get-TEXTURE4>(): kotlin.Int
        public final val TEXTURE5: kotlin.Int
            public final fun <get-TEXTURE5>(): kotlin.Int
        public final val TEXTURE6: kotlin.Int
            public final fun <get-TEXTURE6>(): kotlin.Int
        public final val TEXTURE7: kotlin.Int
            public final fun <get-TEXTURE7>(): kotlin.Int
        public final val TEXTURE8: kotlin.Int
            public final fun <get-TEXTURE8>(): kotlin.Int
        public final val TEXTURE9: kotlin.Int
            public final fun <get-TEXTURE9>(): kotlin.Int
        public final val TEXTURE_2D: kotlin.Int
            public final fun <get-TEXTURE_2D>(): kotlin.Int
        public final val TEXTURE_BINDING_2D: kotlin.Int
            public final fun <get-TEXTURE_BINDING_2D>(): kotlin.Int
        public final val TEXTURE_BINDING_CUBE_MAP: kotlin.Int
            public final fun <get-TEXTURE_BINDING_CUBE_MAP>(): kotlin.Int
        public final val TEXTURE_CUBE_MAP: kotlin.Int
            public final fun <get-TEXTURE_CUBE_MAP>(): kotlin.Int
        public final val TEXTURE_CUBE_MAP_NEGATIVE_X: kotlin.Int
            public final fun <get-TEXTURE_CUBE_MAP_NEGATIVE_X>(): kotlin.Int
        public final val TEXTURE_CUBE_MAP_NEGATIVE_Y: kotlin.Int
            public final fun <get-TEXTURE_CUBE_MAP_NEGATIVE_Y>(): kotlin.Int
        public final val TEXTURE_CUBE_MAP_NEGATIVE_Z: kotlin.Int
            public final fun <get-TEXTURE_CUBE_MAP_NEGATIVE_Z>(): kotlin.Int
        public final val TEXTURE_CUBE_MAP_POSITIVE_X: kotlin.Int
            public final fun <get-TEXTURE_CUBE_MAP_POSITIVE_X>(): kotlin.Int
        public final val TEXTURE_CUBE_MAP_POSITIVE_Y: kotlin.Int
            public final fun <get-TEXTURE_CUBE_MAP_POSITIVE_Y>(): kotlin.Int
        public final val TEXTURE_CUBE_MAP_POSITIVE_Z: kotlin.Int
            public final fun <get-TEXTURE_CUBE_MAP_POSITIVE_Z>(): kotlin.Int
        public final val TEXTURE_MAG_FILTER: kotlin.Int
            public final fun <get-TEXTURE_MAG_FILTER>(): kotlin.Int
        public final val TEXTURE_MIN_FILTER: kotlin.Int
            public final fun <get-TEXTURE_MIN_FILTER>(): kotlin.Int
        public final val TEXTURE_WRAP_S: kotlin.Int
            public final fun <get-TEXTURE_WRAP_S>(): kotlin.Int
        public final val TEXTURE_WRAP_T: kotlin.Int
            public final fun <get-TEXTURE_WRAP_T>(): kotlin.Int
        public final val TRIANGLES: kotlin.Int
            public final fun <get-TRIANGLES>(): kotlin.Int
        public final val TRIANGLE_FAN: kotlin.Int
            public final fun <get-TRIANGLE_FAN>(): kotlin.Int
        public final val TRIANGLE_STRIP: kotlin.Int
            public final fun <get-TRIANGLE_STRIP>(): kotlin.Int
        public final val UNPACK_ALIGNMENT: kotlin.Int
            public final fun <get-UNPACK_ALIGNMENT>(): kotlin.Int
        public final val UNPACK_COLORSPACE_CONVERSION_WEBGL: kotlin.Int
            public final fun <get-UNPACK_COLORSPACE_CONVERSION_WEBGL>(): kotlin.Int
        public final val UNPACK_FLIP_Y_WEBGL: kotlin.Int
            public final fun <get-UNPACK_FLIP_Y_WEBGL>(): kotlin.Int
        public final val UNPACK_PREMULTIPLY_ALPHA_WEBGL: kotlin.Int
            public final fun <get-UNPACK_PREMULTIPLY_ALPHA_WEBGL>(): kotlin.Int
        public final val UNSIGNED_BYTE: kotlin.Int
            public final fun <get-UNSIGNED_BYTE>(): kotlin.Int
        public final val UNSIGNED_INT: kotlin.Int
            public final fun <get-UNSIGNED_INT>(): kotlin.Int
        public final val UNSIGNED_SHORT: kotlin.Int
            public final fun <get-UNSIGNED_SHORT>(): kotlin.Int
        public final val UNSIGNED_SHORT_4_4_4_4: kotlin.Int
            public final fun <get-UNSIGNED_SHORT_4_4_4_4>(): kotlin.Int
        public final val UNSIGNED_SHORT_5_5_5_1: kotlin.Int
            public final fun <get-UNSIGNED_SHORT_5_5_5_1>(): kotlin.Int
        public final val UNSIGNED_SHORT_5_6_5: kotlin.Int
            public final fun <get-UNSIGNED_SHORT_5_6_5>(): kotlin.Int
        public final val VALIDATE_STATUS: kotlin.Int
            public final fun <get-VALIDATE_STATUS>(): kotlin.Int
        public final val VENDOR: kotlin.Int
            public final fun <get-VENDOR>(): kotlin.Int
        public final val VERSION: kotlin.Int
            public final fun <get-VERSION>(): kotlin.Int
        public final val VERTEX_ATTRIB_ARRAY_BUFFER_BINDING: kotlin.Int
            public final fun <get-VERTEX_ATTRIB_ARRAY_BUFFER_BINDING>(): kotlin.Int
        public final val VERTEX_ATTRIB_ARRAY_ENABLED: kotlin.Int
            public final fun <get-VERTEX_ATTRIB_ARRAY_ENABLED>(): kotlin.Int
        public final val VERTEX_ATTRIB_ARRAY_NORMALIZED: kotlin.Int
            public final fun <get-VERTEX_ATTRIB_ARRAY_NORMALIZED>(): kotlin.Int
        public final val VERTEX_ATTRIB_ARRAY_POINTER: kotlin.Int
            public final fun <get-VERTEX_ATTRIB_ARRAY_POINTER>(): kotlin.Int
        public final val VERTEX_ATTRIB_ARRAY_SIZE: kotlin.Int
            public final fun <get-VERTEX_ATTRIB_ARRAY_SIZE>(): kotlin.Int
        public final val VERTEX_ATTRIB_ARRAY_STRIDE: kotlin.Int
            public final fun <get-VERTEX_ATTRIB_ARRAY_STRIDE>(): kotlin.Int
        public final val VERTEX_ATTRIB_ARRAY_TYPE: kotlin.Int
            public final fun <get-VERTEX_ATTRIB_ARRAY_TYPE>(): kotlin.Int
        public final val VERTEX_SHADER: kotlin.Int
            public final fun <get-VERTEX_SHADER>(): kotlin.Int
        public final val VIEWPORT: kotlin.Int
            public final fun <get-VIEWPORT>(): kotlin.Int
        public final val ZERO: kotlin.Int
            public final fun <get-ZERO>(): kotlin.Int
    }
}

public external interface WebGLRenderingContextBase {
    public abstract val canvas: org.w3c.dom.HTMLCanvasElement
        public abstract fun <get-canvas>(): org.w3c.dom.HTMLCanvasElement
    public abstract val drawingBufferHeight: kotlin.Int
        public abstract fun <get-drawingBufferHeight>(): kotlin.Int
    public abstract val drawingBufferWidth: kotlin.Int
        public abstract fun <get-drawingBufferWidth>(): kotlin.Int
    public abstract fun activeTexture(/*0*/ texture: kotlin.Int): kotlin.Unit
    public abstract fun attachShader(/*0*/ program: org.khronos.webgl.WebGLProgram?, /*1*/ shader: org.khronos.webgl.WebGLShader?): kotlin.Unit
    public abstract fun bindAttribLocation(/*0*/ program: org.khronos.webgl.WebGLProgram?, /*1*/ index: kotlin.Int, /*2*/ name: kotlin.String): kotlin.Unit
    public abstract fun bindBuffer(/*0*/ target: kotlin.Int, /*1*/ buffer: org.khronos.webgl.WebGLBuffer?): kotlin.Unit
    public abstract fun bindFramebuffer(/*0*/ target: kotlin.Int, /*1*/ framebuffer: org.khronos.webgl.WebGLFramebuffer?): kotlin.Unit
    public abstract fun bindRenderbuffer(/*0*/ target: kotlin.Int, /*1*/ renderbuffer: org.khronos.webgl.WebGLRenderbuffer?): kotlin.Unit
    public abstract fun bindTexture(/*0*/ target: kotlin.Int, /*1*/ texture: org.khronos.webgl.WebGLTexture?): kotlin.Unit
    public abstract fun blendColor(/*0*/ red: kotlin.Float, /*1*/ green: kotlin.Float, /*2*/ blue: kotlin.Float, /*3*/ alpha: kotlin.Float): kotlin.Unit
    public abstract fun blendEquation(/*0*/ mode: kotlin.Int): kotlin.Unit
    public abstract fun blendEquationSeparate(/*0*/ modeRGB: kotlin.Int, /*1*/ modeAlpha: kotlin.Int): kotlin.Unit
    public abstract fun blendFunc(/*0*/ sfactor: kotlin.Int, /*1*/ dfactor: kotlin.Int): kotlin.Unit
    public abstract fun blendFuncSeparate(/*0*/ srcRGB: kotlin.Int, /*1*/ dstRGB: kotlin.Int, /*2*/ srcAlpha: kotlin.Int, /*3*/ dstAlpha: kotlin.Int): kotlin.Unit
    public abstract fun bufferData(/*0*/ target: kotlin.Int, /*1*/ size: kotlin.Int, /*2*/ usage: kotlin.Int): kotlin.Unit
    public abstract fun bufferData(/*0*/ target: kotlin.Int, /*1*/ data: org.khronos.webgl.BufferDataSource?, /*2*/ usage: kotlin.Int): kotlin.Unit
    public abstract fun bufferSubData(/*0*/ target: kotlin.Int, /*1*/ offset: kotlin.Int, /*2*/ data: org.khronos.webgl.BufferDataSource?): kotlin.Unit
    public abstract fun checkFramebufferStatus(/*0*/ target: kotlin.Int): kotlin.Int
    public abstract fun clear(/*0*/ mask: kotlin.Int): kotlin.Unit
    public abstract fun clearColor(/*0*/ red: kotlin.Float, /*1*/ green: kotlin.Float, /*2*/ blue: kotlin.Float, /*3*/ alpha: kotlin.Float): kotlin.Unit
    public abstract fun clearDepth(/*0*/ depth: kotlin.Float): kotlin.Unit
    public abstract fun clearStencil(/*0*/ s: kotlin.Int): kotlin.Unit
    public abstract fun colorMask(/*0*/ red: kotlin.Boolean, /*1*/ green: kotlin.Boolean, /*2*/ blue: kotlin.Boolean, /*3*/ alpha: kotlin.Boolean): kotlin.Unit
    public abstract fun compileShader(/*0*/ shader: org.khronos.webgl.WebGLShader?): kotlin.Unit
    public abstract fun compressedTexImage2D(/*0*/ target: kotlin.Int, /*1*/ level: kotlin.Int, /*2*/ internalformat: kotlin.Int, /*3*/ width: kotlin.Int, /*4*/ height: kotlin.Int, /*5*/ border: kotlin.Int, /*6*/ data: org.khronos.webgl.ArrayBufferView): kotlin.Unit
    public abstract fun compressedTexSubImage2D(/*0*/ target: kotlin.Int, /*1*/ level: kotlin.Int, /*2*/ xoffset: kotlin.Int, /*3*/ yoffset: kotlin.Int, /*4*/ width: kotlin.Int, /*5*/ height: kotlin.Int, /*6*/ format: kotlin.Int, /*7*/ data: org.khronos.webgl.ArrayBufferView): kotlin.Unit
    public abstract fun copyTexImage2D(/*0*/ target: kotlin.Int, /*1*/ level: kotlin.Int, /*2*/ internalformat: kotlin.Int, /*3*/ x: kotlin.Int, /*4*/ y: kotlin.Int, /*5*/ width: kotlin.Int, /*6*/ height: kotlin.Int, /*7*/ border: kotlin.Int): kotlin.Unit
    public abstract fun copyTexSubImage2D(/*0*/ target: kotlin.Int, /*1*/ level: kotlin.Int, /*2*/ xoffset: kotlin.Int, /*3*/ yoffset: kotlin.Int, /*4*/ x: kotlin.Int, /*5*/ y: kotlin.Int, /*6*/ width: kotlin.Int, /*7*/ height: kotlin.Int): kotlin.Unit
    public abstract fun createBuffer(): org.khronos.webgl.WebGLBuffer?
    public abstract fun createFramebuffer(): org.khronos.webgl.WebGLFramebuffer?
    public abstract fun createProgram(): org.khronos.webgl.WebGLProgram?
    public abstract fun createRenderbuffer(): org.khronos.webgl.WebGLRenderbuffer?
    public abstract fun createShader(/*0*/ type: kotlin.Int): org.khronos.webgl.WebGLShader?
    public abstract fun createTexture(): org.khronos.webgl.WebGLTexture?
    public abstract fun cullFace(/*0*/ mode: kotlin.Int): kotlin.Unit
    public abstract fun deleteBuffer(/*0*/ buffer: org.khronos.webgl.WebGLBuffer?): kotlin.Unit
    public abstract fun deleteFramebuffer(/*0*/ framebuffer: org.khronos.webgl.WebGLFramebuffer?): kotlin.Unit
    public abstract fun deleteProgram(/*0*/ program: org.khronos.webgl.WebGLProgram?): kotlin.Unit
    public abstract fun deleteRenderbuffer(/*0*/ renderbuffer: org.khronos.webgl.WebGLRenderbuffer?): kotlin.Unit
    public abstract fun deleteShader(/*0*/ shader: org.khronos.webgl.WebGLShader?): kotlin.Unit
    public abstract fun deleteTexture(/*0*/ texture: org.khronos.webgl.WebGLTexture?): kotlin.Unit
    public abstract fun depthFunc(/*0*/ func: kotlin.Int): kotlin.Unit
    public abstract fun depthMask(/*0*/ flag: kotlin.Boolean): kotlin.Unit
    public abstract fun depthRange(/*0*/ zNear: kotlin.Float, /*1*/ zFar: kotlin.Float): kotlin.Unit
    public abstract fun detachShader(/*0*/ program: org.khronos.webgl.WebGLProgram?, /*1*/ shader: org.khronos.webgl.WebGLShader?): kotlin.Unit
    public abstract fun disable(/*0*/ cap: kotlin.Int): kotlin.Unit
    public abstract fun disableVertexAttribArray(/*0*/ index: kotlin.Int): kotlin.Unit
    public abstract fun drawArrays(/*0*/ mode: kotlin.Int, /*1*/ first: kotlin.Int, /*2*/ count: kotlin.Int): kotlin.Unit
    public abstract fun drawElements(/*0*/ mode: kotlin.Int, /*1*/ count: kotlin.Int, /*2*/ type: kotlin.Int, /*3*/ offset: kotlin.Int): kotlin.Unit
    public abstract fun enable(/*0*/ cap: kotlin.Int): kotlin.Unit
    public abstract fun enableVertexAttribArray(/*0*/ index: kotlin.Int): kotlin.Unit
    public abstract fun finish(): kotlin.Unit
    public abstract fun flush(): kotlin.Unit
    public abstract fun framebufferRenderbuffer(/*0*/ target: kotlin.Int, /*1*/ attachment: kotlin.Int, /*2*/ renderbuffertarget: kotlin.Int, /*3*/ renderbuffer: org.khronos.webgl.WebGLRenderbuffer?): kotlin.Unit
    public abstract fun framebufferTexture2D(/*0*/ target: kotlin.Int, /*1*/ attachment: kotlin.Int, /*2*/ textarget: kotlin.Int, /*3*/ texture: org.khronos.webgl.WebGLTexture?, /*4*/ level: kotlin.Int): kotlin.Unit
    public abstract fun frontFace(/*0*/ mode: kotlin.Int): kotlin.Unit
    public abstract fun generateMipmap(/*0*/ target: kotlin.Int): kotlin.Unit
    public abstract fun getActiveAttrib(/*0*/ program: org.khronos.webgl.WebGLProgram?, /*1*/ index: kotlin.Int): org.khronos.webgl.WebGLActiveInfo?
    public abstract fun getActiveUniform(/*0*/ program: org.khronos.webgl.WebGLProgram?, /*1*/ index: kotlin.Int): org.khronos.webgl.WebGLActiveInfo?
    public abstract fun getAttachedShaders(/*0*/ program: org.khronos.webgl.WebGLProgram?): kotlin.Array<org.khronos.webgl.WebGLShader>?
    public abstract fun getAttribLocation(/*0*/ program: org.khronos.webgl.WebGLProgram?, /*1*/ name: kotlin.String): kotlin.Int
    public abstract fun getBufferParameter(/*0*/ target: kotlin.Int, /*1*/ pname: kotlin.Int): kotlin.Any?
    public abstract fun getContextAttributes(): org.khronos.webgl.WebGLContextAttributes?
    public abstract fun getError(): kotlin.Int
    public abstract fun getExtension(/*0*/ name: kotlin.String): dynamic
    public abstract fun getFramebufferAttachmentParameter(/*0*/ target: kotlin.Int, /*1*/ attachment: kotlin.Int, /*2*/ pname: kotlin.Int): kotlin.Any?
    public abstract fun getParameter(/*0*/ pname: kotlin.Int): kotlin.Any?
    public abstract fun getProgramInfoLog(/*0*/ program: org.khronos.webgl.WebGLProgram?): kotlin.String?
    public abstract fun getProgramParameter(/*0*/ program: org.khronos.webgl.WebGLProgram?, /*1*/ pname: kotlin.Int): kotlin.Any?
    public abstract fun getRenderbufferParameter(/*0*/ target: kotlin.Int, /*1*/ pname: kotlin.Int): kotlin.Any?
    public abstract fun getShaderInfoLog(/*0*/ shader: org.khronos.webgl.WebGLShader?): kotlin.String?
    public abstract fun getShaderParameter(/*0*/ shader: org.khronos.webgl.WebGLShader?, /*1*/ pname: kotlin.Int): kotlin.Any?
    public abstract fun getShaderPrecisionFormat(/*0*/ shadertype: kotlin.Int, /*1*/ precisiontype: kotlin.Int): org.khronos.webgl.WebGLShaderPrecisionFormat?
    public abstract fun getShaderSource(/*0*/ shader: org.khronos.webgl.WebGLShader?): kotlin.String?
    public abstract fun getSupportedExtensions(): kotlin.Array<kotlin.String>?
    public abstract fun getTexParameter(/*0*/ target: kotlin.Int, /*1*/ pname: kotlin.Int): kotlin.Any?
    public abstract fun getUniform(/*0*/ program: org.khronos.webgl.WebGLProgram?, /*1*/ location: org.khronos.webgl.WebGLUniformLocation?): kotlin.Any?
    public abstract fun getUniformLocation(/*0*/ program: org.khronos.webgl.WebGLProgram?, /*1*/ name: kotlin.String): org.khronos.webgl.WebGLUniformLocation?
    public abstract fun getVertexAttrib(/*0*/ index: kotlin.Int, /*1*/ pname: kotlin.Int): kotlin.Any?
    public abstract fun getVertexAttribOffset(/*0*/ index: kotlin.Int, /*1*/ pname: kotlin.Int): kotlin.Int
    public abstract fun hint(/*0*/ target: kotlin.Int, /*1*/ mode: kotlin.Int): kotlin.Unit
    public abstract fun isBuffer(/*0*/ buffer: org.khronos.webgl.WebGLBuffer?): kotlin.Boolean
    public abstract fun isContextLost(): kotlin.Boolean
    public abstract fun isEnabled(/*0*/ cap: kotlin.Int): kotlin.Boolean
    public abstract fun isFramebuffer(/*0*/ framebuffer: org.khronos.webgl.WebGLFramebuffer?): kotlin.Boolean
    public abstract fun isProgram(/*0*/ program: org.khronos.webgl.WebGLProgram?): kotlin.Boolean
    public abstract fun isRenderbuffer(/*0*/ renderbuffer: org.khronos.webgl.WebGLRenderbuffer?): kotlin.Boolean
    public abstract fun isShader(/*0*/ shader: org.khronos.webgl.WebGLShader?): kotlin.Boolean
    public abstract fun isTexture(/*0*/ texture: org.khronos.webgl.WebGLTexture?): kotlin.Boolean
    public abstract fun lineWidth(/*0*/ width: kotlin.Float): kotlin.Unit
    public abstract fun linkProgram(/*0*/ program: org.khronos.webgl.WebGLProgram?): kotlin.Unit
    public abstract fun pixelStorei(/*0*/ pname: kotlin.Int, /*1*/ param: kotlin.Int): kotlin.Unit
    public abstract fun polygonOffset(/*0*/ factor: kotlin.Float, /*1*/ units: kotlin.Float): kotlin.Unit
    public abstract fun readPixels(/*0*/ x: kotlin.Int, /*1*/ y: kotlin.Int, /*2*/ width: kotlin.Int, /*3*/ height: kotlin.Int, /*4*/ format: kotlin.Int, /*5*/ type: kotlin.Int, /*6*/ pixels: org.khronos.webgl.ArrayBufferView?): kotlin.Unit
    public abstract fun renderbufferStorage(/*0*/ target: kotlin.Int, /*1*/ internalformat: kotlin.Int, /*2*/ width: kotlin.Int, /*3*/ height: kotlin.Int): kotlin.Unit
    public abstract fun sampleCoverage(/*0*/ value: kotlin.Float, /*1*/ invert: kotlin.Boolean): kotlin.Unit
    public abstract fun scissor(/*0*/ x: kotlin.Int, /*1*/ y: kotlin.Int, /*2*/ width: kotlin.Int, /*3*/ height: kotlin.Int): kotlin.Unit
    public abstract fun shaderSource(/*0*/ shader: org.khronos.webgl.WebGLShader?, /*1*/ source: kotlin.String): kotlin.Unit
    public abstract fun stencilFunc(/*0*/ func: kotlin.Int, /*1*/ ref: kotlin.Int, /*2*/ mask: kotlin.Int): kotlin.Unit
    public abstract fun stencilFuncSeparate(/*0*/ face: kotlin.Int, /*1*/ func: kotlin.Int, /*2*/ ref: kotlin.Int, /*3*/ mask: kotlin.Int): kotlin.Unit
    public abstract fun stencilMask(/*0*/ mask: kotlin.Int): kotlin.Unit
    public abstract fun stencilMaskSeparate(/*0*/ face: kotlin.Int, /*1*/ mask: kotlin.Int): kotlin.Unit
    public abstract fun stencilOp(/*0*/ fail: kotlin.Int, /*1*/ zfail: kotlin.Int, /*2*/ zpass: kotlin.Int): kotlin.Unit
    public abstract fun stencilOpSeparate(/*0*/ face: kotlin.Int, /*1*/ fail: kotlin.Int, /*2*/ zfail: kotlin.Int, /*3*/ zpass: kotlin.Int): kotlin.Unit
    public abstract fun texImage2D(/*0*/ target: kotlin.Int, /*1*/ level: kotlin.Int, /*2*/ internalformat: kotlin.Int, /*3*/ width: kotlin.Int, /*4*/ height: kotlin.Int, /*5*/ border: kotlin.Int, /*6*/ format: kotlin.Int, /*7*/ type: kotlin.Int, /*8*/ pixels: org.khronos.webgl.ArrayBufferView?): kotlin.Unit
    public abstract fun texImage2D(/*0*/ target: kotlin.Int, /*1*/ level: kotlin.Int, /*2*/ internalformat: kotlin.Int, /*3*/ format: kotlin.Int, /*4*/ type: kotlin.Int, /*5*/ source: org.khronos.webgl.TexImageSource?): kotlin.Unit
    public abstract fun texParameterf(/*0*/ target: kotlin.Int, /*1*/ pname: kotlin.Int, /*2*/ param: kotlin.Float): kotlin.Unit
    public abstract fun texParameteri(/*0*/ target: kotlin.Int, /*1*/ pname: kotlin.Int, /*2*/ param: kotlin.Int): kotlin.Unit
    public abstract fun texSubImage2D(/*0*/ target: kotlin.Int, /*1*/ level: kotlin.Int, /*2*/ xoffset: kotlin.Int, /*3*/ yoffset: kotlin.Int, /*4*/ width: kotlin.Int, /*5*/ height: kotlin.Int, /*6*/ format: kotlin.Int, /*7*/ type: kotlin.Int, /*8*/ pixels: org.khronos.webgl.ArrayBufferView?): kotlin.Unit
    public abstract fun texSubImage2D(/*0*/ target: kotlin.Int, /*1*/ level: kotlin.Int, /*2*/ xoffset: kotlin.Int, /*3*/ yoffset: kotlin.Int, /*4*/ format: kotlin.Int, /*5*/ type: kotlin.Int, /*6*/ source: org.khronos.webgl.TexImageSource?): kotlin.Unit
    public abstract fun uniform1f(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ x: kotlin.Float): kotlin.Unit
    public abstract fun uniform1fv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: kotlin.Array<kotlin.Float>): kotlin.Unit
    public abstract fun uniform1fv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: org.khronos.webgl.Float32Array): kotlin.Unit
    public abstract fun uniform1i(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ x: kotlin.Int): kotlin.Unit
    public abstract fun uniform1iv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: kotlin.Array<kotlin.Int>): kotlin.Unit
    public abstract fun uniform1iv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: org.khronos.webgl.Int32Array): kotlin.Unit
    public abstract fun uniform2f(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ x: kotlin.Float, /*2*/ y: kotlin.Float): kotlin.Unit
    public abstract fun uniform2fv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: kotlin.Array<kotlin.Float>): kotlin.Unit
    public abstract fun uniform2fv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: org.khronos.webgl.Float32Array): kotlin.Unit
    public abstract fun uniform2i(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ x: kotlin.Int, /*2*/ y: kotlin.Int): kotlin.Unit
    public abstract fun uniform2iv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: kotlin.Array<kotlin.Int>): kotlin.Unit
    public abstract fun uniform2iv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: org.khronos.webgl.Int32Array): kotlin.Unit
    public abstract fun uniform3f(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ x: kotlin.Float, /*2*/ y: kotlin.Float, /*3*/ z: kotlin.Float): kotlin.Unit
    public abstract fun uniform3fv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: kotlin.Array<kotlin.Float>): kotlin.Unit
    public abstract fun uniform3fv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: org.khronos.webgl.Float32Array): kotlin.Unit
    public abstract fun uniform3i(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ x: kotlin.Int, /*2*/ y: kotlin.Int, /*3*/ z: kotlin.Int): kotlin.Unit
    public abstract fun uniform3iv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: kotlin.Array<kotlin.Int>): kotlin.Unit
    public abstract fun uniform3iv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: org.khronos.webgl.Int32Array): kotlin.Unit
    public abstract fun uniform4f(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ x: kotlin.Float, /*2*/ y: kotlin.Float, /*3*/ z: kotlin.Float, /*4*/ w: kotlin.Float): kotlin.Unit
    public abstract fun uniform4fv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: kotlin.Array<kotlin.Float>): kotlin.Unit
    public abstract fun uniform4fv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: org.khronos.webgl.Float32Array): kotlin.Unit
    public abstract fun uniform4i(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ x: kotlin.Int, /*2*/ y: kotlin.Int, /*3*/ z: kotlin.Int, /*4*/ w: kotlin.Int): kotlin.Unit
    public abstract fun uniform4iv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: kotlin.Array<kotlin.Int>): kotlin.Unit
    public abstract fun uniform4iv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ v: org.khronos.webgl.Int32Array): kotlin.Unit
    public abstract fun uniformMatrix2fv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ transpose: kotlin.Boolean, /*2*/ value: kotlin.Array<kotlin.Float>): kotlin.Unit
    public abstract fun uniformMatrix2fv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ transpose: kotlin.Boolean, /*2*/ value: org.khronos.webgl.Float32Array): kotlin.Unit
    public abstract fun uniformMatrix3fv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ transpose: kotlin.Boolean, /*2*/ value: kotlin.Array<kotlin.Float>): kotlin.Unit
    public abstract fun uniformMatrix3fv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ transpose: kotlin.Boolean, /*2*/ value: org.khronos.webgl.Float32Array): kotlin.Unit
    public abstract fun uniformMatrix4fv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ transpose: kotlin.Boolean, /*2*/ value: kotlin.Array<kotlin.Float>): kotlin.Unit
    public abstract fun uniformMatrix4fv(/*0*/ location: org.khronos.webgl.WebGLUniformLocation?, /*1*/ transpose: kotlin.Boolean, /*2*/ value: org.khronos.webgl.Float32Array): kotlin.Unit
    public abstract fun useProgram(/*0*/ program: org.khronos.webgl.WebGLProgram?): kotlin.Unit
    public abstract fun validateProgram(/*0*/ program: org.khronos.webgl.WebGLProgram?): kotlin.Unit
    public abstract fun vertexAttrib1f(/*0*/ index: kotlin.Int, /*1*/ x: kotlin.Float): kotlin.Unit
    public abstract fun vertexAttrib1fv(/*0*/ index: kotlin.Int, /*1*/ values: dynamic): kotlin.Unit
    public abstract fun vertexAttrib2f(/*0*/ index: kotlin.Int, /*1*/ x: kotlin.Float, /*2*/ y: kotlin.Float): kotlin.Unit
    public abstract fun vertexAttrib2fv(/*0*/ index: kotlin.Int, /*1*/ values: dynamic): kotlin.Unit
    public abstract fun vertexAttrib3f(/*0*/ index: kotlin.Int, /*1*/ x: kotlin.Float, /*2*/ y: kotlin.Float, /*3*/ z: kotlin.Float): kotlin.Unit
    public abstract fun vertexAttrib3fv(/*0*/ index: kotlin.Int, /*1*/ values: dynamic): kotlin.Unit
    public abstract fun vertexAttrib4f(/*0*/ index: kotlin.Int, /*1*/ x: kotlin.Float, /*2*/ y: kotlin.Float, /*3*/ z: kotlin.Float, /*4*/ w: kotlin.Float): kotlin.Unit
    public abstract fun vertexAttrib4fv(/*0*/ index: kotlin.Int, /*1*/ values: dynamic): kotlin.Unit
    public abstract fun vertexAttribPointer(/*0*/ index: kotlin.Int, /*1*/ size: kotlin.Int, /*2*/ type: kotlin.Int, /*3*/ normalized: kotlin.Boolean, /*4*/ stride: kotlin.Int, /*5*/ offset: kotlin.Int): kotlin.Unit
    public abstract fun viewport(/*0*/ x: kotlin.Int, /*1*/ y: kotlin.Int, /*2*/ width: kotlin.Int, /*3*/ height: kotlin.Int): kotlin.Unit

    public companion object Companion {
        public final val ACTIVE_ATTRIBUTES: kotlin.Int
            public final fun <get-ACTIVE_ATTRIBUTES>(): kotlin.Int
        public final val ACTIVE_TEXTURE: kotlin.Int
            public final fun <get-ACTIVE_TEXTURE>(): kotlin.Int
        public final val ACTIVE_UNIFORMS: kotlin.Int
            public final fun <get-ACTIVE_UNIFORMS>(): kotlin.Int
        public final val ALIASED_LINE_WIDTH_RANGE: kotlin.Int
            public final fun <get-ALIASED_LINE_WIDTH_RANGE>(): kotlin.Int
        public final val ALIASED_POINT_SIZE_RANGE: kotlin.Int
            public final fun <get-ALIASED_POINT_SIZE_RANGE>(): kotlin.Int
        public final val ALPHA: kotlin.Int
            public final fun <get-ALPHA>(): kotlin.Int
        public final val ALPHA_BITS: kotlin.Int
            public final fun <get-ALPHA_BITS>(): kotlin.Int
        public final val ALWAYS: kotlin.Int
            public final fun <get-ALWAYS>(): kotlin.Int
        public final val ARRAY_BUFFER: kotlin.Int
            public final fun <get-ARRAY_BUFFER>(): kotlin.Int
        public final val ARRAY_BUFFER_BINDING: kotlin.Int
            public final fun <get-ARRAY_BUFFER_BINDING>(): kotlin.Int
        public final val ATTACHED_SHADERS: kotlin.Int
            public final fun <get-ATTACHED_SHADERS>(): kotlin.Int
        public final val BACK: kotlin.Int
            public final fun <get-BACK>(): kotlin.Int
        public final val BLEND: kotlin.Int
            public final fun <get-BLEND>(): kotlin.Int
        public final val BLEND_COLOR: kotlin.Int
            public final fun <get-BLEND_COLOR>(): kotlin.Int
        public final val BLEND_DST_ALPHA: kotlin.Int
            public final fun <get-BLEND_DST_ALPHA>(): kotlin.Int
        public final val BLEND_DST_RGB: kotlin.Int
            public final fun <get-BLEND_DST_RGB>(): kotlin.Int
        public final val BLEND_EQUATION: kotlin.Int
            public final fun <get-BLEND_EQUATION>(): kotlin.Int
        public final val BLEND_EQUATION_ALPHA: kotlin.Int
            public final fun <get-BLEND_EQUATION_ALPHA>(): kotlin.Int
        public final val BLEND_EQUATION_RGB: kotlin.Int
            public final fun <get-BLEND_EQUATION_RGB>(): kotlin.Int
        public final val BLEND_SRC_ALPHA: kotlin.Int
            public final fun <get-BLEND_SRC_ALPHA>(): kotlin.Int
        public final val BLEND_SRC_RGB: kotlin.Int
            public final fun <get-BLEND_SRC_RGB>(): kotlin.Int
        public final val BLUE_BITS: kotlin.Int
            public final fun <get-BLUE_BITS>(): kotlin.Int
        public final val BOOL: kotlin.Int
            public final fun <get-BOOL>(): kotlin.Int
        public final val BOOL_VEC2: kotlin.Int
            public final fun <get-BOOL_VEC2>(): kotlin.Int
        public final val BOOL_VEC3: kotlin.Int
            public final fun <get-BOOL_VEC3>(): kotlin.Int
        public final val BOOL_VEC4: kotlin.Int
            public final fun <get-BOOL_VEC4>(): kotlin.Int
        public final val BROWSER_DEFAULT_WEBGL: kotlin.Int
            public final fun <get-BROWSER_DEFAULT_WEBGL>(): kotlin.Int
        public final val BUFFER_SIZE: kotlin.Int
            public final fun <get-BUFFER_SIZE>(): kotlin.Int
        public final val BUFFER_USAGE: kotlin.Int
            public final fun <get-BUFFER_USAGE>(): kotlin.Int
        public final val BYTE: kotlin.Int
            public final fun <get-BYTE>(): kotlin.Int
        public final val CCW: kotlin.Int
            public final fun <get-CCW>(): kotlin.Int
        public final val CLAMP_TO_EDGE: kotlin.Int
            public final fun <get-CLAMP_TO_EDGE>(): kotlin.Int
        public final val COLOR_ATTACHMENT0: kotlin.Int
            public final fun <get-COLOR_ATTACHMENT0>(): kotlin.Int
        public final val COLOR_BUFFER_BIT: kotlin.Int
            public final fun <get-COLOR_BUFFER_BIT>(): kotlin.Int
        public final val COLOR_CLEAR_VALUE: kotlin.Int
            public final fun <get-COLOR_CLEAR_VALUE>(): kotlin.Int
        public final val COLOR_WRITEMASK: kotlin.Int
            public final fun <get-COLOR_WRITEMASK>(): kotlin.Int
        public final val COMPILE_STATUS: kotlin.Int
            public final fun <get-COMPILE_STATUS>(): kotlin.Int
        public final val COMPRESSED_TEXTURE_FORMATS: kotlin.Int
            public final fun <get-COMPRESSED_TEXTURE_FORMATS>(): kotlin.Int
        public final val CONSTANT_ALPHA: kotlin.Int
            public final fun <get-CONSTANT_ALPHA>(): kotlin.Int
        public final val CONSTANT_COLOR: kotlin.Int
            public final fun <get-CONSTANT_COLOR>(): kotlin.Int
        public final val CONTEXT_LOST_WEBGL: kotlin.Int
            public final fun <get-CONTEXT_LOST_WEBGL>(): kotlin.Int
        public final val CULL_FACE: kotlin.Int
            public final fun <get-CULL_FACE>(): kotlin.Int
        public final val CULL_FACE_MODE: kotlin.Int
            public final fun <get-CULL_FACE_MODE>(): kotlin.Int
        public final val CURRENT_PROGRAM: kotlin.Int
            public final fun <get-CURRENT_PROGRAM>(): kotlin.Int
        public final val CURRENT_VERTEX_ATTRIB: kotlin.Int
            public final fun <get-CURRENT_VERTEX_ATTRIB>(): kotlin.Int
        public final val CW: kotlin.Int
            public final fun <get-CW>(): kotlin.Int
        public final val DECR: kotlin.Int
            public final fun <get-DECR>(): kotlin.Int
        public final val DECR_WRAP: kotlin.Int
            public final fun <get-DECR_WRAP>(): kotlin.Int
        public final val DELETE_STATUS: kotlin.Int
            public final fun <get-DELETE_STATUS>(): kotlin.Int
        public final val DEPTH_ATTACHMENT: kotlin.Int
            public final fun <get-DEPTH_ATTACHMENT>(): kotlin.Int
        public final val DEPTH_BITS: kotlin.Int
            public final fun <get-DEPTH_BITS>(): kotlin.Int
        public final val DEPTH_BUFFER_BIT: kotlin.Int
            public final fun <get-DEPTH_BUFFER_BIT>(): kotlin.Int
        public final val DEPTH_CLEAR_VALUE: kotlin.Int
            public final fun <get-DEPTH_CLEAR_VALUE>(): kotlin.Int
        public final val DEPTH_COMPONENT: kotlin.Int
            public final fun <get-DEPTH_COMPONENT>(): kotlin.Int
        public final val DEPTH_COMPONENT16: kotlin.Int
            public final fun <get-DEPTH_COMPONENT16>(): kotlin.Int
        public final val DEPTH_FUNC: kotlin.Int
            public final fun <get-DEPTH_FUNC>(): kotlin.Int
        public final val DEPTH_RANGE: kotlin.Int
            public final fun <get-DEPTH_RANGE>(): kotlin.Int
        public final val DEPTH_STENCIL: kotlin.Int
            public final fun <get-DEPTH_STENCIL>(): kotlin.Int
        public final val DEPTH_STENCIL_ATTACHMENT: kotlin.Int
            public final fun <get-DEPTH_STENCIL_ATTACHMENT>(): kotlin.Int
        public final val DEPTH_TEST: kotlin.Int
            public final fun <get-DEPTH_TEST>(): kotlin.Int
        public final val DEPTH_WRITEMASK: kotlin.Int
            public final fun <get-DEPTH_WRITEMASK>(): kotlin.Int
        public final val DITHER: kotlin.Int
            public final fun <get-DITHER>(): kotlin.Int
        public final val DONT_CARE: kotlin.Int
            public final fun <get-DONT_CARE>(): kotlin.Int
        public final val DST_ALPHA: kotlin.Int
            public final fun <get-DST_ALPHA>(): kotlin.Int
        public final val DST_COLOR: kotlin.Int
            public final fun <get-DST_COLOR>(): kotlin.Int
        public final val DYNAMIC_DRAW: kotlin.Int
            public final fun <get-DYNAMIC_DRAW>(): kotlin.Int
        public final val ELEMENT_ARRAY_BUFFER: kotlin.Int
            public final fun <get-ELEMENT_ARRAY_BUFFER>(): kotlin.Int
        public final val ELEMENT_ARRAY_BUFFER_BINDING: kotlin.Int
            public final fun <get-ELEMENT_ARRAY_BUFFER_BINDING>(): kotlin.Int
        public final val EQUAL: kotlin.Int
            public final fun <get-EQUAL>(): kotlin.Int
        public final val FASTEST: kotlin.Int
            public final fun <get-FASTEST>(): kotlin.Int
        public final val FLOAT: kotlin.Int
            public final fun <get-FLOAT>(): kotlin.Int
        public final val FLOAT_MAT2: kotlin.Int
            public final fun <get-FLOAT_MAT2>(): kotlin.Int
        public final val FLOAT_MAT3: kotlin.Int
            public final fun <get-FLOAT_MAT3>(): kotlin.Int
        public final val FLOAT_MAT4: kotlin.Int
            public final fun <get-FLOAT_MAT4>(): kotlin.Int
        public final val FLOAT_VEC2: kotlin.Int
            public final fun <get-FLOAT_VEC2>(): kotlin.Int
        public final val FLOAT_VEC3: kotlin.Int
            public final fun <get-FLOAT_VEC3>(): kotlin.Int
        public final val FLOAT_VEC4: kotlin.Int
            public final fun <get-FLOAT_VEC4>(): kotlin.Int
        public final val FRAGMENT_SHADER: kotlin.Int
            public final fun <get-FRAGMENT_SHADER>(): kotlin.Int
        public final val FRAMEBUFFER: kotlin.Int
            public final fun <get-FRAMEBUFFER>(): kotlin.Int
        public final val FRAMEBUFFER_ATTACHMENT_OBJECT_NAME: kotlin.Int
            public final fun <get-FRAMEBUFFER_ATTACHMENT_OBJECT_NAME>(): kotlin.Int
        public final val FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE: kotlin.Int
            public final fun <get-FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE>(): kotlin.Int
        public final val FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE: kotlin.Int
            public final fun <get-FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE>(): kotlin.Int
        public final val FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL: kotlin.Int
            public final fun <get-FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL>(): kotlin.Int
        public final val FRAMEBUFFER_BINDING: kotlin.Int
            public final fun <get-FRAMEBUFFER_BINDING>(): kotlin.Int
        public final val FRAMEBUFFER_COMPLETE: kotlin.Int
            public final fun <get-FRAMEBUFFER_COMPLETE>(): kotlin.Int
        public final val FRAMEBUFFER_INCOMPLETE_ATTACHMENT: kotlin.Int
            public final fun <get-FRAMEBUFFER_INCOMPLETE_ATTACHMENT>(): kotlin.Int
        public final val FRAMEBUFFER_INCOMPLETE_DIMENSIONS: kotlin.Int
            public final fun <get-FRAMEBUFFER_INCOMPLETE_DIMENSIONS>(): kotlin.Int
        public final val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: kotlin.Int
            public final fun <get-FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT>(): kotlin.Int
        public final val FRAMEBUFFER_UNSUPPORTED: kotlin.Int
            public final fun <get-FRAMEBUFFER_UNSUPPORTED>(): kotlin.Int
        public final val FRONT: kotlin.Int
            public final fun <get-FRONT>(): kotlin.Int
        public final val FRONT_AND_BACK: kotlin.Int
            public final fun <get-FRONT_AND_BACK>(): kotlin.Int
        public final val FRONT_FACE: kotlin.Int
            public final fun <get-FRONT_FACE>(): kotlin.Int
        public final val FUNC_ADD: kotlin.Int
            public final fun <get-FUNC_ADD>(): kotlin.Int
        public final val FUNC_REVERSE_SUBTRACT: kotlin.Int
            public final fun <get-FUNC_REVERSE_SUBTRACT>(): kotlin.Int
        public final val FUNC_SUBTRACT: kotlin.Int
            public final fun <get-FUNC_SUBTRACT>(): kotlin.Int
        public final val GENERATE_MIPMAP_HINT: kotlin.Int
            public final fun <get-GENERATE_MIPMAP_HINT>(): kotlin.Int
        public final val GEQUAL: kotlin.Int
            public final fun <get-GEQUAL>(): kotlin.Int
        public final val GREATER: kotlin.Int
            public final fun <get-GREATER>(): kotlin.Int
        public final val GREEN_BITS: kotlin.Int
            public final fun <get-GREEN_BITS>(): kotlin.Int
        public final val HIGH_FLOAT: kotlin.Int
            public final fun <get-HIGH_FLOAT>(): kotlin.Int
        public final val HIGH_INT: kotlin.Int
            public final fun <get-HIGH_INT>(): kotlin.Int
        public final val IMPLEMENTATION_COLOR_READ_FORMAT: kotlin.Int
            public final fun <get-IMPLEMENTATION_COLOR_READ_FORMAT>(): kotlin.Int
        public final val IMPLEMENTATION_COLOR_READ_TYPE: kotlin.Int
            public final fun <get-IMPLEMENTATION_COLOR_READ_TYPE>(): kotlin.Int
        public final val INCR: kotlin.Int
            public final fun <get-INCR>(): kotlin.Int
        public final val INCR_WRAP: kotlin.Int
            public final fun <get-INCR_WRAP>(): kotlin.Int
        public final val INT: kotlin.Int
            public final fun <get-INT>(): kotlin.Int
        public final val INT_VEC2: kotlin.Int
            public final fun <get-INT_VEC2>(): kotlin.Int
        public final val INT_VEC3: kotlin.Int
            public final fun <get-INT_VEC3>(): kotlin.Int
        public final val INT_VEC4: kotlin.Int
            public final fun <get-INT_VEC4>(): kotlin.Int
        public final val INVALID_ENUM: kotlin.Int
            public final fun <get-INVALID_ENUM>(): kotlin.Int
        public final val INVALID_FRAMEBUFFER_OPERATION: kotlin.Int
            public final fun <get-INVALID_FRAMEBUFFER_OPERATION>(): kotlin.Int
        public final val INVALID_OPERATION: kotlin.Int
            public final fun <get-INVALID_OPERATION>(): kotlin.Int
        public final val INVALID_VALUE: kotlin.Int
            public final fun <get-INVALID_VALUE>(): kotlin.Int
        public final val INVERT: kotlin.Int
            public final fun <get-INVERT>(): kotlin.Int
        public final val KEEP: kotlin.Int
            public final fun <get-KEEP>(): kotlin.Int
        public final val LEQUAL: kotlin.Int
            public final fun <get-LEQUAL>(): kotlin.Int
        public final val LESS: kotlin.Int
            public final fun <get-LESS>(): kotlin.Int
        public final val LINEAR: kotlin.Int
            public final fun <get-LINEAR>(): kotlin.Int
        public final val LINEAR_MIPMAP_LINEAR: kotlin.Int
            public final fun <get-LINEAR_MIPMAP_LINEAR>(): kotlin.Int
        public final val LINEAR_MIPMAP_NEAREST: kotlin.Int
            public final fun <get-LINEAR_MIPMAP_NEAREST>(): kotlin.Int
        public final val LINES: kotlin.Int
            public final fun <get-LINES>(): kotlin.Int
        public final val LINE_LOOP: kotlin.Int
            public final fun <get-LINE_LOOP>(): kotlin.Int
        public final val LINE_STRIP: kotlin.Int
            public final fun <get-LINE_STRIP>(): kotlin.Int
        public final val LINE_WIDTH: kotlin.Int
            public final fun <get-LINE_WIDTH>(): kotlin.Int
        public final val LINK_STATUS: kotlin.Int
            public final fun <get-LINK_STATUS>(): kotlin.Int
        public final val LOW_FLOAT: kotlin.Int
            public final fun <get-LOW_FLOAT>(): kotlin.Int
        public final val LOW_INT: kotlin.Int
            public final fun <get-LOW_INT>(): kotlin.Int
        public final val LUMINANCE: kotlin.Int
            public final fun <get-LUMINANCE>(): kotlin.Int
        public final val LUMINANCE_ALPHA: kotlin.Int
            public final fun <get-LUMINANCE_ALPHA>(): kotlin.Int
        public final val MAX_COMBINED_TEXTURE_IMAGE_UNITS: kotlin.Int
            public final fun <get-MAX_COMBINED_TEXTURE_IMAGE_UNITS>(): kotlin.Int
        public final val MAX_CUBE_MAP_TEXTURE_SIZE: kotlin.Int
            public final fun <get-MAX_CUBE_MAP_TEXTURE_SIZE>(): kotlin.Int
        public final val MAX_FRAGMENT_UNIFORM_VECTORS: kotlin.Int
            public final fun <get-MAX_FRAGMENT_UNIFORM_VECTORS>(): kotlin.Int
        public final val MAX_RENDERBUFFER_SIZE: kotlin.Int
            public final fun <get-MAX_RENDERBUFFER_SIZE>(): kotlin.Int
        public final val MAX_TEXTURE_IMAGE_UNITS: kotlin.Int
            public final fun <get-MAX_TEXTURE_IMAGE_UNITS>(): kotlin.Int
        public final val MAX_TEXTURE_SIZE: kotlin.Int
            public final fun <get-MAX_TEXTURE_SIZE>(): kotlin.Int
        public final val MAX_VARYING_VECTORS: kotlin.Int
            public final fun <get-MAX_VARYING_VECTORS>(): kotlin.Int
        public final val MAX_VERTEX_ATTRIBS: kotlin.Int
            public final fun <get-MAX_VERTEX_ATTRIBS>(): kotlin.Int
        public final val MAX_VERTEX_TEXTURE_IMAGE_UNITS: kotlin.Int
            public final fun <get-MAX_VERTEX_TEXTURE_IMAGE_UNITS>(): kotlin.Int
        public final val MAX_VERTEX_UNIFORM_VECTORS: kotlin.Int
            public final fun <get-MAX_VERTEX_UNIFORM_VECTORS>(): kotlin.Int
        public final val MAX_VIEWPORT_DIMS: kotlin.Int
            public final fun <get-MAX_VIEWPORT_DIMS>(): kotlin.Int
        public final val MEDIUM_FLOAT: kotlin.Int
            public final fun <get-MEDIUM_FLOAT>(): kotlin.Int
        public final val MEDIUM_INT: kotlin.Int
            public final fun <get-MEDIUM_INT>(): kotlin.Int
        public final val MIRRORED_REPEAT: kotlin.Int
            public final fun <get-MIRRORED_REPEAT>(): kotlin.Int
        public final val NEAREST: kotlin.Int
            public final fun <get-NEAREST>(): kotlin.Int
        public final val NEAREST_MIPMAP_LINEAR: kotlin.Int
            public final fun <get-NEAREST_MIPMAP_LINEAR>(): kotlin.Int
        public final val NEAREST_MIPMAP_NEAREST: kotlin.Int
            public final fun <get-NEAREST_MIPMAP_NEAREST>(): kotlin.Int
        public final val NEVER: kotlin.Int
            public final fun <get-NEVER>(): kotlin.Int
        public final val NICEST: kotlin.Int
            public final fun <get-NICEST>(): kotlin.Int
        public final val NONE: kotlin.Int
            public final fun <get-NONE>(): kotlin.Int
        public final val NOTEQUAL: kotlin.Int
            public final fun <get-NOTEQUAL>(): kotlin.Int
        public final val NO_ERROR: kotlin.Int
            public final fun <get-NO_ERROR>(): kotlin.Int
        public final val ONE: kotlin.Int
            public final fun <get-ONE>(): kotlin.Int
        public final val ONE_MINUS_CONSTANT_ALPHA: kotlin.Int
            public final fun <get-ONE_MINUS_CONSTANT_ALPHA>(): kotlin.Int
        public final val ONE_MINUS_CONSTANT_COLOR: kotlin.Int
            public final fun <get-ONE_MINUS_CONSTANT_COLOR>(): kotlin.Int
        public final val ONE_MINUS_DST_ALPHA: kotlin.Int
            public final fun <get-ONE_MINUS_DST_ALPHA>(): kotlin.Int
        public final val ONE_MINUS_DST_COLOR: kotlin.Int
            public final fun <get-ONE_MINUS_DST_COLOR>(): kotlin.Int
        public final val ONE_MINUS_SRC_ALPHA: kotlin.Int
            public final fun <get-ONE_MINUS_SRC_ALPHA>(): kotlin.Int
        public final val ONE_MINUS_SRC_COLOR: kotlin.Int
            public final fun <get-ONE_MINUS_SRC_COLOR>(): kotlin.Int
        public final val OUT_OF_MEMORY: kotlin.Int
            public final fun <get-OUT_OF_MEMORY>(): kotlin.Int
        public final val PACK_ALIGNMENT: kotlin.Int
            public final fun <get-PACK_ALIGNMENT>(): kotlin.Int
        public final val POINTS: kotlin.Int
            public final fun <get-POINTS>(): kotlin.Int
        public final val POLYGON_OFFSET_FACTOR: kotlin.Int
            public final fun <get-POLYGON_OFFSET_FACTOR>(): kotlin.Int
        public final val POLYGON_OFFSET_FILL: kotlin.Int
            public final fun <get-POLYGON_OFFSET_FILL>(): kotlin.Int
        public final val POLYGON_OFFSET_UNITS: kotlin.Int
            public final fun <get-POLYGON_OFFSET_UNITS>(): kotlin.Int
        public final val RED_BITS: kotlin.Int
            public final fun <get-RED_BITS>(): kotlin.Int
        public final val RENDERBUFFER: kotlin.Int
            public final fun <get-RENDERBUFFER>(): kotlin.Int
        public final val RENDERBUFFER_ALPHA_SIZE: kotlin.Int
            public final fun <get-RENDERBUFFER_ALPHA_SIZE>(): kotlin.Int
        public final val RENDERBUFFER_BINDING: kotlin.Int
            public final fun <get-RENDERBUFFER_BINDING>(): kotlin.Int
        public final val RENDERBUFFER_BLUE_SIZE: kotlin.Int
            public final fun <get-RENDERBUFFER_BLUE_SIZE>(): kotlin.Int
        public final val RENDERBUFFER_DEPTH_SIZE: kotlin.Int
            public final fun <get-RENDERBUFFER_DEPTH_SIZE>(): kotlin.Int
        public final val RENDERBUFFER_GREEN_SIZE: kotlin.Int
            public final fun <get-RENDERBUFFER_GREEN_SIZE>(): kotlin.Int
        public final val RENDERBUFFER_HEIGHT: kotlin.Int
            public final fun <get-RENDERBUFFER_HEIGHT>(): kotlin.Int
        public final val RENDERBUFFER_INTERNAL_FORMAT: kotlin.Int
            public final fun <get-RENDERBUFFER_INTERNAL_FORMAT>(): kotlin.Int
        public final val RENDERBUFFER_RED_SIZE: kotlin.Int
            public final fun <get-RENDERBUFFER_RED_SIZE>(): kotlin.Int
        public final val RENDERBUFFER_STENCIL_SIZE: kotlin.Int
            public final fun <get-RENDERBUFFER_STENCIL_SIZE>(): kotlin.Int
        public final val RENDERBUFFER_WIDTH: kotlin.Int
            public final fun <get-RENDERBUFFER_WIDTH>(): kotlin.Int
        public final val RENDERER: kotlin.Int
            public final fun <get-RENDERER>(): kotlin.Int
        public final val REPEAT: kotlin.Int
            public final fun <get-REPEAT>(): kotlin.Int
        public final val REPLACE: kotlin.Int
            public final fun <get-REPLACE>(): kotlin.Int
        public final val RGB: kotlin.Int
            public final fun <get-RGB>(): kotlin.Int
        public final val RGB565: kotlin.Int
            public final fun <get-RGB565>(): kotlin.Int
        public final val RGB5_A1: kotlin.Int
            public final fun <get-RGB5_A1>(): kotlin.Int
        public final val RGBA: kotlin.Int
            public final fun <get-RGBA>(): kotlin.Int
        public final val RGBA4: kotlin.Int
            public final fun <get-RGBA4>(): kotlin.Int
        public final val SAMPLER_2D: kotlin.Int
            public final fun <get-SAMPLER_2D>(): kotlin.Int
        public final val SAMPLER_CUBE: kotlin.Int
            public final fun <get-SAMPLER_CUBE>(): kotlin.Int
        public final val SAMPLES: kotlin.Int
            public final fun <get-SAMPLES>(): kotlin.Int
        public final val SAMPLE_ALPHA_TO_COVERAGE: kotlin.Int
            public final fun <get-SAMPLE_ALPHA_TO_COVERAGE>(): kotlin.Int
        public final val SAMPLE_BUFFERS: kotlin.Int
            public final fun <get-SAMPLE_BUFFERS>(): kotlin.Int
        public final val SAMPLE_COVERAGE: kotlin.Int
            public final fun <get-SAMPLE_COVERAGE>(): kotlin.Int
        public final val SAMPLE_COVERAGE_INVERT: kotlin.Int
            public final fun <get-SAMPLE_COVERAGE_INVERT>(): kotlin.Int
        public final val SAMPLE_COVERAGE_VALUE: kotlin.Int
            public final fun <get-SAMPLE_COVERAGE_VALUE>(): kotlin.Int
        public final val SCISSOR_BOX: kotlin.Int
            public final fun <get-SCISSOR_BOX>(): kotlin.Int
        public final val SCISSOR_TEST: kotlin.Int
            public final fun <get-SCISSOR_TEST>(): kotlin.Int
        public final val SHADER_TYPE: kotlin.Int
            public final fun <get-SHADER_TYPE>(): kotlin.Int
        public final val SHADING_LANGUAGE_VERSION: kotlin.Int
            public final fun <get-SHADING_LANGUAGE_VERSION>(): kotlin.Int
        public final val SHORT: kotlin.Int
            public final fun <get-SHORT>(): kotlin.Int
        public final val SRC_ALPHA: kotlin.Int
            public final fun <get-SRC_ALPHA>(): kotlin.Int
        public final val SRC_ALPHA_SATURATE: kotlin.Int
            public final fun <get-SRC_ALPHA_SATURATE>(): kotlin.Int
        public final val SRC_COLOR: kotlin.Int
            public final fun <get-SRC_COLOR>(): kotlin.Int
        public final val STATIC_DRAW: kotlin.Int
            public final fun <get-STATIC_DRAW>(): kotlin.Int
        public final val STENCIL_ATTACHMENT: kotlin.Int
            public final fun <get-STENCIL_ATTACHMENT>(): kotlin.Int
        public final val STENCIL_BACK_FAIL: kotlin.Int
            public final fun <get-STENCIL_BACK_FAIL>(): kotlin.Int
        public final val STENCIL_BACK_FUNC: kotlin.Int
            public final fun <get-STENCIL_BACK_FUNC>(): kotlin.Int
        public final val STENCIL_BACK_PASS_DEPTH_FAIL: kotlin.Int
            public final fun <get-STENCIL_BACK_PASS_DEPTH_FAIL>(): kotlin.Int
        public final val STENCIL_BACK_PASS_DEPTH_PASS: kotlin.Int
            public final fun <get-STENCIL_BACK_PASS_DEPTH_PASS>(): kotlin.Int
        public final val STENCIL_BACK_REF: kotlin.Int
            public final fun <get-STENCIL_BACK_REF>(): kotlin.Int
        public final val STENCIL_BACK_VALUE_MASK: kotlin.Int
            public final fun <get-STENCIL_BACK_VALUE_MASK>(): kotlin.Int
        public final val STENCIL_BACK_WRITEMASK: kotlin.Int
            public final fun <get-STENCIL_BACK_WRITEMASK>(): kotlin.Int
        public final val STENCIL_BITS: kotlin.Int
            public final fun <get-STENCIL_BITS>(): kotlin.Int
        public final val STENCIL_BUFFER_BIT: kotlin.Int
            public final fun <get-STENCIL_BUFFER_BIT>(): kotlin.Int
        public final val STENCIL_CLEAR_VALUE: kotlin.Int
            public final fun <get-STENCIL_CLEAR_VALUE>(): kotlin.Int
        public final val STENCIL_FAIL: kotlin.Int
            public final fun <get-STENCIL_FAIL>(): kotlin.Int
        public final val STENCIL_FUNC: kotlin.Int
            public final fun <get-STENCIL_FUNC>(): kotlin.Int
        public final val STENCIL_INDEX: kotlin.Int
            public final fun <get-STENCIL_INDEX>(): kotlin.Int
        public final val STENCIL_INDEX8: kotlin.Int
            public final fun <get-STENCIL_INDEX8>(): kotlin.Int
        public final val STENCIL_PASS_DEPTH_FAIL: kotlin.Int
            public final fun <get-STENCIL_PASS_DEPTH_FAIL>(): kotlin.Int
        public final val STENCIL_PASS_DEPTH_PASS: kotlin.Int
            public final fun <get-STENCIL_PASS_DEPTH_PASS>(): kotlin.Int
        public final val STENCIL_REF: kotlin.Int
            public final fun <get-STENCIL_REF>(): kotlin.Int
        public final val STENCIL_TEST: kotlin.Int
            public final fun <get-STENCIL_TEST>(): kotlin.Int
        public final val STENCIL_VALUE_MASK: kotlin.Int
            public final fun <get-STENCIL_VALUE_MASK>(): kotlin.Int
        public final val STENCIL_WRITEMASK: kotlin.Int
            public final fun <get-STENCIL_WRITEMASK>(): kotlin.Int
        public final val STREAM_DRAW: kotlin.Int
            public final fun <get-STREAM_DRAW>(): kotlin.Int
        public final val SUBPIXEL_BITS: kotlin.Int
            public final fun <get-SUBPIXEL_BITS>(): kotlin.Int
        public final val TEXTURE: kotlin.Int
            public final fun <get-TEXTURE>(): kotlin.Int
        public final val TEXTURE0: kotlin.Int
            public final fun <get-TEXTURE0>(): kotlin.Int
        public final val TEXTURE1: kotlin.Int
            public final fun <get-TEXTURE1>(): kotlin.Int
        public final val TEXTURE10: kotlin.Int
            public final fun <get-TEXTURE10>(): kotlin.Int
        public final val TEXTURE11: kotlin.Int
            public final fun <get-TEXTURE11>(): kotlin.Int
        public final val TEXTURE12: kotlin.Int
            public final fun <get-TEXTURE12>(): kotlin.Int
        public final val TEXTURE13: kotlin.Int
            public final fun <get-TEXTURE13>(): kotlin.Int
        public final val TEXTURE14: kotlin.Int
            public final fun <get-TEXTURE14>(): kotlin.Int
        public final val TEXTURE15: kotlin.Int
            public final fun <get-TEXTURE15>(): kotlin.Int
        public final val TEXTURE16: kotlin.Int
            public final fun <get-TEXTURE16>(): kotlin.Int
        public final val TEXTURE17: kotlin.Int
            public final fun <get-TEXTURE17>(): kotlin.Int
        public final val TEXTURE18: kotlin.Int
            public final fun <get-TEXTURE18>(): kotlin.Int
        public final val TEXTURE19: kotlin.Int
            public final fun <get-TEXTURE19>(): kotlin.Int
        public final val TEXTURE2: kotlin.Int
            public final fun <get-TEXTURE2>(): kotlin.Int
        public final val TEXTURE20: kotlin.Int
            public final fun <get-TEXTURE20>(): kotlin.Int
        public final val TEXTURE21: kotlin.Int
            public final fun <get-TEXTURE21>(): kotlin.Int
        public final val TEXTURE22: kotlin.Int
            public final fun <get-TEXTURE22>(): kotlin.Int
        public final val TEXTURE23: kotlin.Int
            public final fun <get-TEXTURE23>(): kotlin.Int
        public final val TEXTURE24: kotlin.Int
            public final fun <get-TEXTURE24>(): kotlin.Int
        public final val TEXTURE25: kotlin.Int
            public final fun <get-TEXTURE25>(): kotlin.Int
        public final val TEXTURE26: kotlin.Int
            public final fun <get-TEXTURE26>(): kotlin.Int
        public final val TEXTURE27: kotlin.Int
            public final fun <get-TEXTURE27>(): kotlin.Int
        public final val TEXTURE28: kotlin.Int
            public final fun <get-TEXTURE28>(): kotlin.Int
        public final val TEXTURE29: kotlin.Int
            public final fun <get-TEXTURE29>(): kotlin.Int
        public final val TEXTURE3: kotlin.Int
            public final fun <get-TEXTURE3>(): kotlin.Int
        public final val TEXTURE30: kotlin.Int
            public final fun <get-TEXTURE30>(): kotlin.Int
        public final val TEXTURE31: kotlin.Int
            public final fun <get-TEXTURE31>(): kotlin.Int
        public final val TEXTURE4: kotlin.Int
            public final fun <get-TEXTURE4>(): kotlin.Int
        public final val TEXTURE5: kotlin.Int
            public final fun <get-TEXTURE5>(): kotlin.Int
        public final val TEXTURE6: kotlin.Int
            public final fun <get-TEXTURE6>(): kotlin.Int
        public final val TEXTURE7: kotlin.Int
            public final fun <get-TEXTURE7>(): kotlin.Int
        public final val TEXTURE8: kotlin.Int
            public final fun <get-TEXTURE8>(): kotlin.Int
        public final val TEXTURE9: kotlin.Int
            public final fun <get-TEXTURE9>(): kotlin.Int
        public final val TEXTURE_2D: kotlin.Int
            public final fun <get-TEXTURE_2D>(): kotlin.Int
        public final val TEXTURE_BINDING_2D: kotlin.Int
            public final fun <get-TEXTURE_BINDING_2D>(): kotlin.Int
        public final val TEXTURE_BINDING_CUBE_MAP: kotlin.Int
            public final fun <get-TEXTURE_BINDING_CUBE_MAP>(): kotlin.Int
        public final val TEXTURE_CUBE_MAP: kotlin.Int
            public final fun <get-TEXTURE_CUBE_MAP>(): kotlin.Int
        public final val TEXTURE_CUBE_MAP_NEGATIVE_X: kotlin.Int
            public final fun <get-TEXTURE_CUBE_MAP_NEGATIVE_X>(): kotlin.Int
        public final val TEXTURE_CUBE_MAP_NEGATIVE_Y: kotlin.Int
            public final fun <get-TEXTURE_CUBE_MAP_NEGATIVE_Y>(): kotlin.Int
        public final val TEXTURE_CUBE_MAP_NEGATIVE_Z: kotlin.Int
            public final fun <get-TEXTURE_CUBE_MAP_NEGATIVE_Z>(): kotlin.Int
        public final val TEXTURE_CUBE_MAP_POSITIVE_X: kotlin.Int
            public final fun <get-TEXTURE_CUBE_MAP_POSITIVE_X>(): kotlin.Int
        public final val TEXTURE_CUBE_MAP_POSITIVE_Y: kotlin.Int
            public final fun <get-TEXTURE_CUBE_MAP_POSITIVE_Y>(): kotlin.Int
        public final val TEXTURE_CUBE_MAP_POSITIVE_Z: kotlin.Int
            public final fun <get-TEXTURE_CUBE_MAP_POSITIVE_Z>(): kotlin.Int
        public final val TEXTURE_MAG_FILTER: kotlin.Int
            public final fun <get-TEXTURE_MAG_FILTER>(): kotlin.Int
        public final val TEXTURE_MIN_FILTER: kotlin.Int
            public final fun <get-TEXTURE_MIN_FILTER>(): kotlin.Int
        public final val TEXTURE_WRAP_S: kotlin.Int
            public final fun <get-TEXTURE_WRAP_S>(): kotlin.Int
        public final val TEXTURE_WRAP_T: kotlin.Int
            public final fun <get-TEXTURE_WRAP_T>(): kotlin.Int
        public final val TRIANGLES: kotlin.Int
            public final fun <get-TRIANGLES>(): kotlin.Int
        public final val TRIANGLE_FAN: kotlin.Int
            public final fun <get-TRIANGLE_FAN>(): kotlin.Int
        public final val TRIANGLE_STRIP: kotlin.Int
            public final fun <get-TRIANGLE_STRIP>(): kotlin.Int
        public final val UNPACK_ALIGNMENT: kotlin.Int
            public final fun <get-UNPACK_ALIGNMENT>(): kotlin.Int
        public final val UNPACK_COLORSPACE_CONVERSION_WEBGL: kotlin.Int
            public final fun <get-UNPACK_COLORSPACE_CONVERSION_WEBGL>(): kotlin.Int
        public final val UNPACK_FLIP_Y_WEBGL: kotlin.Int
            public final fun <get-UNPACK_FLIP_Y_WEBGL>(): kotlin.Int
        public final val UNPACK_PREMULTIPLY_ALPHA_WEBGL: kotlin.Int
            public final fun <get-UNPACK_PREMULTIPLY_ALPHA_WEBGL>(): kotlin.Int
        public final val UNSIGNED_BYTE: kotlin.Int
            public final fun <get-UNSIGNED_BYTE>(): kotlin.Int
        public final val UNSIGNED_INT: kotlin.Int
            public final fun <get-UNSIGNED_INT>(): kotlin.Int
        public final val UNSIGNED_SHORT: kotlin.Int
            public final fun <get-UNSIGNED_SHORT>(): kotlin.Int
        public final val UNSIGNED_SHORT_4_4_4_4: kotlin.Int
            public final fun <get-UNSIGNED_SHORT_4_4_4_4>(): kotlin.Int
        public final val UNSIGNED_SHORT_5_5_5_1: kotlin.Int
            public final fun <get-UNSIGNED_SHORT_5_5_5_1>(): kotlin.Int
        public final val UNSIGNED_SHORT_5_6_5: kotlin.Int
            public final fun <get-UNSIGNED_SHORT_5_6_5>(): kotlin.Int
        public final val VALIDATE_STATUS: kotlin.Int
            public final fun <get-VALIDATE_STATUS>(): kotlin.Int
        public final val VENDOR: kotlin.Int
            public final fun <get-VENDOR>(): kotlin.Int
        public final val VERSION: kotlin.Int
            public final fun <get-VERSION>(): kotlin.Int
        public final val VERTEX_ATTRIB_ARRAY_BUFFER_BINDING: kotlin.Int
            public final fun <get-VERTEX_ATTRIB_ARRAY_BUFFER_BINDING>(): kotlin.Int
        public final val VERTEX_ATTRIB_ARRAY_ENABLED: kotlin.Int
            public final fun <get-VERTEX_ATTRIB_ARRAY_ENABLED>(): kotlin.Int
        public final val VERTEX_ATTRIB_ARRAY_NORMALIZED: kotlin.Int
            public final fun <get-VERTEX_ATTRIB_ARRAY_NORMALIZED>(): kotlin.Int
        public final val VERTEX_ATTRIB_ARRAY_POINTER: kotlin.Int
            public final fun <get-VERTEX_ATTRIB_ARRAY_POINTER>(): kotlin.Int
        public final val VERTEX_ATTRIB_ARRAY_SIZE: kotlin.Int
            public final fun <get-VERTEX_ATTRIB_ARRAY_SIZE>(): kotlin.Int
        public final val VERTEX_ATTRIB_ARRAY_STRIDE: kotlin.Int
            public final fun <get-VERTEX_ATTRIB_ARRAY_STRIDE>(): kotlin.Int
        public final val VERTEX_ATTRIB_ARRAY_TYPE: kotlin.Int
            public final fun <get-VERTEX_ATTRIB_ARRAY_TYPE>(): kotlin.Int
        public final val VERTEX_SHADER: kotlin.Int
            public final fun <get-VERTEX_SHADER>(): kotlin.Int
        public final val VIEWPORT: kotlin.Int
            public final fun <get-VIEWPORT>(): kotlin.Int
        public final val ZERO: kotlin.Int
            public final fun <get-ZERO>(): kotlin.Int
    }
}
