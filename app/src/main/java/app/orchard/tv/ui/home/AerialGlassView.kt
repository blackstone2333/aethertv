package app.orchard.tv.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.Surface
import androidx.compose.ui.geometry.Rect
import app.orchard.tv.system.DiagLog
import app.orchard.tv.ui.components.GlassOptics
import app.orchard.tv.wallpaper.Images
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.coroutines.resume

/** One decoder, one OES texture, one screen pass. The lens samples the very same video frame. */
class AerialGlassView(context: Context, private val ready: (Surface?) -> Unit, private val failed: () -> Unit) : GLSurfaceView(context) {
    private val lens = VideoRenderer()
    @Volatile private var closed = false
    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(lens)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun plate(rect: Rect?, density: Float, frosted: Boolean, alpha: Float, brightness: Float = .94f, radius: Float = GlassOptics.RADIUS_DP) {
        val next = Plate(rect ?: Rect.Zero, density, frosted, if (rect == null) 0f else alpha, brightness, radius)
        if (lens.plate != next) { lens.plate = next; requestRender() }
    }

    /** Read the undecorated frame offscreen only on pause; never burn the dock into the wallpaper. */
    suspend fun rawFrame(): Bitmap? = suspendCancellableCoroutine { continuation ->
        queueEvent {
            val bitmap = runCatching { lens.capture() }.getOrNull()
            if (continuation.isActive) continuation.resume(bitmap) else bitmap?.recycle()
        }
        requestRender()
    }

    fun close() {
        if (closed) return
        closed = true
        ready(null)
        queueEvent { lens.release() }
    }

    private data class Plate(val rect: Rect = Rect.Zero, val density: Float = 1f, val frosted: Boolean = false, val alpha: Float = 0f, val brightness: Float = .94f, val radius: Float = GlassOptics.RADIUS_DP)

