package com.example.exoplayer

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.Log
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.exoplayer.databinding.ActivityMainBinding



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var player: ExoPlayer? = null
    private val playbackStateListener: Player.Listener = playbackStateListener()

    private var playWhenReady = true
    private var currentItem = 0
    private var playbackPosition = 0L

    private var isFullscreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        val imageView = findViewById<ImageView>(R.id.imageview_fullscreen)

        imageView.setOnClickListener {
            if(isFullscreen){
                imageView.setImageResource(R.drawable.ic_baseline_fullscreen_24)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                isFullscreen = false
            }else{
                imageView.setImageResource(R.drawable.ic_baseline_fullscreen_exit_24)
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                isFullscreen = true
            }
        }
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
        supportActionBar?.hide()
        WindowInsetsControllerCompat(window, binding.videoView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.hide(WindowInsetsCompat.Type.captionBar())
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

            exoPlayer.removeListener(playbackStateListener)
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

                exoPlayer.addListener(playbackStateListener)
                exoPlayer.prepare()
            }
    }

}


private const val TAG = "PlayerActivity"

//エラーや再生状態の変更など、重要なプレーヤイベントを通知するために使用
private fun playbackStateListener() = object : Player.Listener {

    //再生状態が変化した時に呼び出される
    override fun onPlaybackStateChanged(playbackState: Int) {
        val stateString:String = when(playbackState){
            ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -" //playerのインスタンス化はされたが準備まだ
            ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -" //十分なデータがばっだされておらず再生できない
            ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -" //再生できる
            ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -" //終了した
            else -> "UNKNOWN_STATE             -"
        }
        Log.d(TAG, "changed state to $stateString")
    }
}
