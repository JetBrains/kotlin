/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

// NOTE: THIS FILE IS AUTO-GENERATED, DO NOT EDIT!
// See libraries/tools/idl2k for details

@file:Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
package org.khronos.webgl

import kotlin.js.*
import org.w3c.css.masking.*
import org.w3c.dom.*
import org.w3c.dom.clipboard.*
import org.w3c.dom.css.*
import org.w3c.dom.events.*
import org.w3c.dom.mediacapture.*
import org.w3c.dom.parsing.*
import org.w3c.dom.pointerevents.*
import org.w3c.dom.svg.*
import org.w3c.dom.url.*
import org.w3c.fetch.*
import org.w3c.files.*
import org.w3c.notifications.*
import org.w3c.performance.*
import org.w3c.workers.*
import org.w3c.xhr.*

public external interface WebGLContextAttributes {
    var alpha: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var depth: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var stencil: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var antialias: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var premultipliedAlpha: Boolean? /* = true */
        get() = definedExternally
        set(value) = definedExternally
    var preserveDrawingBuffer: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var preferLowPowerToHighPerformance: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
    var failIfMajorPerformanceCaveat: Boolean? /* = false */
        get() = definedExternally
        set(value) = definedExternally
}

@kotlin.internal.InlineOnly
public inline fun WebGLContextAttributes(alpha: Boolean? = true, depth: Boolean? = true, stencil: Boolean? = false, antialias: Boolean? = true, premultipliedAlpha: Boolean? = true, preserveDrawingBuffer: Boolean? = false, preferLowPowerToHighPerformance: Boolean? = false, failIfMajorPerformanceCaveat: Boolean? = false): WebGLContextAttributes {
    val o = js("({})")

    o["alpha"] = alpha
    o["depth"] = depth
    o["stencil"] = stencil
    o["antialias"] = antialias
    o["premultipliedAlpha"] = premultipliedAlpha
    o["preserveDrawingBuffer"] = preserveDrawingBuffer
    o["preferLowPowerToHighPerformance"] = preferLowPowerToHighPerformance
    o["failIfMajorPerformanceCaveat"] = failIfMajorPerformanceCaveat

    return o
}

public external abstract class WebGLObject {
}

/**
 * Exposes the JavaScript [WebGLBuffer](https://developer.mozilla.org/en/docs/Web/API/WebGLBuffer) to Kotlin
 */
public external abstract class WebGLBuffer : WebGLObject {
}

/**
 * Exposes the JavaScript [WebGLFramebuffer](https://developer.mozilla.org/en/docs/Web/API/WebGLFramebuffer) to Kotlin
 */
public external abstract class WebGLFramebuffer : WebGLObject {
}

/**
 * Exposes the JavaScript [WebGLProgram](https://developer.mozilla.org/en/docs/Web/API/WebGLProgram) to Kotlin
 */
public external abstract class WebGLProgram : WebGLObject {
}

/**
 * Exposes the JavaScript [WebGLRenderbuffer](https://developer.mozilla.org/en/docs/Web/API/WebGLRenderbuffer) to Kotlin
 */
public external abstract class WebGLRenderbuffer : WebGLObject {
}

/**
 * Exposes the JavaScript [WebGLShader](https://developer.mozilla.org/en/docs/Web/API/WebGLShader) to Kotlin
 */
public external abstract class WebGLShader : WebGLObject {
}

/**
 * Exposes the JavaScript [WebGLTexture](https://developer.mozilla.org/en/docs/Web/API/WebGLTexture) to Kotlin
 */
public external abstract class WebGLTexture : WebGLObject {
}

/**
 * Exposes the JavaScript [WebGLUniformLocation](https://developer.mozilla.org/en/docs/Web/API/WebGLUniformLocation) to Kotlin
 */
public external abstract class WebGLUniformLocation {
}

/**
 * Exposes the JavaScript [WebGLActiveInfo](https://developer.mozilla.org/en/docs/Web/API/WebGLActiveInfo) to Kotlin
 */
public external abstract class WebGLActiveInfo {
    open val size: Int
    open val type: Int
    open val name: String
}

/**
 * Exposes the JavaScript [WebGLShaderPrecisionFormat](https://developer.mozilla.org/en/docs/Web/API/WebGLShaderPrecisionFormat) to Kotlin
 */
public external abstract class WebGLShaderPrecisionFormat {
    open val rangeMin: Int
    open val rangeMax: Int
    open val precision: Int
}

