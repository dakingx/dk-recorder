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
    }

    private var fileProviderAuthority: String = ""
    private var maxDuration: Int = DEFAULT_DURATION

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null

    private var audioFile: File? = null
    private var audioUri: Uri? = null

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
    }

    override fun onDestroy() {
        recorderListener = null

        releaseRecorder()
        releasePlayer()

        super.onDestroy()
    }

    fun isRecording(): Boolean = audioFile != null

    fun canPlay(): Boolean = audioUri != null

    private fun createRecorder() {
        mediaRecorder = MediaRecorder().apply {
            // 麦克风
            setAudioSource(MediaRecorder.AudioSource.MIC)
            // 输出文件的格式
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            // 音频文件的编码
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            // 最大时长
            setMaxDuration(maxDuration * 1000)
            // 监听器
            setOnInfoListener { mr, what, extra ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stopRecord()
                }
            }
        }
    }

    private fun releaseRecorder() {
        mediaRecorder?.let {
            it.stop()
            it.reset()
            it.release()
        }
        mediaRecorder = null
    }

    private fun createPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener { onPlayFinished() }
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.let {
            it.stop()
            it.reset()
            it.release()
        }
        mediaPlayer = null
    }

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

        releaseRecorder()
        createRecorder()

        mediaRecorder?.let {
            it.setOutputFile(file.absolutePath)
            it.prepare()
            it.start()
        }

        return true
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun pauseRecord() {
        mediaRecorder?.pause()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun resumeRecord() {
        mediaRecorder?.resume()
    }

    fun stopRecord(): Boolean {
        val file = audioFile
        return if (file != null) {
            audioFile = null

            audioUri = requireContext().filePath2Uri(fileProviderAuthority, file.absolutePath)

            releaseRecorder()

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
            releasePlayer()
            createPlayer()

            mediaPlayer?.let {
                it.setDataSource(requireContext(), uri)
                it.prepare()
                it.start()
            }
            true
        } else {
            false
        }
    }

    fun pausePlay() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
    }

    fun resumePlay() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
            }
        }
    }

    fun stopPlay() {
        releasePlayer()
    }

    private fun checkRequiredPermissions(): Boolean =
        context?.checkAppPermission(*REQUIRED_PERMISSIONS.toTypedArray()) ?: false

    private fun toastError(@StringRes stringResId: Int) =
        Toast.makeText(requireContext(), getString(stringResId), Toast.LENGTH_SHORT).show()
}
