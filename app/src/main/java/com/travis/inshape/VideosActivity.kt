package com.travis.inshape

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.material.card.MaterialCardView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class VideosActivity : Base() {
    private lateinit var youTubePlayerView: YouTubePlayerView
    private var youTubePlayer: YouTubePlayer? = null
    private var pendingVideoId: String? = null

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
                    // Load the pending video if available
                    youTubePlayer.loadVideo(it, 0f)
                    // Clear pending video ID
                    pendingVideoId = null
                }
            }
        })

        // Push-Up Card Click Listener
        val pushUpCard = findViewById<MaterialCardView>(R.id.card_push_up)
        pushUpCard.setOnClickListener {
            // Show YouTubePlayerView
            youTubePlayerView.visibility = View.VISIBLE
            // YouTube video ID for Push-Up
            playYouTubeVideo("I9fsqKE5XHo")
        }

        // Squat Card Click Listener
        val squatCard = findViewById<MaterialCardView>(R.id.card_squat)
        squatCard.setOnClickListener {
            // Show YouTubePlayerView
            youTubePlayerView.visibility = View.VISIBLE
            // Valid YouTube video ID for Squat
            playYouTubeVideo("U3HlEF_E9fo")
        }

        // Plank Card Click Listener
        val plankCard = findViewById<MaterialCardView>(R.id.card_plank)
        plankCard.setOnClickListener {
            // Show YouTubePlayerView
            youTubePlayerView.visibility = View.VISIBLE
            // Valid YouTube video ID for Plank
            playYouTubeVideo("e13yvYaOyqg")
        }

        val chestCard = findViewById<MaterialCardView>(R.id.card_Chest)
        chestCard.setOnClickListener {
            // Show YouTubePlayerView
            youTubePlayerView.visibility = View.VISIBLE
            // Valid YouTube video ID for Chest
            playYouTubeVideo("4Y2ZdHCOXok")
        }

        val RDL = findViewById<MaterialCardView>(R.id.card_RDL)
        RDL.setOnClickListener {
            // Show YouTubePlayerView
            youTubePlayerView.visibility = View.VISIBLE
            // Valid YouTube video ID for RDL
            playYouTubeVideo("_oyxCn2iSjU")
        }

        val Rows = findViewById<MaterialCardView>(R.id.card_BarbellRow)
        Rows.setOnClickListener {
            // Show YouTubePlayerView
            youTubePlayerView.visibility = View.VISIBLE
            // Valid YouTube video ID for Barbell Row
            playYouTubeVideo("qXrTDQG1oUQ")
        }
    }

    // Function to play YouTube video in YouTubePlayerView
    private fun playYouTubeVideo(videoId: String) {
        try {
            if (youTubePlayer != null) {
                // Use the already initialized YouTubePlayer
                youTubePlayer!!.loadVideo(videoId, 0f)
            } else {
                // Store the video ID until the player is ready
                pendingVideoId = videoId
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Important to release resources
        youTubePlayerView.release()
    }
}