public external interface WebGLRenderingContextBase {
    val canvas: HTMLCanvasElement
    val drawingBufferWidth: Int
    val drawingBufferHeight: Int
    fun getContextAttributes(): WebGLContextAttributes?
    fun isContextLost(): Boolean
    fun getSupportedExtensions(): Array<String>?
    fun getExtension(name: String): dynamic
    fun activeTexture(texture: Int): Unit
    fun attachShader(program: WebGLProgram?, shader: WebGLShader?): Unit
    fun bindAttribLocation(program: WebGLProgram?, index: Int, name: String): Unit
    fun bindBuffer(target: Int, buffer: WebGLBuffer?): Unit
    fun bindFramebuffer(target: Int, framebuffer: WebGLFramebuffer?): Unit
    fun bindRenderbuffer(target: Int, renderbuffer: WebGLRenderbuffer?): Unit
    fun bindTexture(target: Int, texture: WebGLTexture?): Unit
    fun blendColor(red: Float, green: Float, blue: Float, alpha: Float): Unit
    fun blendEquation(mode: Int): Unit
    fun blendEquationSeparate(modeRGB: Int, modeAlpha: Int): Unit
    fun blendFunc(sfactor: Int, dfactor: Int): Unit
    fun blendFuncSeparate(srcRGB: Int, dstRGB: Int, srcAlpha: Int, dstAlpha: Int): Unit
    fun bufferData(target: Int, size: Int, usage: Int): Unit
    fun bufferData(target: Int, data: BufferDataSource?, usage: Int): Unit
    fun bufferSubData(target: Int, offset: Int, data: BufferDataSource?): Unit
    fun checkFramebufferStatus(target: Int): Int
    fun clear(mask: Int): Unit
    fun clearColor(red: Float, green: Float, blue: Float, alpha: Float): Unit
    fun clearDepth(depth: Float): Unit
    fun clearStencil(s: Int): Unit
    fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean): Unit
    fun compileShader(shader: WebGLShader?): Unit
    fun compressedTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, data: ArrayBufferView): Unit
    fun compressedTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, data: ArrayBufferView): Unit
    fun copyTexImage2D(target: Int, level: Int, internalformat: Int, x: Int, y: Int, width: Int, height: Int, border: Int): Unit
    fun copyTexSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, x: Int, y: Int, width: Int, height: Int): Unit
    fun createBuffer(): WebGLBuffer?
    fun createFramebuffer(): WebGLFramebuffer?
    fun createProgram(): WebGLProgram?
    fun createRenderbuffer(): WebGLRenderbuffer?
    fun createShader(type: Int): WebGLShader?
    fun createTexture(): WebGLTexture?
    fun cullFace(mode: Int): Unit
    fun deleteBuffer(buffer: WebGLBuffer?): Unit
    fun deleteFramebuffer(framebuffer: WebGLFramebuffer?): Unit
    fun deleteProgram(program: WebGLProgram?): Unit
    fun deleteRenderbuffer(renderbuffer: WebGLRenderbuffer?): Unit
    fun deleteShader(shader: WebGLShader?): Unit
    fun deleteTexture(texture: WebGLTexture?): Unit
    fun depthFunc(func: Int): Unit
    fun depthMask(flag: Boolean): Unit
    fun depthRange(zNear: Float, zFar: Float): Unit
    fun detachShader(program: WebGLProgram?, shader: WebGLShader?): Unit
    fun disable(cap: Int): Unit
    fun disableVertexAttribArray(index: Int): Unit
    fun drawArrays(mode: Int, first: Int, count: Int): Unit
    fun drawElements(mode: Int, count: Int, type: Int, offset: Int): Unit
    fun enable(cap: Int): Unit
    fun enableVertexAttribArray(index: Int): Unit
    fun finish(): Unit
    fun flush(): Unit
    fun framebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: WebGLRenderbuffer?): Unit
    fun framebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: WebGLTexture?, level: Int): Unit
    fun frontFace(mode: Int): Unit
    fun generateMipmap(target: Int): Unit
    fun getActiveAttrib(program: WebGLProgram?, index: Int): WebGLActiveInfo?
    fun getActiveUniform(program: WebGLProgram?, index: Int): WebGLActiveInfo?
    fun getAttachedShaders(program: WebGLProgram?): Array<WebGLShader>?
    fun getAttribLocation(program: WebGLProgram?, name: String): Int
    fun getBufferParameter(target: Int, pname: Int): Any?
    fun getParameter(pname: Int): Any?
    fun getError(): Int
    fun getFramebufferAttachmentParameter(target: Int, attachment: Int, pname: Int): Any?
    fun getProgramParameter(program: WebGLProgram?, pname: Int): Any?
    fun getProgramInfoLog(program: WebGLProgram?): String?
    fun getRenderbufferParameter(target: Int, pname: Int): Any?
    fun getShaderParameter(shader: WebGLShader?, pname: Int): Any?
    fun getShaderPrecisionFormat(shadertype: Int, precisiontype: Int): WebGLShaderPrecisionFormat?
    fun getShaderInfoLog(shader: WebGLShader?): String?
    fun getShaderSource(shader: WebGLShader?): String?
    fun getTexParameter(target: Int, pname: Int): Any?
    fun getUniform(program: WebGLProgram?, location: WebGLUniformLocation?): Any?
    fun getUniformLocation(program: WebGLProgram?, name: String): WebGLUniformLocation?
    fun getVertexAttrib(index: Int, pname: Int): Any?
    fun getVertexAttribOffset(index: Int, pname: Int): Int
    fun hint(target: Int, mode: Int): Unit
    fun isBuffer(buffer: WebGLBuffer?): Boolean
    fun isEnabled(cap: Int): Boolean
    fun isFramebuffer(framebuffer: WebGLFramebuffer?): Boolean
    fun isProgram(program: WebGLProgram?): Boolean
    fun isRenderbuffer(renderbuffer: WebGLRenderbuffer?): Boolean
    fun isShader(shader: WebGLShader?): Boolean
    fun isTexture(texture: WebGLTexture?): Boolean
    fun lineWidth(width: Float): Unit
    fun linkProgram(program: WebGLProgram?): Unit
    fun pixelStorei(pname: Int, param: Int): Unit
    fun polygonOffset(factor: Float, units: Float): Unit
    fun readPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int, pixels: ArrayBufferView?): Unit
    fun renderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int): Unit
    fun sampleCoverage(value: Float, invert: Boolean): Unit
    fun scissor(x: Int, y: Int, width: Int, height: Int): Unit
    fun shaderSource(shader: WebGLShader?, source: String): Unit
    fun stencilFunc(func: Int, ref: Int, mask: Int): Unit
    fun stencilFuncSeparate(face: Int, func: Int, ref: Int, mask: Int): Unit
    fun stencilMask(mask: Int): Unit
    fun stencilMaskSeparate(face: Int, mask: Int): Unit
    fun stencilOp(fail: Int, zfail: Int, zpass: Int): Unit
    fun stencilOpSeparate(face: Int, fail: Int, zfail: Int, zpass: Int): Unit
    fun texImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: ArrayBufferView?): Unit
    fun texImage2D(target: Int, level: Int, internalformat: Int, format: Int, type: Int, source: TexImageSource?): Unit
    fun texParameterf(target: Int, pname: Int, param: Float): Unit
    fun texParameteri(target: Int, pname: Int, param: Int): Unit
    fun texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, width: Int, height: Int, format: Int, type: Int, pixels: ArrayBufferView?): Unit
    fun texSubImage2D(target: Int, level: Int, xoffset: Int, yoffset: Int, format: Int, type: Int, source: TexImageSource?): Unit
    fun uniform1f(location: WebGLUniformLocation?, x: Float): Unit
    fun uniform1fv(location: WebGLUniformLocation?, v: Float32Array): Unit
    fun uniform1fv(location: WebGLUniformLocation?, v: Array<Float>): Unit
    fun uniform1i(location: WebGLUniformLocation?, x: Int): Unit
    fun uniform1iv(location: WebGLUniformLocation?, v: Int32Array): Unit
    fun uniform1iv(location: WebGLUniformLocation?, v: Array<Int>): Unit
    fun uniform2f(location: WebGLUniformLocation?, x: Float, y: Float): Unit
    fun uniform2fv(location: WebGLUniformLocation?, v: Float32Array): Unit
    fun uniform2fv(location: WebGLUniformLocation?, v: Array<Float>): Unit
    fun uniform2i(location: WebGLUniformLocation?, x: Int, y: Int): Unit
    fun uniform2iv(location: WebGLUniformLocation?, v: Int32Array): Unit
    fun uniform2iv(location: WebGLUniformLocation?, v: Array<Int>): Unit
    fun uniform3f(location: WebGLUniformLocation?, x: Float, y: Float, z: Float): Unit
    fun uniform3fv(location: WebGLUniformLocation?, v: Float32Array): Unit
    fun uniform3fv(location: WebGLUniformLocation?, v: Array<Float>): Unit
    fun uniform3i(location: WebGLUniformLocation?, x: Int, y: Int, z: Int): Unit
    fun uniform3iv(location: WebGLUniformLocation?, v: Int32Array): Unit
    fun uniform3iv(location: WebGLUniformLocation?, v: Array<Int>): Unit
    fun uniform4f(location: WebGLUniformLocation?, x: Float, y: Float, z: Float, w: Float): Unit
    fun uniform4fv(location: WebGLUniformLocation?, v: Float32Array): Unit
    fun uniform4fv(location: WebGLUniformLocation?, v: Array<Float>): Unit
    fun uniform4i(location: WebGLUniformLocation?, x: Int, y: Int, z: Int, w: Int): Unit
    fun uniform4iv(location: WebGLUniformLocation?, v: Int32Array): Unit
    fun uniform4iv(location: WebGLUniformLocation?, v: Array<Int>): Unit
    fun uniformMatrix2fv(location: WebGLUniformLocation?, transpose: Boolean, value: Float32Array): Unit
    fun uniformMatrix2fv(location: WebGLUniformLocation?, transpose: Boolean, value: Array<Float>): Unit
    fun uniformMatrix3fv(location: WebGLUniformLocation?, transpose: Boolean, value: Float32Array): Unit
    fun uniformMatrix3fv(location: WebGLUniformLocation?, transpose: Boolean, value: Array<Float>): Unit
    fun uniformMatrix4fv(location: WebGLUniformLocation?, transpose: Boolean, value: Float32Array): Unit
    fun uniformMatrix4fv(location: WebGLUniformLocation?, transpose: Boolean, value: Array<Float>): Unit
    fun useProgram(program: WebGLProgram?): Unit
    fun validateProgram(program: WebGLProgram?): Unit
    fun vertexAttrib1f(index: Int, x: Float): Unit
    fun vertexAttrib1fv(index: Int, values: dynamic): Unit
    fun vertexAttrib2f(index: Int, x: Float, y: Float): Unit
    fun vertexAttrib2fv(index: Int, values: dynamic): Unit
    fun vertexAttrib3f(index: Int, x: Float, y: Float, z: Float): Unit
    fun vertexAttrib3fv(index: Int, values: dynamic): Unit
    fun vertexAttrib4f(index: Int, x: Float, y: Float, z: Float, w: Float): Unit
    fun vertexAttrib4fv(index: Int, values: dynamic): Unit
    fun vertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, stride: Int, offset: Int): Unit
    fun viewport(x: Int, y: Int, width: Int, height: Int): Unit

    companion object {
        val DEPTH_BUFFER_BIT: Int
        val STENCIL_BUFFER_BIT: Int
        val COLOR_BUFFER_BIT: Int
        val POINTS: Int
        val LINES: Int
        val LINE_LOOP: Int
        val LINE_STRIP: Int
        val TRIANGLES: Int
        val TRIANGLE_STRIP: Int
        val TRIANGLE_FAN: Int
        val ZERO: Int
        val ONE: Int
        val SRC_COLOR: Int
        val ONE_MINUS_SRC_COLOR: Int
        val SRC_ALPHA: Int
        val ONE_MINUS_SRC_ALPHA: Int
        val DST_ALPHA: Int
        val ONE_MINUS_DST_ALPHA: Int
        val DST_COLOR: Int
        val ONE_MINUS_DST_COLOR: Int
        val SRC_ALPHA_SATURATE: Int
        val FUNC_ADD: Int
        val BLEND_EQUATION: Int
        val BLEND_EQUATION_RGB: Int
        val BLEND_EQUATION_ALPHA: Int
        val FUNC_SUBTRACT: Int
        val FUNC_REVERSE_SUBTRACT: Int
        val BLEND_DST_RGB: Int
        val BLEND_SRC_RGB: Int
        val BLEND_DST_ALPHA: Int
        val BLEND_SRC_ALPHA: Int
        val CONSTANT_COLOR: Int
        val ONE_MINUS_CONSTANT_COLOR: Int
        val CONSTANT_ALPHA: Int
        val ONE_MINUS_CONSTANT_ALPHA: Int
        val BLEND_COLOR: Int
        val ARRAY_BUFFER: Int
        val ELEMENT_ARRAY_BUFFER: Int
        val ARRAY_BUFFER_BINDING: Int
        val ELEMENT_ARRAY_BUFFER_BINDING: Int
        val STREAM_DRAW: Int
        val STATIC_DRAW: Int
        val DYNAMIC_DRAW: Int
        val BUFFER_SIZE: Int
        val BUFFER_USAGE: Int
        val CURRENT_VERTEX_ATTRIB: Int
        val FRONT: Int
        val BACK: Int
        val FRONT_AND_BACK: Int
        val CULL_FACE: Int
        val BLEND: Int
        val DITHER: Int
        val STENCIL_TEST: Int
        val DEPTH_TEST: Int
        val SCISSOR_TEST: Int
        val POLYGON_OFFSET_FILL: Int
        val SAMPLE_ALPHA_TO_COVERAGE: Int
        val SAMPLE_COVERAGE: Int
        val NO_ERROR: Int
        val INVALID_ENUM: Int
        val INVALID_VALUE: Int
        val INVALID_OPERATION: Int
        val OUT_OF_MEMORY: Int
        val CW: Int
        val CCW: Int
        val LINE_WIDTH: Int
        val ALIASED_POINT_SIZE_RANGE: Int
        val ALIASED_LINE_WIDTH_RANGE: Int
        val CULL_FACE_MODE: Int
        val FRONT_FACE: Int
        val DEPTH_RANGE: Int
        val DEPTH_WRITEMASK: Int
        val DEPTH_CLEAR_VALUE: Int
        val DEPTH_FUNC: Int
        val STENCIL_CLEAR_VALUE: Int
        val STENCIL_FUNC: Int
        val STENCIL_FAIL: Int
        val STENCIL_PASS_DEPTH_FAIL: Int
        val STENCIL_PASS_DEPTH_PASS: Int
        val STENCIL_REF: Int
        val STENCIL_VALUE_MASK: Int
        val STENCIL_WRITEMASK: Int
        val STENCIL_BACK_FUNC: Int
        val STENCIL_BACK_FAIL: Int
        val STENCIL_BACK_PASS_DEPTH_FAIL: Int
        val STENCIL_BACK_PASS_DEPTH_PASS: Int
        val STENCIL_BACK_REF: Int
        val STENCIL_BACK_VALUE_MASK: Int
        val STENCIL_BACK_WRITEMASK: Int
        val VIEWPORT: Int
        val SCISSOR_BOX: Int
        val COLOR_CLEAR_VALUE: Int
        val COLOR_WRITEMASK: Int
        val UNPACK_ALIGNMENT: Int
        val PACK_ALIGNMENT: Int
        val MAX_TEXTURE_SIZE: Int
        val MAX_VIEWPORT_DIMS: Int
        val SUBPIXEL_BITS: Int
        val RED_BITS: Int
        val GREEN_BITS: Int
        val BLUE_BITS: Int
        val ALPHA_BITS: Int
        val DEPTH_BITS: Int
        val STENCIL_BITS: Int
        val POLYGON_OFFSET_UNITS: Int
        val POLYGON_OFFSET_FACTOR: Int
        val TEXTURE_BINDING_2D: Int
        val SAMPLE_BUFFERS: Int
        val SAMPLES: Int
        val SAMPLE_COVERAGE_VALUE: Int
        val SAMPLE_COVERAGE_INVERT: Int
        val COMPRESSED_TEXTURE_FORMATS: Int
        val DONT_CARE: Int
        val FASTEST: Int
        val NICEST: Int
        val GENERATE_MIPMAP_HINT: Int
        val BYTE: Int
        val UNSIGNED_BYTE: Int
        val SHORT: Int
        val UNSIGNED_SHORT: Int
        val INT: Int
        val UNSIGNED_INT: Int
        val FLOAT: Int
        val DEPTH_COMPONENT: Int
        val ALPHA: Int
        val RGB: Int
        val RGBA: Int
        val LUMINANCE: Int
        val LUMINANCE_ALPHA: Int
        val UNSIGNED_SHORT_4_4_4_4: Int
        val UNSIGNED_SHORT_5_5_5_1: Int
        val UNSIGNED_SHORT_5_6_5: Int
        val FRAGMENT_SHADER: Int
        val VERTEX_SHADER: Int
        val MAX_VERTEX_ATTRIBS: Int
        val MAX_VERTEX_UNIFORM_VECTORS: Int
        val MAX_VARYING_VECTORS: Int
        val MAX_COMBINED_TEXTURE_IMAGE_UNITS: Int
        val MAX_VERTEX_TEXTURE_IMAGE_UNITS: Int
        val MAX_TEXTURE_IMAGE_UNITS: Int
        val MAX_FRAGMENT_UNIFORM_VECTORS: Int
        val SHADER_TYPE: Int
        val DELETE_STATUS: Int
        val LINK_STATUS: Int
        val VALIDATE_STATUS: Int
        val ATTACHED_SHADERS: Int
        val ACTIVE_UNIFORMS: Int
        val ACTIVE_ATTRIBUTES: Int
        val SHADING_LANGUAGE_VERSION: Int
        val CURRENT_PROGRAM: Int
        val NEVER: Int
        val LESS: Int
        val EQUAL: Int
        val LEQUAL: Int
        val GREATER: Int
        val NOTEQUAL: Int
        val GEQUAL: Int
        val ALWAYS: Int
        val KEEP: Int
        val REPLACE: Int
        val INCR: Int
        val DECR: Int
        val INVERT: Int
        val INCR_WRAP: Int
        val DECR_WRAP: Int
        val VENDOR: Int
        val RENDERER: Int
        val VERSION: Int
        val NEAREST: Int
        val LINEAR: Int
        val NEAREST_MIPMAP_NEAREST: Int
        val LINEAR_MIPMAP_NEAREST: Int
        val NEAREST_MIPMAP_LINEAR: Int
        val LINEAR_MIPMAP_LINEAR: Int
        val TEXTURE_MAG_FILTER: Int
        val TEXTURE_MIN_FILTER: Int
        val TEXTURE_WRAP_S: Int
        val TEXTURE_WRAP_T: Int
        val TEXTURE_2D: Int
        val TEXTURE: Int
        val TEXTURE_CUBE_MAP: Int
        val TEXTURE_BINDING_CUBE_MAP: Int
        val TEXTURE_CUBE_MAP_POSITIVE_X: Int
        val TEXTURE_CUBE_MAP_NEGATIVE_X: Int
        val TEXTURE_CUBE_MAP_POSITIVE_Y: Int
        val TEXTURE_CUBE_MAP_NEGATIVE_Y: Int
        val TEXTURE_CUBE_MAP_POSITIVE_Z: Int
        val TEXTURE_CUBE_MAP_NEGATIVE_Z: Int
        val MAX_CUBE_MAP_TEXTURE_SIZE: Int
        val TEXTURE0: Int
        val TEXTURE1: Int
        val TEXTURE2: Int
        val TEXTURE3: Int
        val TEXTURE4: Int
        val TEXTURE5: Int
        val TEXTURE6: Int
        val TEXTURE7: Int
        val TEXTURE8: Int
        val TEXTURE9: Int
        val TEXTURE10: Int
        val TEXTURE11: Int
        val TEXTURE12: Int
        val TEXTURE13: Int
        val TEXTURE14: Int
        val TEXTURE15: Int
        val TEXTURE16: Int
        val TEXTURE17: Int
        val TEXTURE18: Int
        val TEXTURE19: Int
        val TEXTURE20: Int
        val TEXTURE21: Int
        val TEXTURE22: Int
        val TEXTURE23: Int
        val TEXTURE24: Int
        val TEXTURE25: Int
        val TEXTURE26: Int
        val TEXTURE27: Int
        val TEXTURE28: Int
        val TEXTURE29: Int
        val TEXTURE30: Int
        val TEXTURE31: Int
        val ACTIVE_TEXTURE: Int
        val REPEAT: Int
        val CLAMP_TO_EDGE: Int
        val MIRRORED_REPEAT: Int
        val FLOAT_VEC2: Int
        val FLOAT_VEC3: Int
        val FLOAT_VEC4: Int
        val INT_VEC2: Int
        val INT_VEC3: Int
        val INT_VEC4: Int
        val BOOL: Int
        val BOOL_VEC2: Int
        val BOOL_VEC3: Int
        val BOOL_VEC4: Int
        val FLOAT_MAT2: Int
        val FLOAT_MAT3: Int
        val FLOAT_MAT4: Int
        val SAMPLER_2D: Int
        val SAMPLER_CUBE: Int
        val VERTEX_ATTRIB_ARRAY_ENABLED: Int
        val VERTEX_ATTRIB_ARRAY_SIZE: Int
        val VERTEX_ATTRIB_ARRAY_STRIDE: Int
        val VERTEX_ATTRIB_ARRAY_TYPE: Int
        val VERTEX_ATTRIB_ARRAY_NORMALIZED: Int
        val VERTEX_ATTRIB_ARRAY_POINTER: Int
        val VERTEX_ATTRIB_ARRAY_BUFFER_BINDING: Int
        val IMPLEMENTATION_COLOR_READ_TYPE: Int
        val IMPLEMENTATION_COLOR_READ_FORMAT: Int
        val COMPILE_STATUS: Int
        val LOW_FLOAT: Int
        val MEDIUM_FLOAT: Int
        val HIGH_FLOAT: Int
        val LOW_INT: Int
        val MEDIUM_INT: Int
        val HIGH_INT: Int
        val FRAMEBUFFER: Int
        val RENDERBUFFER: Int
        val RGBA4: Int
        val RGB5_A1: Int
        val RGB565: Int
        val DEPTH_COMPONENT16: Int
        val STENCIL_INDEX: Int
        val STENCIL_INDEX8: Int
        val DEPTH_STENCIL: Int
        val RENDERBUFFER_WIDTH: Int
        val RENDERBUFFER_HEIGHT: Int
        val RENDERBUFFER_INTERNAL_FORMAT: Int
        val RENDERBUFFER_RED_SIZE: Int
        val RENDERBUFFER_GREEN_SIZE: Int
        val RENDERBUFFER_BLUE_SIZE: Int
        val RENDERBUFFER_ALPHA_SIZE: Int
        val RENDERBUFFER_DEPTH_SIZE: Int
        val RENDERBUFFER_STENCIL_SIZE: Int
        val FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE: Int
        val FRAMEBUFFER_ATTACHMENT_OBJECT_NAME: Int
        val FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL: Int
        val FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE: Int
        val COLOR_ATTACHMENT0: Int
        val DEPTH_ATTACHMENT: Int
        val STENCIL_ATTACHMENT: Int
        val DEPTH_STENCIL_ATTACHMENT: Int
        val NONE: Int
        val FRAMEBUFFER_COMPLETE: Int
        val FRAMEBUFFER_INCOMPLETE_ATTACHMENT: Int
        val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: Int
        val FRAMEBUFFER_INCOMPLETE_DIMENSIONS: Int
        val FRAMEBUFFER_UNSUPPORTED: Int
        val FRAMEBUFFER_BINDING: Int
        val RENDERBUFFER_BINDING: Int
        val MAX_RENDERBUFFER_SIZE: Int
        val INVALID_FRAMEBUFFER_OPERATION: Int
        val UNPACK_FLIP_Y_WEBGL: Int
        val UNPACK_PREMULTIPLY_ALPHA_WEBGL: Int
        val CONTEXT_LOST_WEBGL: Int
        val UNPACK_COLORSPACE_CONVERSION_WEBGL: Int
        val BROWSER_DEFAULT_WEBGL: Int
    }
}

