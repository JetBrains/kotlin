/*
 * Generated file
 * DO NOT EDIT
 * 
 * See libraries/tools/idl2k for details
 */

package org.khronos.webgl

import org.w3c.dom.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.parsing.*
import org.w3c.dom.svg.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

native public open class WebGLContextAttributes {
    var alpha: Boolean = true
    var depth: Boolean = true
    var stencil: Boolean = false
    var antialias: Boolean = true
    var premultipliedAlpha: Boolean = true
    var preserveDrawingBuffer: Boolean = false
    var preferLowPowerToHighPerformance: Boolean = false
    var failIfMajorPerformanceCaveat: Boolean = false
}

native public trait WebGLObject {
}

native public trait WebGLBuffer : WebGLObject {
}

native public trait WebGLFramebuffer : WebGLObject {
}

native public trait WebGLProgram : WebGLObject {
}

native public trait WebGLRenderbuffer : WebGLObject {
}

native public trait WebGLShader : WebGLObject {
}

native public trait WebGLTexture : WebGLObject {
}

native public trait WebGLUniformLocation {
}

native public trait WebGLActiveInfo {
    var size: Int
        get() = noImpl
        set(value) = noImpl
    var type: Int
        get() = noImpl
        set(value) = noImpl
    var name: String
        get() = noImpl
        set(value) = noImpl
}

native public trait WebGLShaderPrecisionFormat {
    var rangeMin: Int
        get() = noImpl
        set(value) = noImpl
    var rangeMax: Int
        get() = noImpl
        set(value) = noImpl
    var precision: Int
        get() = noImpl
        set(value) = noImpl
}

