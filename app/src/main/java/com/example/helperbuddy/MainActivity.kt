package com.example.helperbuddy.Activity

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.helperbuddy.Fragment.ProfileFragment
import com.example.helperbuddy.Fragment.MessagesFragment
import com.example.helperbuddy.R
import com.example.helperbuddy.Fragment.RequestFragment
import com.example.roadbuddy.Fragment.HomeFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {
    private lateinit var fragmentManager: FragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)




        val homeBtn = findViewById<ImageView>(R.id.home)
        val shopBtn = findViewById<ImageView>(R.id.shop)
        val serviceBtn = findViewById<ImageView>(R.id.service)
        val profileBtn = findViewById<ImageView>(R.id.profile)

        homeBtn.setColorFilter(getColor(R.color.red))

        goToFragment(HomeFragment())


        homeBtn.setOnClickListener{
            goToFragment(HomeFragment())
            homeBtn.setColorFilter(getColor(R.color.red))
            serviceBtn.setColorFilter(getColor(R.color.black))
            shopBtn.setColorFilter(getColor(R.color.black))
            profileBtn.setColorFilter(getColor(R.color.black))
        }
        serviceBtn.setOnClickListener{
            goToFragment(MessagesFragment())
            homeBtn.setColorFilter(getColor(R.color.black))
            serviceBtn.setColorFilter(getColor(R.color.red))
            shopBtn.setColorFilter(getColor(R.color.black))
            profileBtn.setColorFilter(getColor(R.color.black))
        }
        shopBtn.setOnClickListener{
            goToFragment(RequestFragment())
            homeBtn.setColorFilter(getColor(R.color.black))
            serviceBtn.setColorFilter(getColor(R.color.black))
            shopBtn.setColorFilter(getColor(R.color.red))
            profileBtn.setColorFilter(getColor(R.color.black))
        }
        profileBtn.setOnClickListener{
            goToFragment(ProfileFragment())
            homeBtn.setColorFilter(getColor(R.color.black))
            serviceBtn.setColorFilter(getColor(R.color.black))
            shopBtn.setColorFilter(getColor(R.color.black))
            profileBtn.setColorFilter(getColor(R.color.red))
        }
    }

    private  fun goToFragment(fragment: Fragment){
        fragmentManager = supportFragmentManager
        fragmentManager.beginTransaction().replace(R.id.fragmentContainer,fragment).commit()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