/**
 * Exposes the JavaScript [WebGLRenderingContext](https://developer.mozilla.org/en/docs/Web/API/WebGLRenderingContext) to Kotlin
 */
public external abstract class WebGLRenderingContext : WebGLRenderingContextBase, RenderingContext {

    companion object {
        val DEPTH_BUFFER_BIT: Int
        val STENCIL_BUFFER_BIT: Int
        val COLOR_BUFFER_BIT: Int
        val POINTS: Int
        val LINES: Int
        val LINE_LOOP: Int
        val LINE_STRIP: Int
        val TRIANGLES: Int
        val TRIANGLE_STRIP: Int
        val TRIANGLE_FAN: Int
        val ZERO: Int
        val ONE: Int
        val SRC_COLOR: Int
        val ONE_MINUS_SRC_COLOR: Int
        val SRC_ALPHA: Int
        val ONE_MINUS_SRC_ALPHA: Int
        val DST_ALPHA: Int
        val ONE_MINUS_DST_ALPHA: Int
        val DST_COLOR: Int
        val ONE_MINUS_DST_COLOR: Int
        val SRC_ALPHA_SATURATE: Int
        val FUNC_ADD: Int
        val BLEND_EQUATION: Int
        val BLEND_EQUATION_RGB: Int
        val BLEND_EQUATION_ALPHA: Int
        val FUNC_SUBTRACT: Int
        val FUNC_REVERSE_SUBTRACT: Int
        val BLEND_DST_RGB: Int
        val BLEND_SRC_RGB: Int
        val BLEND_DST_ALPHA: Int
        val BLEND_SRC_ALPHA: Int
        val CONSTANT_COLOR: Int
        val ONE_MINUS_CONSTANT_COLOR: Int
        val CONSTANT_ALPHA: Int
        val ONE_MINUS_CONSTANT_ALPHA: Int
        val BLEND_COLOR: Int
        val ARRAY_BUFFER: Int
        val ELEMENT_ARRAY_BUFFER: Int
        val ARRAY_BUFFER_BINDING: Int
        val ELEMENT_ARRAY_BUFFER_BINDING: Int
        val STREAM_DRAW: Int
        val STATIC_DRAW: Int
        val DYNAMIC_DRAW: Int
        val BUFFER_SIZE: Int
        val BUFFER_USAGE: Int
        val CURRENT_VERTEX_ATTRIB: Int
        val FRONT: Int
        val BACK: Int
        val FRONT_AND_BACK: Int
        val CULL_FACE: Int
        val BLEND: Int
        val DITHER: Int
        val STENCIL_TEST: Int
        val DEPTH_TEST: Int
        val SCISSOR_TEST: Int
        val POLYGON_OFFSET_FILL: Int
        val SAMPLE_ALPHA_TO_COVERAGE: Int
        val SAMPLE_COVERAGE: Int
        val NO_ERROR: Int
        val INVALID_ENUM: Int
        val INVALID_VALUE: Int
        val INVALID_OPERATION: Int
        val OUT_OF_MEMORY: Int
        val CW: Int
        val CCW: Int
        val LINE_WIDTH: Int
        val ALIASED_POINT_SIZE_RANGE: Int
        val ALIASED_LINE_WIDTH_RANGE: Int
        val CULL_FACE_MODE: Int
        val FRONT_FACE: Int
        val DEPTH_RANGE: Int
        val DEPTH_WRITEMASK: Int
        val DEPTH_CLEAR_VALUE: Int
        val DEPTH_FUNC: Int
        val STENCIL_CLEAR_VALUE: Int
        val STENCIL_FUNC: Int
        val STENCIL_FAIL: Int
        val STENCIL_PASS_DEPTH_FAIL: Int
        val STENCIL_PASS_DEPTH_PASS: Int
        val STENCIL_REF: Int
        val STENCIL_VALUE_MASK: Int
        val STENCIL_WRITEMASK: Int
        val STENCIL_BACK_FUNC: Int
        val STENCIL_BACK_FAIL: Int
        val STENCIL_BACK_PASS_DEPTH_FAIL: Int
        val STENCIL_BACK_PASS_DEPTH_PASS: Int
        val STENCIL_BACK_REF: Int
        val STENCIL_BACK_VALUE_MASK: Int
        val STENCIL_BACK_WRITEMASK: Int
        val VIEWPORT: Int
        val SCISSOR_BOX: Int
        val COLOR_CLEAR_VALUE: Int
        val COLOR_WRITEMASK: Int
        val UNPACK_ALIGNMENT: Int
        val PACK_ALIGNMENT: Int
        val MAX_TEXTURE_SIZE: Int
        val MAX_VIEWPORT_DIMS: Int
        val SUBPIXEL_BITS: Int
        val RED_BITS: Int
        val GREEN_BITS: Int
        val BLUE_BITS: Int
        val ALPHA_BITS: Int
        val DEPTH_BITS: Int
        val STENCIL_BITS: Int
        val POLYGON_OFFSET_UNITS: Int
        val POLYGON_OFFSET_FACTOR: Int
        val TEXTURE_BINDING_2D: Int
        val SAMPLE_BUFFERS: Int
        val SAMPLES: Int
        val SAMPLE_COVERAGE_VALUE: Int
        val SAMPLE_COVERAGE_INVERT: Int
        val COMPRESSED_TEXTURE_FORMATS: Int
        val DONT_CARE: Int
        val FASTEST: Int
        val NICEST: Int
        val GENERATE_MIPMAP_HINT: Int
        val BYTE: Int
        val UNSIGNED_BYTE: Int
        val SHORT: Int
        val UNSIGNED_SHORT: Int
        val INT: Int
        val UNSIGNED_INT: Int
        val FLOAT: Int
        val DEPTH_COMPONENT: Int
        val ALPHA: Int
        val RGB: Int
        val RGBA: Int
        val LUMINANCE: Int
        val LUMINANCE_ALPHA: Int
        val UNSIGNED_SHORT_4_4_4_4: Int
        val UNSIGNED_SHORT_5_5_5_1: Int
        val UNSIGNED_SHORT_5_6_5: Int
        val FRAGMENT_SHADER: Int
        val VERTEX_SHADER: Int
        val MAX_VERTEX_ATTRIBS: Int
        val MAX_VERTEX_UNIFORM_VECTORS: Int
        val MAX_VARYING_VECTORS: Int
        val MAX_COMBINED_TEXTURE_IMAGE_UNITS: Int
        val MAX_VERTEX_TEXTURE_IMAGE_UNITS: Int
        val MAX_TEXTURE_IMAGE_UNITS: Int
        val MAX_FRAGMENT_UNIFORM_VECTORS: Int
        val SHADER_TYPE: Int
        val DELETE_STATUS: Int
        val LINK_STATUS: Int
        val VALIDATE_STATUS: Int
        val ATTACHED_SHADERS: Int
        val ACTIVE_UNIFORMS: Int
        val ACTIVE_ATTRIBUTES: Int
        val SHADING_LANGUAGE_VERSION: Int
        val CURRENT_PROGRAM: Int
        val NEVER: Int
        val LESS: Int
        val EQUAL: Int
        val LEQUAL: Int
        val GREATER: Int
        val NOTEQUAL: Int
        val GEQUAL: Int
        val ALWAYS: Int
        val KEEP: Int
        val REPLACE: Int
        val INCR: Int
        val DECR: Int
        val INVERT: Int
        val INCR_WRAP: Int
        val DECR_WRAP: Int
        val VENDOR: Int
        val RENDERER: Int
        val VERSION: Int
        val NEAREST: Int
        val LINEAR: Int
        val NEAREST_MIPMAP_NEAREST: Int
        val LINEAR_MIPMAP_NEAREST: Int
        val NEAREST_MIPMAP_LINEAR: Int
        val LINEAR_MIPMAP_LINEAR: Int
        val TEXTURE_MAG_FILTER: Int
        val TEXTURE_MIN_FILTER: Int
        val TEXTURE_WRAP_S: Int
        val TEXTURE_WRAP_T: Int
        val TEXTURE_2D: Int
        val TEXTURE: Int
        val TEXTURE_CUBE_MAP: Int
        val TEXTURE_BINDING_CUBE_MAP: Int
        val TEXTURE_CUBE_MAP_POSITIVE_X: Int
        val TEXTURE_CUBE_MAP_NEGATIVE_X: Int
        val TEXTURE_CUBE_MAP_POSITIVE_Y: Int
        val TEXTURE_CUBE_MAP_NEGATIVE_Y: Int
        val TEXTURE_CUBE_MAP_POSITIVE_Z: Int
        val TEXTURE_CUBE_MAP_NEGATIVE_Z: Int
        val MAX_CUBE_MAP_TEXTURE_SIZE: Int
        val TEXTURE0: Int
        val TEXTURE1: Int
        val TEXTURE2: Int
        val TEXTURE3: Int
        val TEXTURE4: Int
        val TEXTURE5: Int
        val TEXTURE6: Int
        val TEXTURE7: Int
        val TEXTURE8: Int
        val TEXTURE9: Int
        val TEXTURE10: Int
        val TEXTURE11: Int
        val TEXTURE12: Int
        val TEXTURE13: Int
        val TEXTURE14: Int
        val TEXTURE15: Int
        val TEXTURE16: Int
        val TEXTURE17: Int
        val TEXTURE18: Int
        val TEXTURE19: Int
        val TEXTURE20: Int
        val TEXTURE21: Int
        val TEXTURE22: Int
        val TEXTURE23: Int
        val TEXTURE24: Int
        val TEXTURE25: Int
        val TEXTURE26: Int
        val TEXTURE27: Int
        val TEXTURE28: Int
        val TEXTURE29: Int
        val TEXTURE30: Int
        val TEXTURE31: Int
        val ACTIVE_TEXTURE: Int
        val REPEAT: Int
        val CLAMP_TO_EDGE: Int
        val MIRRORED_REPEAT: Int
        val FLOAT_VEC2: Int
        val FLOAT_VEC3: Int
        val FLOAT_VEC4: Int
        val INT_VEC2: Int
        val INT_VEC3: Int
        val INT_VEC4: Int
        val BOOL: Int
        val BOOL_VEC2: Int
        val BOOL_VEC3: Int
        val BOOL_VEC4: Int
        val FLOAT_MAT2: Int
        val FLOAT_MAT3: Int
        val FLOAT_MAT4: Int
        val SAMPLER_2D: Int
        val SAMPLER_CUBE: Int
        val VERTEX_ATTRIB_ARRAY_ENABLED: Int
        val VERTEX_ATTRIB_ARRAY_SIZE: Int
        val VERTEX_ATTRIB_ARRAY_STRIDE: Int
        val VERTEX_ATTRIB_ARRAY_TYPE: Int
        val VERTEX_ATTRIB_ARRAY_NORMALIZED: Int
        val VERTEX_ATTRIB_ARRAY_POINTER: Int
        val VERTEX_ATTRIB_ARRAY_BUFFER_BINDING: Int
        val IMPLEMENTATION_COLOR_READ_TYPE: Int
        val IMPLEMENTATION_COLOR_READ_FORMAT: Int
        val COMPILE_STATUS: Int
        val LOW_FLOAT: Int
        val MEDIUM_FLOAT: Int
        val HIGH_FLOAT: Int
        val LOW_INT: Int
        val MEDIUM_INT: Int
        val HIGH_INT: Int
        val FRAMEBUFFER: Int
        val RENDERBUFFER: Int
        val RGBA4: Int
        val RGB5_A1: Int
        val RGB565: Int
        val DEPTH_COMPONENT16: Int
        val STENCIL_INDEX: Int
        val STENCIL_INDEX8: Int
        val DEPTH_STENCIL: Int
        val RENDERBUFFER_WIDTH: Int
        val RENDERBUFFER_HEIGHT: Int
        val RENDERBUFFER_INTERNAL_FORMAT: Int
        val RENDERBUFFER_RED_SIZE: Int
        val RENDERBUFFER_GREEN_SIZE: Int
        val RENDERBUFFER_BLUE_SIZE: Int
        val RENDERBUFFER_ALPHA_SIZE: Int
        val RENDERBUFFER_DEPTH_SIZE: Int
        val RENDERBUFFER_STENCIL_SIZE: Int
        val FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE: Int
        val FRAMEBUFFER_ATTACHMENT_OBJECT_NAME: Int
        val FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL: Int
        val FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE: Int
        val COLOR_ATTACHMENT0: Int
        val DEPTH_ATTACHMENT: Int
        val STENCIL_ATTACHMENT: Int
        val DEPTH_STENCIL_ATTACHMENT: Int
        val NONE: Int
        val FRAMEBUFFER_COMPLETE: Int
        val FRAMEBUFFER_INCOMPLETE_ATTACHMENT: Int
        val FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT: Int
        val FRAMEBUFFER_INCOMPLETE_DIMENSIONS: Int
        val FRAMEBUFFER_UNSUPPORTED: Int
        val FRAMEBUFFER_BINDING: Int
        val RENDERBUFFER_BINDING: Int
        val MAX_RENDERBUFFER_SIZE: Int
        val INVALID_FRAMEBUFFER_OPERATION: Int
        val UNPACK_FLIP_Y_WEBGL: Int
        val UNPACK_PREMULTIPLY_ALPHA_WEBGL: Int
        val CONTEXT_LOST_WEBGL: Int
        val UNPACK_COLORSPACE_CONVERSION_WEBGL: Int
        val BROWSER_DEFAULT_WEBGL: Int
    }
}