native public trait WebGLRenderingContext : RenderingContext {
    var canvas: HTMLCanvasElement
        get() = noImpl
        set(value) = noImpl
    var drawingBufferWidth: Int
        get() = noImpl
        set(value) = noImpl
    var drawingBufferHeight: Int
        get() = noImpl
        set(value) = noImpl
    fun getContextAttributes(): WebGLContextAttributes? = noImpl
    fun isContextLost(): Boolean = noImpl
    fun getSupportedExtensions(): Array<dynamic> = noImpl
    fun getExtension(name: String): dynamic = noImpl
    fun activeTexture(texture: Int): Unit = noImpl
    fun attachShader(program: WebGLProgram?, shader: WebGLShader?): Unit = noImpl
    fun bindAttribLocation(program: WebGLProgram?, index: Int, name: String): Unit = noImpl
    fun bindBuffer(target: Int, buffer: WebGLBuffer?): Unit = noImpl
    fun bindFramebuffer(target: Int, framebuffer: WebGLFramebuffer?): Unit = noImpl
    fun bindRenderbuffer(target: Int, renderbuffer: WebGLRenderbuffer?): Unit = noImpl
    fun bindTexture(target: Int, texture: WebGLTexture?): Unit = noImpl
    fun blendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = noImpl
    fun blendEquation(mode: Int): Unit = noImpl
    fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit = noImpl
    fun blendFunc(sfactor: Int, dfactor: Int): Unit = noImpl
    fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int): Unit = noImpl
    fun bufferData(target: Int, size: Long, usage: Int): Unit = noImpl
    fun bufferData(target: Int, data: dynamic, usage: Int): Unit = noImpl
    fun bufferSubData(target: Int, offset: Long, data: dynamic): Unit = noImpl
    fun checkFramebufferStatus(target: Int): Int = noImpl
    fun clear(mask: Int): Unit = noImpl
    fun clearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit = noImpl
    fun clearDepth(depth: Float): Unit = noImpl
    fun clearStencil(s: Int): Unit = noImpl
    fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit = noImpl
    fun compileShader(shader: WebGLShader?): Unit = noImpl
    fun compressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, data: ArrayBufferView): Unit = noImpl
    fun compressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, data: ArrayBufferView): Unit = noImpl
    fun copyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit = noImpl
    fun copyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit = noImpl
    fun createBuffer(): WebGLBuffer? = noImpl
    fun createFramebuffer(): WebGLFramebuffer? = noImpl
    fun createProgram(): WebGLProgram? = noImpl
    fun createRenderbuffer(): WebGLRenderbuffer? = noImpl
    fun createShader(type: Int): WebGLShader? = noImpl
    fun createTexture(): WebGLTexture? = noImpl
    fun cullFace(mode: Int): Unit = noImpl
    fun deleteBuffer(buffer: WebGLBuffer?): Unit = noImpl
    fun deleteFramebuffer(framebuffer: WebGLFramebuffer?): Unit = noImpl
    fun deleteProgram(program: WebGLProgram?): Unit = noImpl
    fun deleteRenderbuffer(renderbuffer: WebGLRenderbuffer?): Unit = noImpl
    fun deleteShader(shader: WebGLShader?): Unit = noImpl
    fun deleteTexture(texture: WebGLTexture?): Unit = noImpl
    fun depthFunc(func: Int): Unit = noImpl
    fun depthMask(flag: Boolean): Unit = noImpl
    fun depthRange(zNear: Float, zFar: Float): Unit = noImpl
    fun detachShader(program: WebGLProgram?, shader: WebGLShader?): Unit = noImpl
    fun disable(cap: Int): Unit = noImpl
    fun disableVertexAttribArray(index: Int): Unit = noImpl
    fun drawArrays(mode: Int, first: Int, count: Int): Unit = noImpl
    fun drawElements(mode: Int, count: Int, type: Int, offset: Long): Unit = noImpl
    fun enable(cap: Int): Unit = noImpl
    fun enableVertexAttribArray(index: Int): Unit = noImpl
    fun finish(): Unit = noImpl
    fun flush(): Unit = noImpl
    fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: WebGLRenderbuffer?): Unit = noImpl
    fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: WebGLTexture?, level: Int): Unit = noImpl
    fun frontFace(mode: Int): Unit = noImpl
    fun generateMipmap(target: Int): Unit = noImpl
    fun getActiveAttrib(program: WebGLProgram?, index: Int): WebGLActiveInfo? = noImpl
    fun getActiveUniform(program: WebGLProgram?, index: Int): WebGLActiveInfo? = noImpl
    fun getAttachedShaders(program: WebGLProgram?): Array<dynamic> = noImpl
    fun getAttribLocation(program: WebGLProgram?, name: String): Int = noImpl
    fun getBufferParameter(target: Int, pname: Int): Any? = noImpl
    fun getParameter(pname: Int): Any? = noImpl
    fun getError(): Int = noImpl
    fun getFramebufferAttachmentParameter(target: Int, attachment: Int, pname: Int): Any? = noImpl
    fun getProgramParameter(program: WebGLProgram?, pname: Int): Any? = noImpl
    fun getProgramInfoLog(program: WebGLProgram?): String? = noImpl
    fun getRenderbufferParameter(target: Int, pname: Int): Any? = noImpl
    fun getShaderParameter(shader: WebGLShader?, pname: Int): Any? = noImpl
    fun getShaderPrecisionFormat(shadertype: Int, precisiontype: Int): WebGLShaderPrecisionFormat? = noImpl
    fun getShaderInfoLog(shader: WebGLShader?): String? = noImpl
    fun getShaderSource(shader: WebGLShader?): String? = noImpl
    fun getTexParameter(target: Int, pname: Int): Any? = noImpl
    fun getUniform(program: WebGLProgram?, location: WebGLUniformLocation?): Any? = noImpl
    fun getUniformLocation(program: WebGLProgram?, name: String): WebGLUniformLocation? = noImpl
    fun getVertexAttrib(index: Int, pname: Int): Any? = noImpl
    fun getVertexAttribOffset(index: Int, pname: Int): Long = noImpl
    fun hint(target: Int, mode: Int): Unit = noImpl
    fun isBuffer(buffer: WebGLBuffer?): Boolean = noImpl
    fun isEnabled(cap: Int): Boolean = noImpl
    fun isFramebuffer(framebuffer: WebGLFramebuffer?): Boolean = noImpl
    fun isProgram(program: WebGLProgram?): Boolean = noImpl
    fun isRenderbuffer(renderbuffer: WebGLRenderbuffer?): Boolean = noImpl
    fun isShader(shader: WebGLShader?): Boolean = noImpl
    fun isTexture(texture: WebGLTexture?): Boolean = noImpl
    fun lineWidth(width: Float): Unit = noImpl
    fun linkProgram(program: WebGLProgram?): Unit = noImpl
    fun pixelStorei(pname: Int, param: Int): Unit = noImpl
    fun polygonOffset(factor: Float, units: Float): Unit = noImpl
    fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: ArrayBufferView?): Unit = noImpl
    fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit = noImpl
    fun sampleCoverage(value: Float, invert: Boolean): Unit = noImpl
    fun scissor(x: Int, y: Int, width: Int, height: Int): Unit = noImpl
    fun shaderSource(shader: WebGLShader?, source: String): Unit = noImpl
    fun stencilFunc(func: Int, ref: Int, mask: Int): Unit = noImpl
    fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit = noImpl
    fun stencilMask(mask: Int): Unit = noImpl
    fun stencilMaskSeparate(face: Int, mask: Int): Unit = noImpl
    fun stencilOp(fail: Int, zfail: Int, zpass: Int): Unit = noImpl
    fun stencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int): Unit = noImpl
    fun texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: ArrayBufferView?): Unit = noImpl
    fun texImage2D(target: Int, level: Int, internalformat: Int, format: Int, type: Int, source: dynamic): Unit = noImpl
    fun texParameterf(target: Int, pname: Int, param: Float): Unit = noImpl
    fun texParameteri(target: Int, pname: Int, param: Int): Unit = noImpl
    fun texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, pixels: ArrayBufferView?): Unit = noImpl
    fun texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, format: Int, type: Int, source: dynamic): Unit = noImpl
    fun uniform1f(location: WebGLUniformLocation?, x: Float): Unit = noImpl
    fun uniform1fv(location: WebGLUniformLocation?, v: Float32Array): Unit = noImpl
    fun uniform1fv(location: WebGLUniformLocation?, v: Array<Float>): Unit = noImpl
    fun uniform1i(location: WebGLUniformLocation?, x: Int): Unit = noImpl
    fun uniform1iv(location: WebGLUniformLocation?, v: Int32Array): Unit = noImpl
    fun uniform1iv(location: WebGLUniformLocation?, v: Array<Int>): Unit = noImpl
    fun uniform2f(location: WebGLUniformLocation?, x: Float, y: Float): Unit = noImpl
    fun uniform2fv(location: WebGLUniformLocation?, v: Float32Array): Unit = noImpl
    fun uniform2fv(location: WebGLUniformLocation?, v: Array<Float>): Unit = noImpl
    fun uniform2i(location: WebGLUniformLocation?, x: Int, y: Int): Unit = noImpl
    fun uniform2iv(location: WebGLUniformLocation?, v: Int32Array): Unit = noImpl
    fun uniform2iv(location: WebGLUniformLocation?, v: Array<Int>): Unit = noImpl
    fun uniform3f(location: WebGLUniformLocation?, x: Float, y: Float, z: Float): Unit = noImpl
    fun uniform3fv(location: WebGLUniformLocation?, v: Float32Array): Unit = noImpl
    fun uniform3fv(location: WebGLUniformLocation?, v: Array<Float>): Unit = noImpl
    fun uniform3i(location: WebGLUniformLocation?, x: Int, y: Int, z: Int): Unit = noImpl
    fun uniform3iv(location: WebGLUniformLocation?, v: Int32Array): Unit = noImpl
    fun uniform3iv(location: WebGLUniformLocation?, v: Array<Int>): Unit = noImpl
    fun uniform4f(location: WebGLUniformLocation?, x: Float, y: Float, z: Float, w: Float): Unit = noImpl
    fun uniform4fv(location: WebGLUniformLocation?, v: Float32Array): Unit = noImpl
    fun uniform4fv(location: WebGLUniformLocation?, v: Array<Float>): Unit = noImpl
    fun uniform4i(location: WebGLUniformLocation?, x: Int, y: Int, z: Int, w: Int): Unit = noImpl
    fun uniform4iv(location: WebGLUniformLocation?, v: Int32Array): Unit = noImpl
    fun uniform4iv(location: WebGLUniformLocation?, v: Array<Int>): Unit = noImpl
    fun uniformMatrix2fv(location: WebGLUniformLocation?, transpose: Boolean, value: Float32Array): Unit = noImpl
    fun uniformMatrix2fv(location: WebGLUniformLocation?, transpose: Boolean, value: Array<Float>): Unit = noImpl
    fun uniformMatrix3fv(location: WebGLUniformLocation?, transpose: Boolean, value: Float32Array): Unit = noImpl
    fun uniformMatrix3fv(location: WebGLUniformLocation?, transpose: Boolean, value: Array<Float>): Unit = noImpl
    fun uniformMatrix4fv(location: WebGLUniformLocation?, transpose: Boolean, value: Float32Array): Unit = noImpl
    fun uniformMatrix4fv(location: WebGLUniformLocation?, transpose: Boolean, value: Array<Float>): Unit = noImpl
    fun useProgram(program: WebGLProgram?): Unit = noImpl
    fun validateProgram(program: WebGLProgram?): Unit = noImpl
    fun vertexAttrib1f(indx: Int, x: Float): Unit = noImpl
    fun vertexAttrib1fv(indx: Int, values: Float32Array): Unit = noImpl
    fun vertexAttrib1fv(indx: Int, values: Array<Float>): Unit = noImpl
    fun vertexAttrib2f(indx: Int, x: Float, y: Float): Unit = noImpl
    fun vertexAttrib2fv(indx: Int, values: Float32Array): Unit = noImpl
    fun vertexAttrib2fv(indx: Int, values: Array<Float>): Unit = noImpl
    fun vertexAttrib3f(indx: Int, x: Float, y: Float, z: Float): Unit = noImpl
    fun vertexAttrib3fv(indx: Int, values: Float32Array): Unit = noImpl
    fun vertexAttrib3fv(indx: Int, values: Array<Float>): Unit = noImpl
    fun vertexAttrib4f(indx: Int, x: Float, y: Float, z: Float, w: Float): Unit = noImpl
    fun vertexAttrib4fv(indx: Int, values: Float32Array): Unit = noImpl
    fun vertexAttrib4fv(indx: Int, values: Array<Float>): Unit = noImpl
    fun vertexAttribPointer(indx: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Long): Unit = noImpl
    fun viewport(x: Int, y: Int, width: Int, height: Int): Unit = noImpl

    companion object {
        val DEPTH_BUFFER_BIT: Int = 0x00000100
        val STENCIL_BUFFER_BIT: Int = 0x00000400
        val COLOR_BUFFER_BIT: Int = 0x00004000
        val POINTS: Int = 0x0000
        val LINES: Int = 0x0001
        val LINE_LOOP: Int = 0x0002
        val LINE_STRIP: Int = 0x0003
        val TRIANGLES: Int = 0x0004
        val TRIANGLE_STRIP: Int = 0x0005
        val TRIANGLE_FAN: Int = 0x0006
        val ZERO: Int = 0
        val ONE: Int = 1
        val SRC_COLOR: Int = 0x0300
        val ONE_MINUS_SRC_COLOR: Int = 0x0301
        val SRC_ALPHA: Int = 0x0302
        val ONE_MINUS_SRC_ALPHA: Int = 0x0303
        val DST_ALPHA: Int = 0x0304
        val ONE_MINUS_DST_ALPHA: Int = 0x0305
        val DST_COLOR: Int = 0x0306
        val ONE_MINUS_DST_COLOR: Int = 0x0307
        val SRC_ALPHA_SATURATE: Int = 0x0308
        val FUNC_ADD: Int = 0x8006
        val BLEND_EQUATION: Int = 0x8009
        val BLEND_EQUATION_RGB: Int = 0x8009
        val BLEND_EQUATION_ALPHA: Int = 0x883D
        val FUNC_SUBTRACT: Int = 0x800A
        val FUNC_REVERSE_SUBTRACT: Int = 0x800B
        val BLEND_DST_RGB: Int = 0x80C8
        val BLEND_SRC_RGB: Int = 0x80C9
        val BLEND_DST_ALPHA: Int = 0x80CA
        val BLEND_SRC_ALPHA: Int = 0x80CB
        val CONSTANT_COLOR: Int = 0x8001
        val ONE_MINUS_CONSTANT_COLOR: Int = 0x8002
        val CONSTANT_ALPHA: Int = 0x8003
        val ONE_MINUS_CONSTANT_ALPHA: Int = 0x8004
        val BLEND_COLOR: Int = 0x8005
        val ARRAY_BUFFER: Int = 0x8892
        val ELEMENT_ARRAY_BUFFER: Int = 0x8893
        val ARRAY_BUFFER_BINDING: Int = 0x8894
        val ELEMENT_ARRAY_BUFFER_BINDING: Int = 0x8895
        val STREAM_DRAW: Int = 0x88E0
        val STATIC_DRAW: Int = 0x88E4
        val DYNAMIC_DRAW: Int = 0x88E8
        val BUFFER_SIZE: Int = 0x8764
        val BUFFER_USAGE: Int = 0x8765
        val CURRENT_VERTEX_ATTRIB: Int = 0x8626
        val FRONT: Int = 0x0404
        val BACK: Int = 0x0405
        val FRONT_AND_BACK: Int = 0x0408
        val CULL_FACE: Int = 0x0B44
        val BLEND: Int = 0x0BE2
        val DITHER: Int = 0x0BD0
        val STENCIL_TEST: Int = 0x0B90
        val DEPTH_TEST: Int = 0x0B71
        val SCISSOR_TEST: Int = 0x0C11
        val POLYGON_OFFSET_FILL: Int = 0x8037
        val SAMPLE_ALPHA_TO_COVERAGE: Int = 0x809E
        val SAMPLE_COVERAGE: Int = 0x80A0
        val NO_ERROR: Int = 0
        val INVALID_ENUM: Int = 0x0500
        val INVALID_VALUE: Int = 0x0501
        val INVALID_OPERATION: Int = 0x0502
        val OUT_OF_MEMORY: Int = 0x0505
        val CW: Int = 0x0900
        val CCW: Int = 0x0901
        val LINE_WIDTH: Int = 0x0B21
        val ALIASED_POINT_SIZE_RANGE: Int = 0x846D
        val ALIASED_LINE_WIDTH_RANGE: Int = 0x846E
        val CULL_FACE_MODE: Int = 0x0B45
        val FRONT_FACE: Int = 0x0B46
        val DEPTH_RANGE: Int = 0x0B70
        val DEPTH_WRITEMASK: Int = 0x0B72
        val DEPTH_CLEAR_VALUE: Int = 0x0B73
        val DEPTH_FUNC: Int = 0x0B74
        val STENCIL_CLEAR_VALUE: Int = 0x0B91
        val STENCIL_FUNC: Int = 0x0B92
        val STENCIL_FAIL: Int = 0x0B94
        val STENCIL_PASS_DEPTH_FAIL: Int = 0x0B95
        val STENCIL_PASS_DEPTH_PASS: Int = 0x0B96
        val STENCIL_REF: Int = 0x0B97
        val STENCIL_VALUE_MASK: Int = 0x0B93
        val STENCIL_WRITEMASK: Int = 0x0B98
        val STENCIL_BACK_FUNC: Int = 0x8800
        val STENCIL_BACK_FAIL: Int = 0x8801
        val STENCIL_BACK_PASS_DEPTH_FAIL: Int = 0x8802
        val STENCIL_BACK_PASS_DEPTH_PASS: Int = 0x8803
        val STENCIL_BACK_REF: Int = 0x8CA3
        val STENCIL_BACK_VALUE_MASK: Int = 0x8CA4
        val STENCIL_BACK_WRITEMASK: Int = 0x8CA5
        val VIEWPORT: Int = 0x0BA2
        val SCISSOR_BOX: Int = 0x0C10
        val COLOR_CLEAR_VALUE: Int = 0x0C22
        val COLOR_WRITEMASK: Int = 0x0C23
        val UNPACK_ALIGNMENT: Int = 0x0CF5
        val PACK_ALIGNMENT: Int = 0x0D05
        val MAX_TEXTURE_SIZE: Int = 0x0D33
        val MAX_VIEWPORT_DIMS: Int = 0x0D3A
        val SUBPIXEL_BITS: Int = 0x0D50
        val RED_BITS: Int = 0x0D52
        val GREEN_BITS: Int = 0x0D53
        val BLUE_BITS: Int = 0x0D54
        val ALPHA_BITS: Int = 0x0D55
        val DEPTH_BITS: Int = 0x0D56
        val STENCIL_BITS: Int = 0x0D57
        val POLYGON_OFFSET_UNITS: Int = 0x2A00
        val POLYGON_OFFSET_FACTOR: Int = 0x8038
        val TEXTURE_BINDING_2D: Int = 0x8069
        val SAMPLE_BUFFERS: Int = 0x80A8
        val SAMPLES: Int = 0x80A9
        val SAMPLE_COVERAGE_VALUE: Int = 0x80AA
        val SAMPLE_COVERAGE_INVERT: Int = 0x80AB
        val COMPRESSED_TEXTURE_FORMATS: Int = 0x86A3
        val DONT_CARE: Int = 0x1100
        val FASTEST: Int = 0x1101
        val NICEST: Int = 0x1102
        val GENERATE_MIPMAP_HINT: Int = 0x8192
        val BYTE: Int = 0x1400
        val UNSIGNED_BYTE: Int = 0x1401
        val SHORT: Int = 0x1402
        val UNSIGNED_SHORT: Int = 0x1403
        val INT: Int = 0x1404
        val UNSIGNED_INT: Int = 0x1405
        val FLOAT: Int = 0x1406
        val DEPTH_COMPONENT: Int = 0x1902
        val ALPHA: Int = 0x1906
        val RGB: Int = 0x1907
        val RGBA: Int = 0x1908
        val LUMINANCE: Int = 0x1909
        val LUMINANCE_ALPHA: Int = 0x190A
        val UNSIGNED_SHORT_4_4_4_4: Int = 0x8033
        val UNSIGNED_SHORT_5_5_5_1: Int = 0x8034
        val UNSIGNED_SHORT_5_6_5: Int = 0x8363
        val FRAGMENT_SHADER: Int = 0x8B30
        val VERTEX_SHADER: Int = 0x8B31
        val MAX_VERTEX_ATTRIBS: Int = 0x8869
        val MAX_VERTEX_UNIFORM_VECTORS: Int = 0x8DFB
        val MAX_VARYING_VECTORS: Int = 0x8DFC
        val MAX_COMBINED_TEXTURE_IMAGE_UNITS: Int = 0x8B4D
        val MAX_VERTEX_TEXTURE_IMAGE_UNITS: Int = 0x8B4C
        val MAX_TEXTURE_IMAGE_UNITS: Int = 0x8872
        val MAX_FRAGMENT_UNIFORM_VECTORS: Int = 0x8DFD
        val SHADER_TYPE: Int = 0x8B4F
        val DELETE_STATUS: Int = 0x8B80
        val LINK_STATUS: Int = 0x8B82
        val VALIDATE_STATUS: Int = 0x8B83
        val ATTACHED_SHADERS: Int = 0x8B85
        val ACTIVE_UNIFORMS: Int = 0x8B86
        val ACTIVE_ATTRIBUTES: Int = 0x8B89
        val SHADING_LANGUAGE_VERSION: Int = 0x8B8C
        val CURRENT_PROGRAM: Int = 0x8B8D
        val NEVER: Int = 0x0200
        val LESS: Int = 0x0201
        val EQUAL: Int = 0x0202
        val LEQUAL: Int = 0x0203
        val GREATER: Int = 0x0204
        val NOTEQUAL: Int = 0x0205
        val GEQUAL: Int = 0x0206
        val ALWAYS: Int = 0x0207
        val KEEP: Int = 0x1E00
        val REPLACE: Int = 0x1E01
        val INCR: Int = 0x1E02
        val DECR: Int = 0x1E03
        val INVERT: Int = 0x150A
        val INCR_WRAP: Int = 0x8507
        val DECR_WRAP: Int = 0x8508
        val VENDOR: Int = 0x1F00
        val RENDERER: Int = 0x1F01
        val VERSION: Int = 0x1F02
        val NEAREST: Int = 0x2600
        val LINEAR: Int = 0x2601
        val NEAREST_MIPMAP_NEAREST: Int = 0x2700
        val LINEAR_MIPMAP_NEAREST: Int = 0x2701
        val NEAREST_MIPMAP_LINEAR: Int = 0x2702
        val LINEAR_MIPMAP_LINEAR: Int = 0x2703
        val TEXTURE_MAG_FILTER: Int = 0x2800
        val TEXTURE_MIN_FILTER: Int = 0x2801
        val TEXTURE_WRAP_S: Int = 0x2802
        val TEXTURE_WRAP_T: Int = 0x2803
        val TEXTURE_2D: Int = 0x0DE1
        val TEXTURE: Int = 0x1702
        val TEXTURE_CUBE_MAP: Int = 0x8513
        val TEXTURE_BINDING_CUBE_MAP: Int = 0x8514
        val TEXTURE_CUBE_MAP_POSITIVE_X: Int = 0x8515
        val TEXTURE_CUBE_MAP_NEGATIVE_X: Int = 0x8516
        val TEXTURE_CUBE_MAP_POSITIVE_Y: Int = 0x8517
        val TEXTURE_CUBE_MAP_NEGATIVE_Y: Int = 0x8518
        val TEXTURE_CUBE_MAP_POSITIVE_Z: Int = 0x8519
        val TEXTURE_CUBE_MAP_NEGATIVE_Z: Int = 0x851A
        val MAX_CUBE_MAP_TEXTURE_SIZE: Int = 0x851C
        val TEXTURE0: Int = 0x84C0
        val TEXTURE1: Int = 0x84C1
        val TEXTURE2: Int = 0x84C2
        val TEXTURE3: Int = 0x84C3
        val TEXTURE4: Int = 0x84C4
        val TEXTURE5: Int = 0x84C5
        val TEXTURE6: Int = 0x84C6
        val TEXTURE7: Int = 0x84C7
        val TEXTURE8: Int = 0x84C8
        val TEXTURE9: Int = 0x84C9
        val TEXTURE10: Int = 0x84CA
        val TEXTURE11: Int = 0x84CB
        val TEXTURE12: Int = 0x84CC
        val TEXTURE13: Int = 0x84CD
        val TEXTURE14: Int = 0x84CE
        val TEXTURE15: Int = 0x84CF
        val TEXTURE16: Int = 0x84D0
        val TEXTURE17: Int = 0x84D1
        val TEXTURE18: Int = 0x84D2
        val TEXTURE19: Int = 0x84D3
        val TEXTURE20: Int = 0x84D4
        val TEXTURE21: Int = 0x84D5
        val TEXTURE22: Int = 0x84D6
        val TEXTURE23: Int = 0x84D7
        val TEXTURE24: Int = 0x84D8
        val TEXTURE25: Int = 0x84D9
        val TEXTURE26: Int = 0x84DA
        val TEXTURE27: Int = 0x84DB
        val TEXTURE28: Int = 0x84DC
        val TEXTURE29: Int = 0x84DD
        val TEXTURE30: Int = 0x84DE
        val TEXTURE31: Int = 0x84DF
        val ACTIVE_TEXTURE: Int = 0x84E0
        val REPEAT: Int = 0x2901
        val CLAMP_TO_EDGE: Int = 0x812F
        val MIRRORED_REPEAT: Int = 0x8370
        val FLOAT_VEC2: Int = 0x8B50
        val FLOAT_VEC3: Int = 0x8B51
        val FLOAT_VEC4: Int = 0x8B52
        val INT_VEC2: Int = 0x8B53
        val INT_VEC3: Int = 0x8B54
        val INT_VEC4: Int = 0x8B55
        val BOOL: Int = 0x8B56
        val BOOL_VEC2: Int = 0x8B57
        val BOOL_VEC3: Int = 0x8B58
        val BOOL_VEC4: Int = 0x8B59
        val FLOAT_MAT2: Int = 0x8B5A
        val FLOAT_MAT3: Int = 0x8B5B
        val FLOAT_MAT4: Int = 0x8B5C
        val SAMPLER_2D: Int = 0x8B5E
        val SAMPLER_CUBE: Int = 0x8B60
        val VERTEX_ATTRIB_ARRAY_ENABLED: Int = 0x8622
        val VERTEX_ATTRIB_ARRAY_SIZE: Int = 0x8623
        val VERTEX_ATTRIB_ARRAY_STRIDE: Int = 0x8624
        val VERTEX_ATTRIB_ARRAY_TYPE: Int = 0x8625
        val VERTEX_ATTRIB_ARRAY_NORMALIZED: Int = 0x886A
        val VERTEX_ATTRIB_ARRAY_POINTER: Int = 0x8645
        val VERTEX_ATTRIB_ARRAY_BUFFER_BINDING: Int = 0x889F
        val IMPLEMENTATION_COLOR_READ_TYPE: Int = 0x8B9A
        val IMPLEMENTATION_COLOR_READ_FORMAT: Int = 0x8B9B
        val COMPILE_STATUS: Int = 0x8B81
        val LOW_FLOAT: Int = 0x8DF0
        val MEDIUM_FLOAT: Int = 0x8DF1
        val HIGH_FLOAT: Int = 0x8DF2
        val LOW_INT: Int = 0x8DF3
        val MEDIUM_INT: Int = 0x8DF4
        val HIGH_INT: Int = 0x8DF5
        val FRAMEBUFFER: Int = 0x8D40
        val RENDERBUFFER: Int = 0x8D41
        val RGBA4: Int = 0x8056
        val RGB5_A1: Int = 0x8057
        val RGB565: Int = 0x8D62
        val DEPTH_COMPONENT16: Int = 0x81A5
        val STENCIL_INDEX: Int = 0x1901
        val STENCIL_INDEX8: Int = 0x8D48
        val DEPTH_STENCIL: Int = 0x84F9
        val RENDERBUFFER_WIDTH: Int = 0x8D42
        val RENDERBUFFER_HEIGHT: Int = 0x8D43
        val RENDERBUFFER_INTERNAL_FORMAT: Int = 0x8D44
        val RENDERBUFFER_RED_SIZE: Int = 0x8D50
        val RENDERBUFFER_GREEN_SIZE: Int = 0x8D51
        val RENDERBUFFER_BLUE_SIZE: Int = 0x8D52
        val RENDERBUFFER_ALPHA_SIZE: Int = 0x8D53
        val RENDERBUFFER_DEPTH_SIZE: Int = 0x8D54
        val RENDERBUFFER_STENCIL_SIZE: Int = 0x8D55
        val FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE: Int = 0x8CD0
        val FRAMEBUFFER_ATTACHMENT_OBJECT_NAME: Int = 0x8CD1
        val FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL: Int = 0x8CD2
        val FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE: Int = 0x8CD3
        val COLOR_ATTACHMENT0: Int = 0x8CE0
        val DEPTH_ATTACHMENT: Int = 0x8D00
        val STENCIL_ATTACHMENT: Int = 0x8D20
        val DEPTH_STENCIL_ATTACHMENT: Int = 0x821A
        val NONE: Int = 0
        val FRAMEBUFFER_COMPLETE: Int = 0x8CD5
        val FRAMEBUFFER_INCOMPLETE_ATTACHMENT: Int = 0x8CD6
        val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: Int = 0x8CD7
        val FRAMEBUFFER_INCOMPLETE_DIMENSIONS: Int = 0x8CD9
        val FRAMEBUFFER_UNSUPPORTED: Int = 0x8CDD
        val FRAMEBUFFER_BINDING: Int = 0x8CA6
        val RENDERBUFFER_BINDING: Int = 0x8CA7
        val MAX_RENDERBUFFER_SIZE: Int = 0x84E8
        val INVALID_FRAMEBUFFER_OPERATION: Int = 0x0506
        val UNPACK_FLIP_Y_WEBGL: Int = 0x9240
        val UNPACK_PREMULTIPLY_ALPHA_WEBGL: Int = 0x9241
        val CONTEXT_LOST_WEBGL: Int = 0x9242
        val UNPACK_COLORSPACE_CONVERSION_WEBGL: Int = 0x9243
        val BROWSER_DEFAULT_WEBGL: Int = 0x9244
    }
}

