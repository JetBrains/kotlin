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

import kotlinx.cinterop.*
import platform.android.*
import platform.egl.*
import platform.posix.*
import platform.gles3.*
import platform.gles.GL_PERSPECTIVE_CORRECTION_HINT
import platform.gles.GL_SMOOTH
import platform.gles.GL_PROJECTION
import platform.gles.glLoadIdentity
import platform.gles.glFrustumf
import platform.gles.glMatrixMode
import platform.gles.GL_MODELVIEW
import platform.gles.glTranslatef
import platform.gles.glShadeModel
import platform.gles.GL_LIGHT0
import platform.gles.GL_POSITION
import platform.gles.glMaterialfv
import platform.gles.GL_DIFFUSE
import platform.gles.GL_SHININESS
import platform.gles.glPushMatrix
import platform.gles.glRotatef
import platform.gles.glMultMatrixf
import platform.gles.GL_MODELVIEW_MATRIX
import platform.gles.glPopMatrix
import platform.gles.glTexEnvf
import platform.gles.GL_TEXTURE_ENV
import platform.gles.GL_TEXTURE_ENV_MODE
import platform.gles.glLightfv
import platform.gles.GL_LIGHTING
import platform.gles.GL_SPECULAR
import platform.gles.glMaterialf
import platform.gles.glTexEnvfv
import platform.gles.glEnableClientState
import platform.gles.GL_VERTEX_ARRAY
import platform.gles.GL_NORMAL_ARRAY
import platform.gles.GL_TEXTURE_COORD_ARRAY
import platform.gles.glVertexPointer
import platform.gles.glTexCoordPointer
import platform.gles.glNormalPointer
import platform.gles.GL_TEXTURE_ENV_COLOR


class Renderer(val parentArena: NativePlacement, val nativeActivity: ANativeActivity, val savedMatrix: COpaquePointer?) {

    private val arena = MemScope()
    private var display: EGLDisplay? = null
    private var surface: EGLSurface? = null
    private var context: EGLContext? = null
    private var initialized = false

    var screen = Vector2.Zero

    private val matrix = parentArena.allocArray<FloatVar>(16)

    init {
        if (savedMatrix != null) {
            memcpy(matrix, savedMatrix, 16 * 4)
        } else {
            for (i in 0..3)
                for (j in 0..3)
                    matrix[i * 4 + j] = if (i == j) 1.0f else 0.0f
        }
    }