/**
 * Exposes the JavaScript [WebGLContextEvent](https://developer.mozilla.org/en/docs/Web/API/WebGLContextEvent) to Kotlin
 */
public external open class WebGLContextEvent(type: String, eventInit: WebGLContextEventInit = definedExternally) : Event {
    open val statusMessage: String
}

public external interface WebGLContextEventInit : EventInit {
    var statusMessage: String? /* = "" */
        get() = definedExternally
        set(value) = definedExternally
}

@kotlin.internal.InlineOnly
public inline fun WebGLContextEventInit(statusMessage: String? = "", bubbles: Boolean? = false, cancelable: Boolean? = false, composed: Boolean? = false): WebGLContextEventInit {
    val o = js("({})")

    o["statusMessage"] = statusMessage
    o["bubbles"] = bubbles
    o["cancelable"] = cancelable
    o["composed"] = composed

    return o
}

/**
 * Exposes the JavaScript [ArrayBuffer](https://developer.mozilla.org/en/docs/Web/API/ArrayBuffer) to Kotlin
 */
public external open class ArrayBuffer(length: Int) : BufferDataSource {
    open val byteLength: Int
    fun slice(begin: Int, end: Int = definedExternally): ArrayBuffer

    companion object {
        fun isView(value: Any?): Boolean
    }
}