native public open class WebGLContextEvent(type: String, eventInit: WebGLContextEventInit = noImpl) : Event(type, noImpl) {
    var statusMessage: String
        get() = noImpl
        set(value) = noImpl
}

native public open class WebGLContextEventInit : EventInit() {
    var statusMessage: String
}

native public open class ArrayBuffer(length: Int) : Transferable {
    var byteLength: Int
        get() = noImpl
        set(value) = noImpl
    fun slice(begin: Int, end: Int = noImpl): ArrayBuffer = noImpl
}

native public open class Int8Array(length: Int) : ArrayBufferView {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var buffer: ArrayBuffer
        get() = noImpl
        set(value) = noImpl
    var byteOffset: Int
        get() = noImpl
        set(value) = noImpl
    var byteLength: Int
        get() = noImpl
        set(value) = noImpl
    fun get(index: Int): Byte = noImpl
    fun set(index: Int, value: Byte): Unit = noImpl
    fun set(array: Int8Array, offset: Int = noImpl): Unit = noImpl
    fun set(array: Array<Byte>, offset: Int = noImpl): Unit = noImpl
    fun subarray(start: Int, end: Int): Int8Array = noImpl

    companion object {
        val BYTES_PER_ELEMENT: Int = 1
    }
}

native public open class Uint8Array(length: Int) : ArrayBufferView {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var buffer: ArrayBuffer
        get() = noImpl
        set(value) = noImpl
    var byteOffset: Int
        get() = noImpl
        set(value) = noImpl
    var byteLength: Int
        get() = noImpl
        set(value) = noImpl
    fun get(index: Int): Byte = noImpl
    fun set(index: Int, value: Byte): Unit = noImpl
    fun set(array: Uint8Array, offset: Int = noImpl): Unit = noImpl
    fun set(array: Array<Byte>, offset: Int = noImpl): Unit = noImpl
    fun subarray(start: Int, end: Int): Uint8Array = noImpl

    companion object {
        val BYTES_PER_ELEMENT: Int = 1
    }
}