    fun initialize(window: CPointer<ANativeWindow>): Boolean {
        with(arena) {
            logInfo("Initializing context..")
            display = eglGetDisplay(null)
            if (display == null) {
                logError("eglGetDisplay() returned error ${eglGetError()}")
                return false
            }
            if (eglInitialize(display, null, null) == 0) {
                logError("eglInitialize() returned error ${eglGetError()}")
                return false
            }

            val attribs = cValuesOf(
                    EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
                    EGL_BLUE_SIZE, 8,
                    EGL_GREEN_SIZE, 8,
                    EGL_RED_SIZE, 8,
                    EGL_NONE
            )
            val numConfigs = alloc<EGLintVar>()
            if (eglChooseConfig(display, attribs, null, 0, numConfigs.ptr) == 0) {
                logError("eglChooseConfig()#1 returned error ${eglGetError()}")
                destroy()
                return false
            }
            val supportedConfigs = allocArray<EGLConfigVar>(numConfigs.value)
            if (eglChooseConfig(display, attribs, supportedConfigs, numConfigs.value, numConfigs.ptr) == 0) {
                logError("eglChooseConfig()#2 returned error ${eglGetError()}")
                destroy()
                return false
            }
            var configIndex = 0
            while (configIndex < numConfigs.value) {
                val r = alloc<EGLintVar>()
                val g = alloc<EGLintVar>()
                val b = alloc<EGLintVar>()
                val d = alloc<EGLintVar>()
                if (eglGetConfigAttrib(display, supportedConfigs[configIndex], EGL_RED_SIZE, r.ptr) != 0   &&
                        eglGetConfigAttrib(display, supportedConfigs[configIndex], EGL_GREEN_SIZE, g.ptr) != 0 &&
                eglGetConfigAttrib(display, supportedConfigs[configIndex], EGL_BLUE_SIZE, b.ptr) != 0  &&
                eglGetConfigAttrib(display, supportedConfigs[configIndex], EGL_DEPTH_SIZE, d.ptr) != 0 &&
                r.value == 8 && g.value == 8 && b.value == 8 && d.value == 0 ) {
                    break
                }
                ++configIndex
            }
            if (configIndex >= numConfigs.value)
                configIndex = 0

            surface = eglCreateWindowSurface(display, supportedConfigs[configIndex], window, null)
            if (surface == null) {
                logError("eglCreateWindowSurface() returned error ${eglGetError()}")
                destroy()
                return false
            }

            context = eglCreateContext(display, supportedConfigs[configIndex], null, null)
            if (context == null) {
                logError("eglCreateContext() returned error ${eglGetError()}")
                destroy()
                return false
            }

            if (eglMakeCurrent(display, surface, surface, context) == 0) {
                logError("eglMakeCurrent() returned error ${eglGetError()}")
                destroy()
                return false
            }

            val width = alloc<EGLintVar>()
            val height = alloc<EGLintVar>()
            if (eglQuerySurface(display, surface, EGL_WIDTH, width.ptr) == 0
                    || eglQuerySurface(display, surface, EGL_HEIGHT, height.ptr) == 0) {
                logError("eglQuerySurface() returned error ${eglGetError()}")
                destroy()
                return false
            }

            this@Renderer.screen = Vector2(width.value.toFloat(), height.value.toFloat())

            glDisable(GL_DITHER)
            glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_FASTEST)
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            glEnable(GL_CULL_FACE)
            glShadeModel(GL_SMOOTH)
            glEnable(GL_DEPTH_TEST)

            glViewport(0, 0, width.value, height.value)

            val ratio = width.value.toFloat() / height.value
            glMatrixMode(GL_PROJECTION)
            glLoadIdentity()
            glFrustumf(-ratio, ratio, -1.0f, 1.0f, 1.0f, 10.0f)

            glMatrixMode(GL_MODELVIEW)
            glTranslatef(0.0f, 0.0f, -2.0f)
            glLightfv(GL_LIGHT0, GL_POSITION, cValuesOf(1.25f, 1.25f, -2.0f, 0.0f))
            glEnable(GL_LIGHTING)
            glEnable(GL_LIGHT0)
            glEnable(GL_TEXTURE_2D)
            glMaterialfv(GL_FRONT, GL_DIFFUSE, cValuesOf(0.0f, 1.0f, 1.0f, 1.0f))
            glMaterialfv(GL_FRONT, GL_SPECULAR, cValuesOf(0.3f, 0.3f, 0.3f, 1.0f))
            glMaterialf(GL_FRONT, GL_SHININESS, 30.0f)

            loadTexture("kotlin_logo.bmp")

            initialized = true
            return true
        }
    }

    fun getState() = matrix to 16 * 4

    fun rotateBy(vec: Vector2) {
        if (!initialized) return

        val len = vec.length
        if (len < 1e-9f) return
        val angle = 180 * len / screen.length
        val x = - vec.y / len
        val y = - vec.x / len

        glPushMatrix()
        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()
        glRotatef(angle, x, y, 0.0f)
        glMultMatrixf(matrix)
        glGetFloatv(GL_MODELVIEW_MATRIX, matrix)
        glPopMatrix()
    }

    private class BMPHeader(val rawPtr: NativePtr) {
        inline fun <reified T : CPointed> memberAt(offset: Long): T {
            return interpretPointed<T>(this.rawPtr + offset)
        }

        val magic get() = memberAt<ShortVar>(0).value.toInt()
        val size get() = memberAt<IntVar>(2).value
        val zero get() = memberAt<IntVar>(6).value
        val width get() = memberAt<IntVar>(18).value
        val height get() = memberAt<IntVar>(22).value
        val bits get() = memberAt<ShortVar>(28).value.toInt()
        val data get() = interpretCPointer<ByteVar>(rawPtr + 54) as CArrayPointer<ByteVar>
    }

    private fun loadTexture(assetName: String): Unit = memScoped {
        val asset = AAssetManager_open(nativeActivity.assetManager, assetName, AASSET_MODE_BUFFER)
        if (asset == null) {
            logError("Error opening asset")
            return
        }
        val length = AAsset_getLength(asset)
        val buf = allocArray<ByteVar>(length)
        if (AAsset_read(asset, buf, length) != length.toInt()) {
            logError("Error reading asset")
            AAsset_close(asset)
        }
        with (BMPHeader(buf.rawValue)) {
            if (magic != 0x4d42 || zero != 0 || size != length.toInt() || bits != 24) {
                logError("Error parsing texture file")
                AAsset_close(asset)
                return
            }
            val numberOfBytes = width * height * 3
            for (i in 0 until numberOfBytes step 3) {
                val t = data[i]
                data[i] = data[i + 2]
                data[i + 2] = t
            }
            glBindTexture(GL_TEXTURE_2D, 1)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_BLEND.toFloat())
            glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, cValuesOf(1.0f, 1.0f, 1.0f, 1.0f))
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, data)
            AAsset_close(asset)
        }
    }

    private val texturePoints = arrayOf(
            Vector2(0.0f, 0.2f), Vector2(0.0f, 0.8f), Vector2(0.6f, 1.0f), Vector2(1.0f, 0.5f), Vector2(0.8f, 0.0f)
    )

    private val scale = 1.25f

    fun draw() = memScoped {
        if (!initialized) return

        glPushMatrix()
        glMatrixMode(GL_MODELVIEW)

        glMultMatrixf(matrix)

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        glEnableClientState(GL_VERTEX_ARRAY)
        glEnableClientState(GL_NORMAL_ARRAY)
        glEnableClientState(GL_TEXTURE_COORD_ARRAY)

        val poly = RegularPolyhedra.Dodecahedron
        val vertices = mutableListOf<Float>()
        val texCoords = mutableListOf<Float>()
        val triangles = mutableListOf<Byte>()
        val normals = mutableListOf<Float>()
        for (face in poly.faces) {
            val u = poly.vertices[face[2].toInt()] - poly.vertices[face[1].toInt()]
            val v = poly.vertices[face[0].toInt()] - poly.vertices[face[1].toInt()]
            val normal = u.crossProduct(v).normalized()

            val copiedFace = ByteArray(face.size)
            for (j in face.indices) {
                copiedFace[j] = (vertices.size / 4).toByte()
                poly.vertices[face[j].toInt()].copyCoordinatesTo(vertices)
                vertices.add(scale)
                normal.copyCoordinatesTo(normals)
                texturePoints[j].copyCoordinatesTo(texCoords)
            }

            for (j in 1..face.size-2) {
                triangles.add(copiedFace[0])
                triangles.add(copiedFace[j])
                triangles.add(copiedFace[j + 1])
            }
        }

        glFrontFace(GL_CW)
        glVertexPointer(4, GL_FLOAT, 0, vertices.toFloatArray().toCValues().getPointer(this))
        glTexCoordPointer(2, GL_FLOAT, 0, texCoords.toFloatArray().toCValues().getPointer(this))
        glNormalPointer(GL_FLOAT, 0, normals.toFloatArray().toCValues().getPointer(this))
        glDrawElements(GL_TRIANGLES, triangles.size, GL_UNSIGNED_BYTE, triangles.toByteArray().toCValues().getPointer(this))

        glPopMatrix()

        if (eglSwapBuffers(display, surface) == 0) {
            logError("eglSwapBuffers() returned error ${eglGetError()}")
            destroy()
        }
    }

    fun destroy() {
        if (!initialized) return

        logInfo("Destroying context..")

        eglMakeCurrent(display, null, null, null)
        context?.let { eglDestroyContext(display, it) }
        surface?.let { eglDestroySurface(display, it) }
        eglTerminate(display)

        display = null
        surface = null
        context = null
        initialized = false
        // TODO: What should be called here?
        //arena.clear()
    }
}
