package com.uz.sovchi.ui.record

import android.media.AudioFormat
import android.os.Bundle
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.squti.androidwaverecorder.RecorderState
import com.github.squti.androidwaverecorder.WaveRecorder
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.uz.sovchi.PermissionController
import com.uz.sovchi.R
import com.uz.sovchi.data.nomzod.KELIN
import com.uz.sovchi.databinding.AudioListenSheetBinding
import com.uz.sovchi.databinding.FragmentVoiceRecordBinding
import com.uz.sovchi.ui.base.BaseFragment

class VoiceRecordFragment : BaseFragment<FragmentVoiceRecordBinding>() {

    override val layId: Int
        get() = R.layout.fragment_voice_record

    private var type = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        type = arguments?.getInt("type") ?: 0
    }

    companion object {
        val kelinQuestion = arrayOf(
            Question("Oilangiz haqida qisqacha gapirib bering. Nechta farzand, siz nechinchisiz? Ota onangiz bormi"),
            Question("Qayerda yashaysiz? Qayerda tugilgansiz?(Viloyat, shaxar nomi)"),
            Question("Oila qurmaganligiz / Ajrashganligiz sababi?"),
            Question("Nima ish qilasiz? (Kasbingiz, quruvchimsiz, sartaroshmi yoki boshqa)"),
            Question("Harakteringiz qanday? (Ogir bosiqmisiz? Shoxmisiz?)"),
            Question("Nimalarga qiziqasiz (Hobbingiz nima, baliq tutishmi, futbol o'ynashmi, ovqat qilishmi)?"),
            Question("Farzandlaringiz bormi? Bo'lsa malumot bering (Nechta, kimda )"),
            Question("Oliy malumotlimisiz? Qaysi o'qishda oqigansiz yoki bitirgansiz"),
            Question("Ijimoyiv tarmoqlarda aktivmisiz?"),
            Question("Ovqat qilishni bilasizmi?"),
            Question("Turmush o'rtogingiz qanday inson bo'lsin?"),
            Question("Turmush o'rtogingiz qancha pul topishi kerak deb oylaysiz?"),

        )
        val kuyovQuestion = arrayOf(Question("Oilangiz haqida qisqacha gapirib bering. Nechta farzand, siz nechinchisiz? Ota onangiz bormi"),
            Question("Qayerda yashaysiz? Qayerda tugilgansiz?(Viloyat, shaxar nomi)"),
            Question("Oila qurmaganligiz / Ajrashganligiz sababi?"),
            Question("Nima ish qilasiz? (Kasbingiz, quruvchimsiz, sartaroshmi yoki boshqa)"),
            Question("Qancha pul topasiz? Oila boqish uchun yetadi deb oylaysizmi?"),
            Question("Harakteringiz qanday? (Ogir bosiqmisiz? Shoxmisiz?)"),
            Question("Nimalarga qiziqasiz (Hobbingiz nima, baliq tutishmi, futbol o'ynashmi, ovqat qilishmi)?"),
            Question("Farzandlaringiz bormi? Bo'lsa malumot bering (Nechta, kimda )"),
            Question("Oliy malumotlimisiz? Qaysi o'qishda oqigansiz yoki bitirgansiz"),
            Question("Turmush o'rtogingiz qanday inson bo'lsin?"))
    }

    private var isRecording = false
    private val direction: String by lazy {
        context?.externalCacheDir?.absolutePath + "/audioFile.wav"
    }

    private val recorder: WaveRecorder by lazy {
        WaveRecorder(direction).apply {
            waveConfig.sampleRate = 48000
            waveConfig.channels = AudioFormat.CHANNEL_IN_STEREO
            waveConfig.audioEncoding = AudioFormat.ENCODING_PCM_8BIT
            onStateChangeListener = {
                when (it) {
                    RecorderState.RECORDING -> {

                    }

                    RecorderState.STOP -> {
                        audioUri = direction
                    }

                    RecorderState.PAUSE -> {

                    }
                }
            }
        }
    }

    private fun startRecording() {
        PermissionController.getInstance().requestPermissions(requireContext(),
            0,
            arrayOf(android.Manifest.permission.RECORD_AUDIO),
            object : PermissionController.PermissionResult {
                override fun onDenied() {
                    closeFragment()
                }

                override fun onGranted() {
                    binding?.apply {
                        isRecording = true
                        iconView.setImageResource(R.drawable.pause_ic)
                        stateTextView.text = getString(R.string.tugatish)
                        recorder.startRecording()
                    }
                }
            })
    }

    private fun finishRecording() {
        binding?.apply {
            isRecording = false
            iconView.setImageResource(R.drawable.mic_ic)
            stateTextView.text = getString(R.string.boshlash)
            recorder.stopRecording()
            showPlayBottomSheet()
        }
    }

    private fun toggleRecording() {
        if (isRecording) {
            finishRecording()
        } else {
            startRecording()
        }
    }

    private fun initQuestions() {
        val adapter = QuestionAdapter()
        adapter.submitList((if (type == KELIN) kelinQuestion else kuyovQuestion).toMutableList())
        binding?.recyclerView?.apply {
            this.adapter = adapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    override fun viewCreated(bind: FragmentVoiceRecordBinding) {
        bind.apply {
            toolbar.setUpBackButton(this@VoiceRecordFragment)
            iconView.setOnClickListener {
                toggleRecording()
            }
            stateTextView.setOnClickListener {
                toggleRecording()
            }
            initQuestions()
        }
    }

    private var audioUri: String? = null

    private fun onRerecord() {
        audioUri = null
    }

    private fun showPlayBottomSheet() {
        if (audioUri.isNullOrEmpty()) return
        val binding = AudioListenSheetBinding.inflate(layoutInflater, null, false)
        val bottomSheet = BottomSheetDialog(requireContext())
        bottomSheet.setContentView(binding.root)
        bottomSheet.setCancelable(false)
        val exoPlayer = ExoPlayer.Builder(requireContext()).build()
        exoPlayer.setMediaItem(MediaItem.fromUri(audioUri!!))
        exoPlayer.prepare()
        bottomSheet.setOnDismissListener {
            exoPlayer.release()
        }
        binding.apply {
            retryButton.setOnClickListener {
                onRerecord()
                bottomSheet.dismiss()
            }
            doneButton.setOnClickListener {
                bottomSheet.dismiss()
                setFragmentResult("audio", Bundle().apply {
                    putString("url", audioUri)
                })
                closeFragment()
            }
            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        exoPlayer.seekTo(0)
                        exoPlayer.pause()
                        playView.setImageResource(R.drawable.play_ic)
                    }
                }
            })
            playView.setOnClickListener {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    playView.setImageResource(R.drawable.play_ic)
                } else {
                    exoPlayer.play()
                    playView.setImageResource(R.drawable.pause_ic)
                }
            }
        }
        bottomSheet.show()
    }
}