native public open class Uint8ClampedArray(length: Int) : ArrayBufferView {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var buffer: ArrayBuffer
        get() = noImpl
        set(value) = noImpl
    var byteOffset: Int
        get() = noImpl
        set(value) = noImpl
    var byteLength: Int
        get() = noImpl
        set(value) = noImpl
    fun get(index: Int): Byte = noImpl
    fun set(index: Int, value: Byte): Unit = noImpl
    fun set(array: Uint8ClampedArray, offset: Int = noImpl): Unit = noImpl
    fun set(array: Array<Byte>, offset: Int = noImpl): Unit = noImpl
    fun subarray(start: Int, end: Int): Uint8ClampedArray = noImpl

    companion object {
        val BYTES_PER_ELEMENT: Int = 1
    }
}

native public open class Int16Array(length: Int) : ArrayBufferView {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var buffer: ArrayBuffer
        get() = noImpl
        set(value) = noImpl
    var byteOffset: Int
        get() = noImpl
        set(value) = noImpl
    var byteLength: Int
        get() = noImpl
        set(value) = noImpl
    fun get(index: Int): Short = noImpl
    fun set(index: Int, value: Short): Unit = noImpl
    fun set(array: Int16Array, offset: Int = noImpl): Unit = noImpl
    fun set(array: Array<Short>, offset: Int = noImpl): Unit = noImpl
    fun subarray(start: Int, end: Int): Int16Array = noImpl

    companion object {
        val BYTES_PER_ELEMENT: Int = 2
    }
}

