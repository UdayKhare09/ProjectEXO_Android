package dev.uday.projectexo_android.utils

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.uday.projectexo_android.R

class SoundManager private constructor(private val context: Context) {
    private var messageReceivePlayer: MediaPlayer? = null
    private var messageSendPlayer: MediaPlayer? = null
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    init {
        prepareMediaPlayers()
    }

    private fun prepareMediaPlayers() {
        // files are in res/sound
        messageReceivePlayer = MediaPlayer.create(context, R.raw.receive)
        messageSendPlayer = MediaPlayer.create(context, R.raw.send)
    }

    fun playMessageReceived(vibrate: Boolean = true) {
        messageReceivePlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.prepare()
            }
            it.start()
        }

        if (vibrate) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.EFFECT_DOUBLE_CLICK))
        }
    }

    fun playMessageSent() {
        messageSendPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                it.prepare()
            }
            it.start()
        }
    }

    fun release() {
        messageReceivePlayer?.release()
        messageReceivePlayer = null

        messageSendPlayer?.release()
        messageSendPlayer = null
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile private var instance: SoundManager? = null

        fun getInstance(context: Context): SoundManager {
            return instance ?: synchronized(this) {
                instance ?: SoundManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

@Composable
fun rememberSoundManager(): SoundManager {
    val context = LocalContext.current
    val soundManager = remember { SoundManager.getInstance(context) }

    DisposableEffect(Unit) {
        onDispose {
            // Optional cleanup - could keep instance alive for app lifecycle
            soundManager.release()
        }
    }

    return soundManager
}