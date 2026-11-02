package app.orchard.tv.system
import android.content.Context
import android.media.AudioManager
object Sounds {
    private var audio: AudioManager? = null
    fun init(context: Context) { audio = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    fun focusMoved() = Unit
    fun select() { audio?.playSoundEffect(AudioManager.FX_KEY_CLICK) }
}
