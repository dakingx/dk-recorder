package com.dakingx.dkrecorder.fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dakingx.dkrecorder.R
import kotlinx.android.synthetic.main.fragment_tiny_recorder.*
import java.util.concurrent.atomic.AtomicInteger

private sealed class ViewState {
    object Recorder : ViewState()

    object Player : ViewState()

    object Destroy : ViewState()
}

class TinyRecorderFragment : RecorderFragment() {

    companion object {
        fun newInstance(fileProviderAuthority: String, maxDuration: Int = DEFAULT_DURATION) =
                TinyRecorderFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_FILE_PROVIDER_AUTH, fileProviderAuthority)
                        putInt(ARG_MAX_DURATION, maxDuration)
                    }
                }

        private const val MSG_DURATION = 0x123
    }

    private var viewState: ViewState = ViewState.Destroy
    private var duration = AtomicInteger(0)

    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            if (msg.what == MSG_DURATION) {
                val cur = duration.incrementAndGet()
                updateTimeTv()
                sendEmptyMessageDelayed(MSG_DURATION, 1000)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_tiny_recorder, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // 录音
        recordIv.setOnClickListener {
            startRecord(false)
            startTimer()

            recordIv.visibility = View.INVISIBLE
            stopIv.visibility = View.VISIBLE
        }
        // 播放
        playIv.setOnClickListener {
            startPlay()
            startTimer()

            playIv.visibility = View.INVISIBLE
            stopIv.visibility = View.VISIBLE
        }
        // 删除
        deleteIv.setOnClickListener {
            stopPlay()
            stopTimer()
            deleteRecord()
            enterRecorder()
        }
        // 停止
        stopIv.setOnClickListener {
            if (viewState == ViewState.Recorder) {
                stopRecord()
            } else if (viewState == ViewState.Player) {
                stopPlay()
                enterPlayer()
            }
            stopTimer()
        }

        viewState = ViewState.Recorder
        enterRecorder()
    }

    override fun onDestroyView() {
        if (viewState == ViewState.Recorder) {
            viewState = ViewState.Destroy
            stopRecord()
        } else if (viewState == ViewState.Player) {
            viewState = ViewState.Destroy
            stopPlay()
        }
        stopTimer()

        super.onDestroyView()
    }

    override fun onRecordFinished(uri: Uri) {
        if (viewState == ViewState.Recorder) {
            enterPlayer()
        }

        super.onRecordFinished(uri)
    }

    override fun onPlayFinished() {
        if (viewState == ViewState.Player) {
            enterPlayer()
        }

        super.onPlayFinished()
    }

    @SuppressLint("SetTextI18n")
    private fun enterRecorder() {
        stopTimer()

        recordIv.visibility = View.VISIBLE
        deleteIv.visibility = View.INVISIBLE
        playIv.visibility = View.INVISIBLE
        stopIv.visibility = View.INVISIBLE

        viewState = ViewState.Recorder
    }

    @SuppressLint("SetTextI18n")
    private fun enterPlayer() {
        stopTimer()

        recordIv.visibility = View.INVISIBLE
        deleteIv.visibility = View.VISIBLE
        playIv.visibility = View.VISIBLE
        stopIv.visibility = View.INVISIBLE

        viewState = ViewState.Player
    }

    @SuppressLint("SetTextI18n")
    private fun startTimer() {
        duration.set(0)
        updateTimeTv()
        handler.sendEmptyMessageDelayed(MSG_DURATION, 1000)
    }

    @SuppressLint("SetTextI18n")
    private fun stopTimer() {
        duration.set(0)
        updateTimeTv()
        handler.removeMessages(MSG_DURATION)
    }

    @SuppressLint("SetTextI18n")
    private fun updateTimeTv() {
        val cur = duration.get()
        val mins = cur / 60
        val secs = cur % 60
        timeTv.text = "${if (mins < 10) "0${mins}" else "$mins"}:${if (secs < 10) "0${secs}" else "$secs"}"
    }
}