    private inner class VideoRenderer : Renderer {
        @Volatile var plate = Plate()
        private val pending = AtomicBoolean(false)
        private var texture: SurfaceTexture? = null
        private var output: Surface? = null
        private var oes = 0
        private var program = 0
        private var width = 1; private var height = 1
        private var hasFrame = false
        private val transform = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
        private val vertices = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
            put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)); position(0)
        }
        private var position = 0; private var matrix = 0; private var resolution = 0
        private var bounds = 0; private var optical = 0; private var enabled = 0
        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            if (closed) return
            runCatching { initialize() }.onFailure {
                DiagLog.i("wallpaper glass unavailable: ${it.javaClass.simpleName}")
                release(); post { if (!closed) failed() }
            }
        }
        private fun initialize() {
            release()
            val vertex = shader(GL_VERTEX_SHADER, VERTEX)
            val fragment = shader(GL_FRAGMENT_SHADER, FRAGMENT)
            program = glCreateProgram().also {
                glAttachShader(it, vertex); glAttachShader(it, fragment); glLinkProgram(it)
                val ok = IntArray(1); glGetProgramiv(it, GL_LINK_STATUS, ok, 0)
                check(ok[0] == GL_TRUE) { glGetProgramInfoLog(it) }
            }
            glDeleteShader(vertex); glDeleteShader(fragment)
            position = glGetAttribLocation(program, "aPosition")
            matrix = glGetUniformLocation(program, "uTransform")
            resolution = glGetUniformLocation(program, "uResolution")
            bounds = glGetUniformLocation(program, "uPlate")
            optical = glGetUniformLocation(program, "uOptical")
            enabled = glGetUniformLocation(program, "uEnabled")
            val ids = IntArray(1); glGenTextures(1, ids, 0); oes = ids[0]
            glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oes)
            glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            val producer = SurfaceTexture(oes).also { texture = it }
            producer.setOnFrameAvailableListener { pending.set(true); requestRender() }
            val surface = Surface(producer).also { output = it }
            post { if (!closed && output === surface) ready(surface) }
            DiagLog.i("wallpaper glass: OES shader ready")
        }
        override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) { width = w; height = h; glViewport(0, 0, w, h) }
        override fun onDrawFrame(gl: GL10?) {
            if (closed || program == 0) return
            if (pending.getAndSet(false)) {
                texture?.updateTexImage(); texture?.getTransformMatrix(transform); hasFrame = true
            }
            draw(true)
        }
        private fun draw(decorated: Boolean) {
            glClearColor(0f, 0f, 0f, 1f); glClear(GL_COLOR_BUFFER_BIT)
            if (!hasFrame) return
            glUseProgram(program)
            glActiveTexture(GL_TEXTURE0); glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oes)
            glUniform1i(glGetUniformLocation(program, "uVideo"), 0)
            glUniformMatrix4fv(matrix, 1, false, transform, 0)
            glUniform2f(resolution, width.toFloat(), height.toFloat())
            val p = plate
            glUniform4f(bounds, p.rect.left, p.rect.top, p.rect.right, p.rect.bottom)
            glUniform4f(optical, p.radius * p.density, GlassOptics.DEPTH_DP * p.density,
                GlassOptics.BEND_DP * p.density, if (p.frosted) 1f else 0f)
            glUniform1f(enabled, if (decorated) p.alpha else 0f)
            glUniform1f(glGetUniformLocation(program, "uBrightness"), p.brightness)
            glUniform1f(glGetUniformLocation(program, "uDensity"), p.density)
            glUniform4fv(glGetUniformLocation(program, "uShade"), 1, Images.SHADE, 0)
            glUniform4fv(glGetUniformLocation(program, "uShadeAt"), 1, Images.SHADE_AT, 0)
            glUniform1f(glGetUniformLocation(program, "uShadeLeft"), Images.SHADE_LEFT)
            vertices.position(0); glVertexAttribPointer(position, 2, GL_FLOAT, false, 0, vertices)
            glEnableVertexAttribArray(position); glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        }
        fun capture(): Bitmap? {
            if (!hasFrame || closed) return null
            val ids = IntArray(1); val fb = IntArray(1)
            glGenTextures(1, ids, 0); glBindTexture(GL_TEXTURE_2D, ids[0])
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glGenFramebuffers(1, fb, 0); glBindFramebuffer(GL_FRAMEBUFFER, fb[0])
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ids[0], 0)
            return try {
                check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE)
                draw(false)
                val bytes = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
                glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, bytes)
                val raw = IntArray(width * height); bytes.asIntBuffer().get(raw)
                val argb = IntArray(raw.size)
                for (y in 0 until height) for (x in 0 until width) {
                    val c = raw[(height - 1 - y) * width + x]
                    argb[y * width + x] = (c and -0x1000000) or ((c and 255) shl 16) or (c and 0xff00) or ((c ushr 16) and 255)
                }
                DiagLog.i("wallpaper glass: undecorated pause frame ${width}x$height")
                Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888)
            } finally {
                glBindFramebuffer(GL_FRAMEBUFFER, 0); glDeleteFramebuffers(1, fb, 0); glDeleteTextures(1, ids, 0)
                requestRender()
            }
        }
        fun release() {
            texture?.setOnFrameAvailableListener(null); output?.release(); texture?.release()
            texture = null; output = null; hasFrame = false
            if (oes != 0) glDeleteTextures(1, intArrayOf(oes), 0)
            if (program != 0) glDeleteProgram(program)
            oes = 0; program = 0
        }
        private fun shader(type: Int, source: String): Int = glCreateShader(type).also {
            glShaderSource(it, source); glCompileShader(it)
            val ok = IntArray(1); glGetShaderiv(it, GL_COMPILE_STATUS, ok, 0)
            check(ok[0] == GL_TRUE) { glGetShaderInfoLog(it) }
        }
    }

    companion object {
        private const val VERTEX = """
            attribute vec2 aPosition;
            varying vec2 vUv;
            void main() { gl_Position = vec4(aPosition, 0.0, 1.0); vUv = (aPosition + 1.0) * 0.5; }
        """
        private const val FRAGMENT = """
            #extension GL_OES_EGL_image_external : require
            #ifdef GL_FRAGMENT_PRECISION_HIGH
            precision highp float;
            #else
            precision mediump float;
            #endif
            uniform samplerExternalOES uVideo;
            uniform mat4 uTransform;
            uniform vec2 uResolution;
            uniform vec4 uPlate;
            uniform vec4 uOptical;
            uniform float uEnabled;
            uniform float uBrightness;
            uniform float uDensity;
            uniform vec4 uShade;
            uniform vec4 uShadeAt;
            uniform float uShadeLeft;
            varying vec2 vUv;
            vec3 video(vec2 p) {
                vec2 uv = vec2(p.x / uResolution.x, 1.0 - p.y / uResolution.y);
                return texture2D(uVideo, (uTransform * vec4(uv, 0.0, 1.0)).xy).rgb;
            }
            vec3 scene(vec2 p) {
                vec2 uv = p / uResolution;
                float shade;
                if (uv.y < uShadeAt.y) shade = mix(uShade.x, uShade.y, clamp(uv.y/uShadeAt.y, 0.0, 1.0));
                else if (uv.y < uShadeAt.z) shade = mix(uShade.y, uShade.z, (uv.y-uShadeAt.y)/(uShadeAt.z-uShadeAt.y));
                else shade = mix(uShade.z, uShade.w, clamp((uv.y-uShadeAt.z)/(uShadeAt.w-uShadeAt.z),0.0,1.0));
                float left = uShadeLeft * max(1.0-uv.x*2.0,0.0);
                return video(p) * (1.0-shade*uEnabled) * (1.0-left*uEnabled);
            }
            void main() {
                vec2 p = vec2(vUv.x, 1.0 - vUv.y) * uResolution;
                vec3 original = scene(p);
                vec3 color = original;
                if (uEnabled > 0.001) {
                    vec2 halfSize = (uPlate.zw - uPlate.xy) * 0.5;
                    vec2 local = p - (uPlate.xy + halfSize);
                    float radius = min(uOptical.x, min(halfSize.x, halfSize.y));
                    vec2 q = abs(local) - halfSize + radius;
                    vec2 outer = max(q, 0.0);
                    float len = length(outer);
                    float d = len + min(max(q.x, q.y), 0.0) - radius;
                    if (d < uOptical.x * 0.22) {
                        if (d <= 0.0) {
                            vec2 normal = (len > 0.001 ? outer / len : (q.x > q.y ? vec2(1.0,0.0) : vec2(0.0,1.0))) * sign(local);
                            float band = clamp(1.0 + d / uOptical.y, 0.0, 1.0);
                            vec2 bent = p - normal * uOptical.z * band * band * (3.0 - 2.0 * band);
                            color = scene(bent);
                            if (uOptical.w > 0.5) {
                                vec2 o = vec2(uOptical.y * 0.6);
                                color = (color * 2.0 + scene(bent+vec2(o.x,0.0)) + scene(bent-vec2(o.x,0.0)) + scene(bent+vec2(0.0,o.y)) + scene(bent-vec2(0.0,o.y))) / 6.0;
                                color = mix(color, vec3(0.09), 0.32);
                            } else { color *= uBrightness; }
                            float px = uDensity;
                            float dark = exp(-pow((d + 1.8*px) / (1.15*px), 2.0)) * 0.22;
                            float light = exp(-pow((d + 0.55*px) / (0.6*px), 2.0));
                            float vertical = pow(abs(normal.y), 3.0);
                            float specular = mix(0.09, normal.y < 0.0 ? 0.58 : 0.44, vertical);
                            color = color * (1.0-dark);
                            color = mix(color, mix(vec3(0.49,0.51,0.54), vec3(1.0), vertical), light * specular);
                            color = mix(original, color, smoothstep(0.0, px, -d) * uEnabled);
                        } else {
                            float shadow = exp(-d / (uOptical.x * 0.08)) * 0.12 * uEnabled;
                            color *= 1.0 - shadow;
                        }
                    }
                }
                gl_FragColor = vec4(color, 1.0);
            }
        """
    }
}
