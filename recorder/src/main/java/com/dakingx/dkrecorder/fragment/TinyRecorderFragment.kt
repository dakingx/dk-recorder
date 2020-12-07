package com.dakingx.dkrecorder.fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dakingx.dkrecorder.R
import kotlinx.android.synthetic.main.fragment_tiny_recorder.*

private sealed class ViewState {
    object Recorder : ViewState()

    object Player : ViewState()
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
    }

    private var viewState: ViewState = ViewState.Recorder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_tiny_recorder, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        enterRecorder()
        // 录音
        recordIv.setOnClickListener {
            startRecord(false)

            recordIv.visibility = View.INVISIBLE
            stopIv.visibility = View.VISIBLE
        }
        // 播放
        playIv.setOnClickListener {
            startPlay()

            playIv.visibility = View.INVISIBLE
            stopIv.visibility = View.VISIBLE
        }
        // 删除
        deleteIv.setOnClickListener {
            stopPlay()
            enterRecorder()
        }
        // 停止
        stopIv.setOnClickListener {
            if (viewState == ViewState.Recorder) {
                stopRecord()
            } else {
                stopPlay()
                enterRecorder()
            }
        }
    }

    override fun onRecordFinished(uri: Uri) {
        if (viewState == ViewState.Recorder) {
            enterPlayer()
        }

        super.onRecordFinished(uri)
    }

    override fun onPlayFinished() {
        if (viewState == ViewState.Player) {
            playIv.visibility = View.VISIBLE
            stopIv.visibility = View.INVISIBLE
        }

        super.onPlayFinished()
    }

    @SuppressLint("SetTextI18n")
    private fun enterRecorder() {
        timeTv.text = "00:00"
        recordIv.visibility = View.VISIBLE
        deleteIv.visibility = View.INVISIBLE
        playIv.visibility = View.INVISIBLE
        stopIv.visibility = View.INVISIBLE

        viewState = ViewState.Recorder
    }

    @SuppressLint("SetTextI18n")
    private fun enterPlayer() {
        timeTv.text = "00:00"
        recordIv.visibility = View.INVISIBLE
        deleteIv.visibility = View.VISIBLE
        playIv.visibility = View.VISIBLE
        stopIv.visibility = View.INVISIBLE

        viewState = ViewState.Player
    }
}