native public open class Uint16Array(length: Int) : ArrayBufferView {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var buffer: ArrayBuffer
        get() = noImpl
        set(value) = noImpl
    var byteOffset: Int
        get() = noImpl
        set(value) = noImpl
    var byteLength: Int
        get() = noImpl
        set(value) = noImpl
    fun get(index: Int): Short = noImpl
    fun set(index: Int, value: Short): Unit = noImpl
    fun set(array: Uint16Array, offset: Int = noImpl): Unit = noImpl
    fun set(array: Array<Short>, offset: Int = noImpl): Unit = noImpl
    fun subarray(start: Int, end: Int): Uint16Array = noImpl

    companion object {
        val BYTES_PER_ELEMENT: Int = 2
    }
}

native public open class Int32Array(length: Int) : ArrayBufferView {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var buffer: ArrayBuffer
        get() = noImpl
        set(value) = noImpl
    var byteOffset: Int
        get() = noImpl
        set(value) = noImpl
    var byteLength: Int
        get() = noImpl
        set(value) = noImpl
    fun get(index: Int): Int = noImpl
    fun set(index: Int, value: Int): Unit = noImpl
    fun set(array: Int32Array, offset: Int = noImpl): Unit = noImpl
    fun set(array: Array<Int>, offset: Int = noImpl): Unit = noImpl
    fun subarray(start: Int, end: Int): Int32Array = noImpl

    companion object {
        val BYTES_PER_ELEMENT: Int = 4
    }
}

