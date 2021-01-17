import org.jetbrains.skiko.skia.native.*
import platform.OpenGL3.*
import platform.AppKit.*
import kotlinx.cinterop.*
import platform.OpenGLCommon.GLenum


fun openglGetIntegerv(pname: GLenum): UInt {
    var result: UInt = 0U
    memScoped {
        val data = alloc<IntVar>()
        glGetIntegerv(pname, data.ptr);
        result = data.value.toUInt();
    }
    return result
}

fun example(canvas: SkCanvas) {

    val paint: SkPaint = SkPaint()
    paint.setColor(SK_ColorGREEN)

    canvas.clear(SK_ColorRED);

    for (i in 0..200) {
        for (j in 0..i) {
            // TODO: get rid of `.pointed` dereference
            canvas.drawPoint(i.toFloat(), j.toFloat(), paint.ptr);
        }
    }

    // TODO: need memory management.
    // nativeHeap.free(paint)
}

fun raster(width: Int, height: Int, path: String) {

       //val glContext = NSOpenGLContext();
       //glContext.makeCurrentContext()

        val interf = GrGLCreateNativeInterface()

       val context = GrDirectContext.MakeGL(interf)

        val fbId = openglGetIntegerv(GL_DRAW_FRAMEBUFFER_BINDING.toUInt())

       var renderTarget: GrBackendRenderTarget? = null

        // TODO: Skia C++ interop: glInfo is a `struct`, not `class`,
        // so we fallback to C interop here.
        memScoped {
            val glInfo: GrGLFramebufferInfo = alloc<GrGLFramebufferInfo>()
            glInfo.fFBOID = fbId
            glInfo.fFormat = kRGBA8.toUInt()
            renderTarget = GrBackendRenderTarget(width, height, 0, 0, glInfo.readValue())
        }

       val rasterSurface = SkSurface.MakeFromBackendRenderTarget(
            // TODO: C++ interop knows nothing about inheritance.
            context!!.ptr.reinterpret<GrRecordingContext>(),
            renderTarget!!.ptr,
            GrSurfaceOrigin.kBottomLeft_GrSurfaceOrigin,
            colorType = kBGRA_8888_SkColorType,
            colorSpace = SkColorSpace.MakeSRGB(), // TODO: Skia C++ interop: need passing sk_sp.
            surfaceProps = null,
            releaseProc = null,
            releaseContext = null
        )
        // val rasterSurface: SkSurface = SkSurface.MakeRasterN32Premul(width, height, null)
        //    ?: error("No surface")

        // TODO: get rid of `.pointed` dereference
        val rasterCanvas: SkCanvas = rasterSurface?.getCanvas()?.pointed 
            ?: error("Could not get canvas")

        example(rasterCanvas)

        val img = rasterSurface?.makeImageSnapshot()
            ?: error("no snapshot")
        val png = img.encodeToData()
            ?: error("no data")

   
        val out2 = SkFILEWStream(path.cstr)
        out2.write(png.data(), png.size())
}


fun main() {
    val fileName = "picture.png"
    raster(640, 480, fileName)
    println("Look at $fileName")
}