/**
 * Exposes the JavaScript [ArrayBufferView](https://developer.mozilla.org/en/docs/Web/API/ArrayBufferView) to Kotlin
 */
public external interface ArrayBufferView : BufferDataSource {
    val buffer: ArrayBuffer
    val byteOffset: Int
    val byteLength: Int
}

/**
 * Exposes the JavaScript [Int8Array](https://developer.mozilla.org/en/docs/Web/API/Int8Array) to Kotlin
 */
public external open class Int8Array : ArrayBufferView {
    constructor(length: Int)
    constructor(array: Int8Array)
    constructor(array: Array<Byte>)
    constructor(buffer: ArrayBuffer, byteOffset: Int = definedExternally, length: Int = definedExternally)
    open val length: Int
    override val buffer: ArrayBuffer
    override val byteOffset: Int
    override val byteLength: Int
    fun set(array: Int8Array, offset: Int = definedExternally): Unit
    fun set(array: Array<Byte>, offset: Int = definedExternally): Unit
    fun subarray(start: Int, end: Int): Int8Array

    companion object {
        val BYTES_PER_ELEMENT: Int
    }
}
@kotlin.internal.InlineOnly inline operator fun Int8Array.get(index: Int): Byte = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun Int8Array.set(index: Int, value: Byte): Unit { asDynamic()[index] = value; }

