package com.princetracker.app

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LocationShareService : Service() {
    private lateinit var client: FusedLocationProviderClient
    private lateinit var callback: LocationCallback
    private val channelId = "prince_tracker_location"
    override fun onCreate() {
        super.onCreate()
        createChannel()
        val notification = NotificationCompat.Builder(this, channelId).setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Prince Tracker is sharing location")
            .setContentText("Your live location is being shared after your approval. Tap Stop in the app to end sharing.")
            .setOngoing(true).build()
        startForeground(71, notification)
        client = LocationServices.getFusedLocationProviderClient(this)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run { stopSelf(); return }
        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                FirebaseFirestore.getInstance().collection("locations").document(uid).set(mapOf("lat" to loc.latitude, "lng" to loc.longitude, "updatedAt" to System.currentTimeMillis(), "sharing" to true))
            }
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) { stopSelf(); return }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).setMinUpdateIntervalMillis(3000L).build()
        client.requestLocationUpdates(request, callback, mainLooper)
    }
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "Live location sharing", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY
    override fun onDestroy() {
        if (::callback.isInitialized && ::client.isInitialized) client.removeLocationUpdates(callback)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) FirebaseFirestore.getInstance().collection("locations").document(uid).delete()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
