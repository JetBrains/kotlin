/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package sample.androidnative

import kotlinx.cinterop.*
import platform.android.*
import platform.egl.*
import platform.posix.*
import platform.gles.*
import sample.androidnative.bmpformat.BMPHeader

class Renderer(val container: DisposableContainer,
               val nativeActivity: ANativeActivity,
               val savedMatrix: COpaquePointer?) {
    private var display: EGLDisplay? = null
    private var surface: EGLSurface? = null
    private var context: EGLContext? = null
    private var initialized = false

    var screen = Vector2.Zero

    private val matrix = container.arena.allocArray<FloatVar>(16)

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
        with(container.arena) {
            logInfo("Initializing context..")
            display = eglGetDisplay(null)
            if (display == null) {
                logError("eglGetDisplay() returned error ${eglGetError()}")
                return false
            }
            if (eglInitialize(display, null, null) == 0u) {
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
            if (eglChooseConfig(display, attribs, null, 0, numConfigs.ptr) == 0u) {
                throw Error("eglChooseConfig()#1 returned error ${eglGetError()}")
            }
            val supportedConfigs = allocArray<EGLConfigVar>(numConfigs.value)
            if (eglChooseConfig(display, attribs, supportedConfigs, numConfigs.value, numConfigs.ptr) == 0u) {
                throw Error("eglChooseConfig()#2 returned error ${eglGetError()}")
            }
            var configIndex = 0
            while (configIndex < numConfigs.value) {
                val r = alloc<EGLintVar>()
                val g = alloc<EGLintVar>()
                val b = alloc<EGLintVar>()
                val d = alloc<EGLintVar>()
                if (eglGetConfigAttrib(display, supportedConfigs[configIndex], EGL_RED_SIZE, r.ptr) != 0u &&
                        eglGetConfigAttrib (display, supportedConfigs[configIndex], EGL_GREEN_SIZE, g.ptr) != 0u &&
                eglGetConfigAttrib(display, supportedConfigs[configIndex], EGL_BLUE_SIZE, b.ptr) != 0u &&
                eglGetConfigAttrib(display, supportedConfigs[configIndex], EGL_DEPTH_SIZE, d.ptr) != 0u &&
                r.value == 8 && g.value == 8 && b.value == 8 && d.value == 0) break
                ++configIndex
            }
            if (configIndex >= numConfigs.value)
                configIndex = 0

            surface = eglCreateWindowSurface(display, supportedConfigs[configIndex], window, null)
            if (surface == null) {
                throw Error("eglCreateWindowSurface() returned error ${eglGetError()}")
            }

            context = eglCreateContext(display, supportedConfigs[configIndex], null, null)
            if (context == null) {
                throw Error("eglCreateContext() returned error ${eglGetError()}")
            }

            if (eglMakeCurrent(display, surface, surface, context) == 0u) {
                throw Error("eglMakeCurrent() returned error ${eglGetError()}")
            }

            val width = alloc<EGLintVar>()
            val height = alloc<EGLintVar>()
            if (eglQuerySurface(display, surface, EGL_WIDTH, width.ptr) == 0u
                    || eglQuerySurface (display, surface, EGL_HEIGHT, height.ptr) == 0u) {
            throw Error("eglQuerySurface() returned error ${eglGetError()}")
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
            checkErrors()
            glLoadIdentity()
            glFrustumf(-ratio, ratio, -1.0f, 1.0f, 1.0f, 10.0f)

            glMatrixMode(GL_MODELVIEW)
            checkErrors()
            glTranslatef(0.0f, 0.0f, -2.0f)
            glLightfv(GL_LIGHT0, GL_POSITION, cValuesOf(1.25f, 1.25f, -2.0f, 0.0f))
            glEnable(GL_LIGHTING)
            glEnable(GL_LIGHT0)
            glEnable(GL_TEXTURE_2D)
            glMaterialfv(GL_FRONT_AND_BACK, GL_DIFFUSE, cValuesOf(0.0f, 1.0f, 1.0f, 1.0f))
            glMaterialfv(GL_FRONT_AND_BACK, GL_SPECULAR, cValuesOf(0.3f, 0.3f, 0.3f, 1.0f))
            glMaterialf(GL_FRONT_AND_BACK, GL_SHININESS, 30.0f)

            loadTexture("kotlin_logo.bmp")

            initialized = true
            return true
        }
    }

    fun checkErrors() {
        val error = glGetError()
        if (error.toInt() != GL_NO_ERROR)
            throw Error("OpenGL error 0x${error.toInt().toString(16)}")
    }

    fun getState() = matrix to 16 * 4

    fun rotateBy(vector: Vector2) {
        if (!initialized) return

        val length = vector.length
        if (length < 1e-9f) return
        val angle = 180 * length / screen.length
        val x = -vector.y / length
        val y = -vector.x / length

        glPushMatrix()
        glMatrixMode(GL_MODELVIEW)
        checkErrors()
        glLoadIdentity()
        glRotatef(angle, x, y, 0.0f)
        glMultMatrixf(matrix)
        glGetFloatv(GL_MODELVIEW_MATRIX, matrix)
        glPopMatrix()
    }


    private fun loadTexture(assetName: String): Unit = memScoped {
        val asset = AAssetManager_open(nativeActivity.assetManager, assetName, AASSET_MODE_BUFFER.convert())
                ?: throw Error("Error opening asset $assetName")
        println("loading texture $assetName")
        try {
            val length = AAsset_getLength(asset)
            val buffer = allocArray<ByteVar>(length)
            if (AAsset_read(asset, buffer, length.convert()) != length.toInt()) {
                throw Error("Error reading asset")
            }

            with(buffer.reinterpret<BMPHeader>().pointed) {
                if (magic != 0x4d42.toUShort() || zero != 0u || size != length.toUInt() || bits != 24.toUShort()) {
                    throw Error("Error parsing texture file")
                }
                val numberOfBytes = width * height * 3
                // Swap BGR in bitmap to RGB.
                for (i in 0 until numberOfBytes step 3) {
                    val t = data[i]
                    data[i] = data[i + 2]
                    data[i + 2] = t
                }
                println("loaded texture ${width}x${height}")
                glBindTexture(GL_TEXTURE_2D, 1)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
                glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_BLEND.toFloat())
                glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, cValuesOf(1.0f, 1.0f, 1.0f, 1.0f))
                glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, data)

            }
        } finally {
            AAsset_close(asset)
        }
    }

    private val texturePoints = arrayOf(
            Vector2(0.0f, 0.2f), Vector2(0.0f, 0.8f), Vector2(0.6f, 1.0f), Vector2(1.0f, 0.5f), Vector2(0.8f, 0.0f)
    )

    private val scale = 1.25f

    fun draw(): Unit {
        if (!initialized) return

        glPushMatrix()
        glMatrixMode(GL_MODELVIEW)
        checkErrors()

        glMultMatrixf(matrix)

        glClear((GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT).toUInt())

        glEnableClientState(GL_VERTEX_ARRAY)
        glEnableClientState(GL_NORMAL_ARRAY)
        glEnableClientState(GL_TEXTURE_COORD_ARRAY)

        val polygon = RegularPolyhedra.Dodecahedron
        val vertices = mutableListOf<Float>()
        val texCoords = mutableListOf<Float>()
        val triangles = mutableListOf<Byte>()
        val normals = mutableListOf<Float>()
        for (face in polygon.faces) {
            val u = polygon.vertices[face[2].toInt()] - polygon.vertices[face[1].toInt()]
            val v = polygon.vertices[face[0].toInt()] - polygon.vertices[face[1].toInt()]
            val normal = u.crossProduct(v).normalized()

            val copiedFace = ByteArray(face.size)
            for (j in face.indices) {
                copiedFace[j] = (vertices.size / 4).toByte()
                polygon.vertices[face[j].toInt()].copyCoordinatesTo(vertices)
                vertices.add(scale)
                normal.copyCoordinatesTo(normals)
                texturePoints[j].copyCoordinatesTo(texCoords)
            }

            for (j in 1..face.size - 2) {
                triangles.add(copiedFace[0])
                triangles.add(copiedFace[j])
                triangles.add(copiedFace[j + 1])
            }
        }

        memScoped {
            glFrontFace(GL_CW)
            glVertexPointer(4, GL_FLOAT, 0, vertices.toFloatArray().toCValues().ptr)
            glTexCoordPointer(2, GL_FLOAT, 0, texCoords.toFloatArray().toCValues().ptr)
            glNormalPointer(GL_FLOAT, 0, normals.toFloatArray().toCValues().ptr)
            glDrawElements(GL_TRIANGLES, triangles.size, GL_UNSIGNED_BYTE, triangles.toByteArray().toCValues().ptr)
        }

        glPopMatrix()

        if (eglSwapBuffers(display, surface) == 0u) {
            val error = eglGetError()
            if (error != EGL_BAD_SURFACE)
                throw Error("eglSwapBuffers() returned error $error")
            else {
                if (eglMakeCurrent(display, surface, surface, context) == 0u) {
                    throw Error("Reinit eglMakeCurrent() returned error ${eglGetError()}")
                }
                if (eglSwapBuffers(display, surface) == 0u)
                    throw Error("Bad eglSwapBuffers() after surface reinit: ${eglGetError()}")
            }
        }
    }

    fun start() {
        logInfo("Starting renderer.")
        if (initialized) {
            if (eglMakeCurrent(display, surface, surface, context) == 0u) {
                throw Error("eglMakeCurrent() returned error ${eglGetError()}")
            }
        }
    }

    fun stop() {
        logInfo("Stopping renderer..")
        eglMakeCurrent(display, null, null, null)
    }

    fun destroy() {
        if (!initialized) return

        logInfo("Destroying renderer..")
        eglMakeCurrent(display, null, null, null)

        context?.let { eglDestroyContext(display, it) }
        surface?.let { eglDestroySurface(display, it) }
        display?.let { eglTerminate(display) }

        display = null
        surface = null
        context = null
        initialized = false
    }
}