native public open class Uint32Array(length: Int) : ArrayBufferView {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var buffer: ArrayBuffer
        get() = noImpl
        set(value) = noImpl
    var byteOffset: Int
        get() = noImpl
        set(value) = noImpl
    var byteLength: Int
        get() = noImpl
        set(value) = noImpl
    fun get(index: Int): Int = noImpl
    fun set(index: Int, value: Int): Unit = noImpl
    fun set(array: Uint32Array, offset: Int = noImpl): Unit = noImpl
    fun set(array: Array<Int>, offset: Int = noImpl): Unit = noImpl
    fun subarray(start: Int, end: Int): Uint32Array = noImpl

    companion object {
        val BYTES_PER_ELEMENT: Int = 4
    }
}

native public open class Float32Array(length: Int) : ArrayBufferView {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var buffer: ArrayBuffer
        get() = noImpl
        set(value) = noImpl
    var byteOffset: Int
        get() = noImpl
        set(value) = noImpl
    var byteLength: Int
        get() = noImpl
        set(value) = noImpl
    fun get(index: Int): Float = noImpl
    fun set(index: Int, value: Float): Unit = noImpl
    fun set(array: Float32Array, offset: Int = noImpl): Unit = noImpl
    fun set(array: Array<Float>, offset: Int = noImpl): Unit = noImpl
    fun subarray(start: Int, end: Int): Float32Array = noImpl

    companion object {
        val BYTES_PER_ELEMENT: Int = 4
    }
}

