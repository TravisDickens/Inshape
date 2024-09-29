package com.travis.inshape

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class VideosActivity : AppCompatActivity() {
    private lateinit var youTubePlayerView: YouTubePlayerView
    private var youTubePlayer: YouTubePlayer? = null // Change to nullable
    private var pendingVideoId: String? = null // Store pending video ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_videos)

        // Initialize the YouTubePlayerView
        youTubePlayerView = findViewById(R.id.youtubePlayerView)
        lifecycle.addObserver(youTubePlayerView)

        // Initially hide the YouTubePlayerView
        youTubePlayerView.visibility = View.GONE

        // Set the YouTubePlayer listener only once
        youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                this@VideosActivity.youTubePlayer = youTubePlayer
                pendingVideoId?.let {
                    youTubePlayer.loadVideo(it, 0f) // Load the pending video if available
                    pendingVideoId = null // Clear pending video ID
                }
            }
        })

        // Push-Up Card Click Listener
        val pushUpCard = findViewById<MaterialCardView>(R.id.card_push_up)
        pushUpCard.setOnClickListener {
            youTubePlayerView.visibility = View.VISIBLE // Show YouTubePlayerView
            playYouTubeVideo("I9fsqKE5XHo") // YouTube video ID for Push-Up
        }

        // Squat Card Click Listener
        val squatCard = findViewById<MaterialCardView>(R.id.card_squat)
        squatCard.setOnClickListener {
            youTubePlayerView.visibility = View.VISIBLE // Show YouTubePlayerView
            playYouTubeVideo("U3HlEF_E9fo") // Valid YouTube video ID for Squat
        }

        // Plank Card Click Listener
        val plankCard = findViewById<MaterialCardView>(R.id.card_plank)
        plankCard.setOnClickListener {
            youTubePlayerView.visibility = View.VISIBLE // Show YouTubePlayerView
            playYouTubeVideo("e13yvYaOyqg") // Valid YouTube video ID for Plank
        }

        val chestCard = findViewById<MaterialCardView>(R.id.card_Chest)
        chestCard.setOnClickListener {
            youTubePlayerView.visibility = View.VISIBLE // Show YouTubePlayerView
            playYouTubeVideo("4Y2ZdHCOXok") // Valid YouTube video ID for Plank
        }

        val RDL = findViewById<MaterialCardView>(R.id.card_RDL)
        RDL.setOnClickListener {
            youTubePlayerView.visibility = View.VISIBLE // Show YouTubePlayerView
            playYouTubeVideo("_oyxCn2iSjU") // Valid YouTube video ID for Plank
        }

        val Rows = findViewById<MaterialCardView>(R.id.card_BarbellRow)
        Rows.setOnClickListener {
            youTubePlayerView.visibility = View.VISIBLE // Show YouTubePlayerView
            playYouTubeVideo("qXrTDQG1oUQ") // Valid YouTube video ID for Plank
        }
    }

    // Function to play YouTube video in YouTubePlayerView
    private fun playYouTubeVideo(videoId: String) {
        if (youTubePlayer != null) {
            youTubePlayer!!.loadVideo(videoId, 0f) // Use the already initialized YouTubePlayer
        } else {
            pendingVideoId = videoId // Store the video ID until the player is ready
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        youTubePlayerView.release() // Important to release resources
    }
}
