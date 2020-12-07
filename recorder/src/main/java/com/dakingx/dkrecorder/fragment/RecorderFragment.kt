package com.dakingx.dkrecorder.fragment

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.text.format.DateFormat
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import com.dakingx.dkrecorder.R
import com.dakingx.dkrecorder.ext.checkAppPermission
import com.dakingx.dkrecorder.ext.filePath2Uri
import com.dakingx.dkrecorder.ext.generateTempFile
import java.io.File
import java.util.*

interface RecorderListener {
    fun onRecordFinished(uri: Uri)

    fun onPlayFinished()
}

open class RecorderFragment : BaseFragment() {

    companion object {
        val REQUIRED_PERMISSIONS = listOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE
        )

        fun newInstance(fileProviderAuthority: String, maxDuration: Int = DEFAULT_DURATION) =
                RecorderFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_FILE_PROVIDER_AUTH, fileProviderAuthority)
                        putInt(ARG_MAX_DURATION, maxDuration)
                    }
                }

        const val ARG_FILE_PROVIDER_AUTH = "arg_file_provider_auth"
        const val ARG_MAX_DURATION = "arg_max_duration"

        const val DEFAULT_DURATION = 30

        private const val MSG_STOP_RECORDER = 0x123
    }

    private var fileProviderAuthority: String = ""
    private var maxDuration: Int = DEFAULT_DURATION

    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var mediaPlayer: MediaPlayer

    private var audioFile: File? = null
    private var audioUri: Uri? = null

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == MSG_STOP_RECORDER) {
                stopRecord()
            }
        }
    }

    var recorderListener: RecorderListener? = null

    protected open fun onRecordFinished(uri: Uri) {
        recorderListener?.onRecordFinished(uri)
    }

    protected open fun onPlayFinished() {
        recorderListener?.onPlayFinished()
    }

    override fun restoreState(bundle: Bundle?) {
        bundle?.apply {
            getString(ARG_FILE_PROVIDER_AUTH)?.let {
                fileProviderAuthority = it
            }
            maxDuration = getInt(ARG_MAX_DURATION, DEFAULT_DURATION)
        }
    }

    override fun storeState(bundle: Bundle) {
        bundle.also {
            it.putString(ARG_FILE_PROVIDER_AUTH, fileProviderAuthority)
            it.putInt(ARG_MAX_DURATION, maxDuration)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (recorderListener == null) {
            recorderListener = context as? RecorderListener
        }

        mediaRecorder = MediaRecorder().apply {
            // 麦克风
            setAudioSource(MediaRecorder.AudioSource.MIC)
            // 音频文件的编码
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            // 输出文件的格式
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        }

        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener { onPlayFinished() }
        }
    }

    override fun onDestroy() {
        recorderListener = null

        mediaRecorder.stop()
        mediaRecorder.release()

        mediaPlayer.stop()
        mediaPlayer.release()

        handler.removeMessages(MSG_STOP_RECORDER)

        super.onDestroy()
    }

    fun isRecording(): Boolean = audioFile != null

    fun canPlay(): Boolean = audioUri != null

    fun startRecord(errorTip: Boolean = true): Boolean {
        if (!checkRequiredPermissions()) {
            if (errorTip) {
                toastError(R.string.tip_lack_required_permissions)
            }
            return false
        }

        val file = requireContext().generateTempFile(
                "audio_${
                    DateFormat.format(
                            "yyyyMMdd_HHmmss",
                            Calendar.getInstance()
                    )
                }", "m4a"
        )
        if (file == null) {
            if (errorTip) {
                toastError(R.string.tip_gen_tmp_file_fail)
            }
            return false
        }
        audioFile = file
        audioUri = null

        mediaRecorder.setOutputFile(file.absolutePath)
        mediaRecorder.prepare()
        mediaRecorder.start()

        handler.sendEmptyMessageDelayed(MSG_STOP_RECORDER, maxDuration * 1000L)
        return true
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun pauseRecord() {
        mediaRecorder.pause()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun resumeRecord() {
        mediaRecorder.resume()
    }

    fun stopRecord(): Boolean {
        val file = audioFile
        return if (file != null) {
            audioFile = null

            handler.removeMessages(MSG_STOP_RECORDER)

            audioUri = requireContext().filePath2Uri(fileProviderAuthority, file.absolutePath)

            mediaRecorder.stop()
//            mediaRecorder.release()

            audioUri?.let {
                onRecordFinished(it)
            }

            true
        } else {
            false
        }
    }

    fun startPlay(): Boolean {
        val uri = audioUri
        return if (uri != null) {
            mediaPlayer.setDataSource(requireContext(), uri)
            mediaPlayer.prepare()
            mediaPlayer.start()
            true
        } else {
            false
        }
    }

    fun pausePlay() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    fun resumePlay() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    fun stopPlay() {
        mediaPlayer.stop()
//        mediaPlayer.release()
    }

    private fun checkRequiredPermissions(): Boolean =
            context?.checkAppPermission(*REQUIRED_PERMISSIONS.toTypedArray()) ?: false

    private fun toastError(@StringRes stringResId: Int) =
            Toast.makeText(requireContext(), getString(stringResId), Toast.LENGTH_SHORT).show()
}
