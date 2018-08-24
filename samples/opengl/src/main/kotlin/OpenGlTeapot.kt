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
import platform.GLUT.*
import platform.OpenGL.*
import platform.OpenGLCommon.*

// Ported from http://openglsamples.sourceforge.net/projects/index.php/blog/index/

private var rotation: GLfloat = 0.0f
private val rotationSpeed: GLfloat = 0.2f

private val windowWidth = 640
private val windowHeight = 480

fun display() {
    // Clear Screen and Depth Buffer
    glClear((GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT).convert())
    glLoadIdentity()

    // Define a viewing transformation
    gluLookAt(4.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)

    // Push and pop the current matrix stack.
    // This causes that translations and rotations on this matrix wont influence others.

    glPushMatrix()
    glColor3f(1.0f, 0.0f, 0.0f)
    glTranslatef(0.0f, 0.0f, 0.0f)
    glRotatef(rotation, 0.0f, 1.0f, 0.0f)
    glRotatef(90.0f, 0.0f, 1.0f, 0.0f)

    // Draw the teapot
    glutSolidTeapot(1.0)
    glPopMatrix()


    rotation += rotationSpeed
    glutSwapBuffers()
}


fun initialize() {
    // select projection matrix
    glMatrixMode(GL_PROJECTION.convert())

    // set the viewport
    glViewport(0, 0, windowWidth, windowHeight)

    // set matrix mode
    glMatrixMode(GL_PROJECTION.convert())

    // reset projection matrix
    glLoadIdentity()
    val aspect = windowWidth.toDouble() / windowHeight

    // set up a perspective projection matrix
    gluPerspective(45.0, aspect, 1.0, 500.0)

    // specify which matrix is the current matrix
    glMatrixMode(GL_MODELVIEW.convert())
    glShadeModel(GL_SMOOTH.convert())

    // specify the clear value for the depth buffer
    glClearDepth(1.0)
    glEnable(GL_DEPTH_TEST.convert())
    glDepthFunc(GL_LEQUAL.convert())

    // specify implementation-specific hints
    glHint(GL_PERSPECTIVE_CORRECTION_HINT.convert(), GL_NICEST.convert())

    glLightModelfv(GL_LIGHT_MODEL_AMBIENT.convert(), cValuesOf(0.1f, 0.1f, 0.1f, 1.0f))
    glLightfv(GL_LIGHT0.convert(), GL_DIFFUSE.convert(), cValuesOf(0.6f, 0.6f, 0.6f, 1.0f))
    glLightfv(GL_LIGHT0.convert(), GL_SPECULAR.convert(), cValuesOf(0.7f, 0.7f, 0.3f, 1.0f))

    glEnable(GL_LIGHT0.convert())
    glEnable(GL_COLOR_MATERIAL.convert())
    glShadeModel(GL_SMOOTH.convert())
    glLightModeli(GL_LIGHT_MODEL_TWO_SIDE.convert(), GL_FALSE)
    glDepthFunc(GL_LEQUAL.convert())
    glEnable(GL_DEPTH_TEST.convert())
    glEnable(GL_LIGHTING.convert())
    glEnable(GL_LIGHT0.convert())
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
}

fun main(args: Array<String>) {
    // initialize and run program
    memScoped {
        val argc = alloc<IntVar>().apply { value = 0 }
        glutInit(argc.ptr, null) // TODO: pass real args
    }

    // Display Mode
    glutInitDisplayMode((GLUT_RGB or GLUT_DOUBLE or GLUT_DEPTH).convert())

    // Set window size
    glutInitWindowSize(windowWidth, windowHeight)

    // create Window
    glutCreateWindow("The GLUT Teapot")

    // register Display Function
    glutDisplayFunc(staticCFunction(::display))

    // register Idle Function
    glutIdleFunc(staticCFunction(::display))

    initialize()

    // run GLUT mainloop
    glutMainLoop()
}