/**
 * Exposes the JavaScript [Uint8Array](https://developer.mozilla.org/en/docs/Web/API/Uint8Array) to Kotlin
 */
public external open class Uint8Array : ArrayBufferView {
    constructor(length: Int)
    constructor(array: Uint8Array)
    constructor(array: Array<Byte>)
    constructor(buffer: ArrayBuffer, byteOffset: Int = definedExternally, length: Int = definedExternally)
    open val length: Int
    override val buffer: ArrayBuffer
    override val byteOffset: Int
    override val byteLength: Int
    fun set(array: Uint8Array, offset: Int = definedExternally): Unit
    fun set(array: Array<Byte>, offset: Int = definedExternally): Unit
    fun subarray(start: Int, end: Int): Uint8Array

    companion object {
        val BYTES_PER_ELEMENT: Int
    }
}
@kotlin.internal.InlineOnly inline operator fun Uint8Array.get(index: Int): Byte = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun Uint8Array.set(index: Int, value: Byte): Unit { asDynamic()[index] = value; }

/**
 * Exposes the JavaScript [Uint8ClampedArray](https://developer.mozilla.org/en/docs/Web/API/Uint8ClampedArray) to Kotlin
 */
public external open class Uint8ClampedArray : ArrayBufferView {
    constructor(length: Int)
    constructor(array: Uint8ClampedArray)
    constructor(array: Array<Byte>)
    constructor(buffer: ArrayBuffer, byteOffset: Int = definedExternally, length: Int = definedExternally)
    open val length: Int
    override val buffer: ArrayBuffer
    override val byteOffset: Int
    override val byteLength: Int
    fun set(array: Uint8ClampedArray, offset: Int = definedExternally): Unit
    fun set(array: Array<Byte>, offset: Int = definedExternally): Unit
    fun subarray(start: Int, end: Int): Uint8ClampedArray

    companion object {
        val BYTES_PER_ELEMENT: Int
    }
}
@kotlin.internal.InlineOnly inline operator fun Uint8ClampedArray.get(index: Int): Byte = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun Uint8ClampedArray.set(index: Int, value: Byte): Unit { asDynamic()[index] = value; }