native public open class Float64Array(length: Int) : ArrayBufferView {
    var length: Int
        get() = noImpl
        set(value) = noImpl
    var buffer: ArrayBuffer
        get() = noImpl
        set(value) = noImpl
    var byteOffset: Int
        get() = noImpl
        set(value) = noImpl
    var byteLength: Int
        get() = noImpl
        set(value) = noImpl
    fun get(index: Int): Double = noImpl
    fun set(index: Int, value: Double): Unit = noImpl
    fun set(array: Float64Array, offset: Int = noImpl): Unit = noImpl
    fun set(array: Array<Double>, offset: Int = noImpl): Unit = noImpl
    fun subarray(start: Int, end: Int): Float64Array = noImpl

    companion object {
        val BYTES_PER_ELEMENT: Int = 8
    }
}

native public open class DataView(buffer: ArrayBuffer, byteOffset: Int = noImpl, byteLength: Int = noImpl) : ArrayBufferView {
    var buffer: ArrayBuffer
        get() = noImpl
        set(value) = noImpl
    var byteOffset: Int
        get() = noImpl
        set(value) = noImpl
    var byteLength: Int
        get() = noImpl
        set(value) = noImpl
    fun getInt8(byteOffset: Int): Byte = noImpl
    fun getUint8(byteOffset: Int): Byte = noImpl
    fun getInt16(byteOffset: Int, littleEndian: Boolean = noImpl): Short = noImpl
    fun getUint16(byteOffset: Int, littleEndian: Boolean = noImpl): Short = noImpl
    fun getInt32(byteOffset: Int, littleEndian: Boolean = noImpl): Int = noImpl
    fun getUint32(byteOffset: Int, littleEndian: Boolean = noImpl): Int = noImpl
    fun getFloat32(byteOffset: Int, littleEndian: Boolean = noImpl): Float = noImpl
    fun getFloat64(byteOffset: Int, littleEndian: Boolean = noImpl): Double = noImpl
    fun setInt8(byteOffset: Int, value: Byte): Unit = noImpl
    fun setUint8(byteOffset: Int, value: Byte): Unit = noImpl
    fun setInt16(byteOffset: Int, value: Short, littleEndian: Boolean = noImpl): Unit = noImpl
    fun setUint16(byteOffset: Int, value: Short, littleEndian: Boolean = noImpl): Unit = noImpl
    fun setInt32(byteOffset: Int, value: Int, littleEndian: Boolean = noImpl): Unit = noImpl
    fun setUint32(byteOffset: Int, value: Int, littleEndian: Boolean = noImpl): Unit = noImpl
    fun setFloat32(byteOffset: Int, value: Float, littleEndian: Boolean = noImpl): Unit = noImpl
    fun setFloat64(byteOffset: Int, value: Double, littleEndian: Boolean = noImpl): Unit = noImpl
}

