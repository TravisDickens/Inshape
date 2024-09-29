package com.travis.inshape

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Date
import java.util.Locale

class MidnightResetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

            val activity = context as? HomeActivity
            activity?.resetSteps() // Call resetSteps method to reset steps at midnight
        }
    }