/**
 * Exposes the JavaScript [Int16Array](https://developer.mozilla.org/en/docs/Web/API/Int16Array) to Kotlin
 */
public external open class Int16Array : ArrayBufferView {
    constructor(length: Int)
    constructor(array: Int16Array)
    constructor(array: Array<Short>)
    constructor(buffer: ArrayBuffer, byteOffset: Int = definedExternally, length: Int = definedExternally)
    open val length: Int
    override val buffer: ArrayBuffer
    override val byteOffset: Int
    override val byteLength: Int
    fun set(array: Int16Array, offset: Int = definedExternally): Unit
    fun set(array: Array<Short>, offset: Int = definedExternally): Unit
    fun subarray(start: Int, end: Int): Int16Array

    companion object {
        val BYTES_PER_ELEMENT: Int
    }
}
@kotlin.internal.InlineOnly inline operator fun Int16Array.get(index: Int): Short = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun Int16Array.set(index: Int, value: Short): Unit { asDynamic()[index] = value; }

/**
 * Exposes the JavaScript [Uint16Array](https://developer.mozilla.org/en/docs/Web/API/Uint16Array) to Kotlin
 */
public external open class Uint16Array : ArrayBufferView {
    constructor(length: Int)
    constructor(array: Uint16Array)
    constructor(array: Array<Short>)
    constructor(buffer: ArrayBuffer, byteOffset: Int = definedExternally, length: Int = definedExternally)
    open val length: Int
    override val buffer: ArrayBuffer
    override val byteOffset: Int
    override val byteLength: Int
    fun set(array: Uint16Array, offset: Int = definedExternally): Unit
    fun set(array: Array<Short>, offset: Int = definedExternally): Unit
    fun subarray(start: Int, end: Int): Uint16Array

    companion object {
        val BYTES_PER_ELEMENT: Int
    }
}
@kotlin.internal.InlineOnly inline operator fun Uint16Array.get(index: Int): Short = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun Uint16Array.set(index: Int, value: Short): Unit { asDynamic()[index] = value; }

