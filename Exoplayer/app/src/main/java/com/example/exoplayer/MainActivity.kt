package com.example.exoplayer

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.exoplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var player: ExoPlayer? = null

    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    //APIレベル24以上のAndroid は、複数のウィンドウをサポートする。アプリは視認可能だが、分割ウィンドウモードではアクティブにならないため、onStartでプレーヤーを初期化する
    override fun onStart() {
        super.onStart()

        if(Util.SDK_INT > 23){
            initializePlayer()
        }
    }

    //APIレベル24以下のAndroid では、アプリがリソースを取得するまでできるだけ長く待機する必要があるため、プレーヤーを初期化する前に onResume まで待つ。
    override fun onResume() {
        super.onResume()

        hideSystemUi()
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer()
        }
    }

    //全画面表示するために他のUIを非表示する
    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.videoView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    //APIレベル 24 以下では、onStop が呼び出される保証がないため、onPause でできるだけ早くプレーヤーを解放する
    override fun onPause() {
        super.onPause()

        if(Util.SDK_INT <= 23){
            releasePlayer()
        }
    }

    //APIレベル24以上（マルチウィンドウモードと分割ウィンドウモードが導入されている）では、onStopが呼び出されることが保証される。
    // 一時停止状態では、アクティビティが引き続き表示されるため、onStopまで待ってからプレーヤーを解放する
    override fun onStop() {
        super.onStop()

        if(Util.SDK_INT > 23){
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        //playerを破棄する前に情報を保存しておき、中断したところから再開できるようにする
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.contentPosition
            currentItem = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
        player = null
    }

    private fun initializePlayer(){
        /*
        Adaptive Streaming
        変化するネットワークに対応して、動画の再生品質を変え、ユーザーが快適に再生を続けることができるようにするストリーミング。
        複数のビットレートのフラグメント（ビデオとオーディオそれぞれ数秒に区切ったもの）を用意して、それをクライアントが状況に応じて要求する
         */
        val trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()
            .also { exoPlayer ->
                binding.videoView.player = exoPlayer
                //DASHというアダプティブ ストリーミング フォーマットを使用する。MediaItemはBuilderでMIMEタイプを指定して作成する。
//                val mediaItem = MediaItem.fromUri(getString(R.string.media_url_mp4))
                val mediaItem = MediaItem.Builder()
                    .setUri(getString(R.string.media_url_dash))
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build()

                exoPlayer.setMediaItem(mediaItem)
                //playerにadd,move, removeなどの操作でプレイリストを作成できる
//                val secondMediaItem = MediaItem.fromUri(getString(R.string.media_url_mp3))
//                exoPlayer.addMediaItem(secondMediaItem)
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.seekTo(currentItem, playbackPosition)
                exoPlayer.prepare()
            }
    }
}