/**
 * Exposes the JavaScript [Int32Array](https://developer.mozilla.org/en/docs/Web/API/Int32Array) to Kotlin
 */
public external open class Int32Array : ArrayBufferView {
    constructor(length: Int)
    constructor(array: Int32Array)
    constructor(array: Array<Int>)
    constructor(buffer: ArrayBuffer, byteOffset: Int = definedExternally, length: Int = definedExternally)
    open val length: Int
    override val buffer: ArrayBuffer
    override val byteOffset: Int
    override val byteLength: Int
    fun set(array: Int32Array, offset: Int = definedExternally): Unit
    fun set(array: Array<Int>, offset: Int = definedExternally): Unit
    fun subarray(start: Int, end: Int): Int32Array

    companion object {
        val BYTES_PER_ELEMENT: Int
    }
}
@kotlin.internal.InlineOnly inline operator fun Int32Array.get(index: Int): Int = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun Int32Array.set(index: Int, value: Int): Unit { asDynamic()[index] = value; }

/**
 * Exposes the JavaScript [Uint32Array](https://developer.mozilla.org/en/docs/Web/API/Uint32Array) to Kotlin
 */
public external open class Uint32Array : ArrayBufferView {
    constructor(length: Int)
    constructor(array: Uint32Array)
    constructor(array: Array<Int>)
    constructor(buffer: ArrayBuffer, byteOffset: Int = definedExternally, length: Int = definedExternally)
    open val length: Int
    override val buffer: ArrayBuffer
    override val byteOffset: Int
    override val byteLength: Int
    fun set(array: Uint32Array, offset: Int = definedExternally): Unit
    fun set(array: Array<Int>, offset: Int = definedExternally): Unit
    fun subarray(start: Int, end: Int): Uint32Array

    companion object {
        val BYTES_PER_ELEMENT: Int
    }
}
@kotlin.internal.InlineOnly inline operator fun Uint32Array.get(index: Int): Int = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun Uint32Array.set(index: Int, value: Int): Unit { asDynamic()[index] = value; }

/**
 * Exposes the JavaScript [Float32Array](https://developer.mozilla.org/en/docs/Web/API/Float32Array) to Kotlin
 */
public external open class Float32Array : ArrayBufferView {
    constructor(length: Int)
    constructor(array: Float32Array)
    constructor(array: Array<Float>)
    constructor(buffer: ArrayBuffer, byteOffset: Int = definedExternally, length: Int = definedExternally)
    open val length: Int
    override val buffer: ArrayBuffer
    override val byteOffset: Int
    override val byteLength: Int
    fun set(array: Float32Array, offset: Int = definedExternally): Unit
    fun set(array: Array<Float>, offset: Int = definedExternally): Unit
    fun subarray(start: Int, end: Int): Float32Array

    companion object {
        val BYTES_PER_ELEMENT: Int
    }
}
@kotlin.internal.InlineOnly inline operator fun Float32Array.get(index: Int): Float = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun Float32Array.set(index: Int, value: Float): Unit { asDynamic()[index] = value; }

/**
 * Exposes the JavaScript [Float64Array](https://developer.mozilla.org/en/docs/Web/API/Float64Array) to Kotlin
 */
public external open class Float64Array : ArrayBufferView {
    constructor(length: Int)
    constructor(array: Float64Array)
    constructor(array: Array<Double>)
    constructor(buffer: ArrayBuffer, byteOffset: Int = definedExternally, length: Int = definedExternally)
    open val length: Int
    override val buffer: ArrayBuffer
    override val byteOffset: Int
    override val byteLength: Int
    fun set(array: Float64Array, offset: Int = definedExternally): Unit
    fun set(array: Array<Double>, offset: Int = definedExternally): Unit
    fun subarray(start: Int, end: Int): Float64Array

    companion object {
        val BYTES_PER_ELEMENT: Int
    }
}
@kotlin.internal.InlineOnly inline operator fun Float64Array.get(index: Int): Double = asDynamic()[index]
@kotlin.internal.InlineOnly inline operator fun Float64Array.set(index: Int, value: Double): Unit { asDynamic()[index] = value; }

/**
 * Exposes the JavaScript [DataView](https://developer.mozilla.org/en/docs/Web/API/DataView) to Kotlin
 */
public external open class DataView(buffer: ArrayBuffer, byteOffset: Int = definedExternally, byteLength: Int = definedExternally) : ArrayBufferView {
    override val buffer: ArrayBuffer
    override val byteOffset: Int
    override val byteLength: Int
    fun getInt8(byteOffset: Int): Byte
    fun getUint8(byteOffset: Int): Byte
    fun getInt16(byteOffset: Int, littleEndian: Boolean = definedExternally): Short
    fun getUint16(byteOffset: Int, littleEndian: Boolean = definedExternally): Short
    fun getInt32(byteOffset: Int, littleEndian: Boolean = definedExternally): Int
    fun getUint32(byteOffset: Int, littleEndian: Boolean = definedExternally): Int
    fun getFloat32(byteOffset: Int, littleEndian: Boolean = definedExternally): Float
    fun getFloat64(byteOffset: Int, littleEndian: Boolean = definedExternally): Double
    fun setInt8(byteOffset: Int, value: Byte): Unit
    fun setUint8(byteOffset: Int, value: Byte): Unit
    fun setInt16(byteOffset: Int, value: Short, littleEndian: Boolean = definedExternally): Unit
    fun setUint16(byteOffset: Int, value: Short, littleEndian: Boolean = definedExternally): Unit
    fun setInt32(byteOffset: Int, value: Int, littleEndian: Boolean = definedExternally): Unit
    fun setUint32(byteOffset: Int, value: Int, littleEndian: Boolean = definedExternally): Unit
    fun setFloat32(byteOffset: Int, value: Float, littleEndian: Boolean = definedExternally): Unit
    fun setFloat64(byteOffset: Int, value: Double, littleEndian: Boolean = definedExternally): Unit
}

public external @marker interface BufferDataSource {
}

public external @marker interface TexImageSource {
}

