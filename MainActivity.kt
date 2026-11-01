package com.princetracker.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var status: TextView
    private lateinit var phone: EditText
    private lateinit var otp: EditText
    private var verificationId: String? = null
    private var map: GoogleMap? = null
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) startSharingService()
        else toast("Location permission is required to share your location.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 28, 24, 16); setBackgroundColor(0xFFF4F7FB.toInt()) }
        root.addView(TextView(this).apply { text = "PRINCE TRACKER"; textSize = 25f; setTextColor(0xFF14233D.toInt()); gravity = Gravity.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD })
        root.addView(TextView(this).apply { text = "Share live location only with permission"; textSize = 14f; gravity = Gravity.CENTER; setPadding(0, 6, 0, 18) })
        phone = EditText(this).apply { hint = "+91 mobile number"; inputType = 3 }
        otp = EditText(this).apply { hint = "OTP code"; inputType = 2 }
        root.addView(phone); root.addView(button("Send OTP") { sendOtp() }); root.addView(otp); root.addView(button("Verify OTP") { verifyOtp() })
        root.addView(button("Send location request") { sendRequest() })
        root.addView(button("Accept pending requests") { showIncomingRequests() })
        root.addView(button("Start sharing my live location") { requestLocationPermission() })
        root.addView(button("Stop sharing now") { stopSharing() })
        status = TextView(this).apply { text = "Not signed in"; setPadding(0, 14, 0, 8); setTextColor(0xFF14233D.toInt()) }; root.addView(status)
        val mapContainer = FrameLayout(this).apply { id = android.view.View.generateViewId() }
        root.addView(mapContainer, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        if (auth.currentUser != null) { status.text = "Signed in: ${auth.currentUser?.phoneNumber}"; listenForRequests() }
        val fragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction().replace(mapContainer.id, fragment).commit()
        fragment.getMapAsync(this)
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply { text = label; isAllCaps = false; setOnClickListener { action() } }
    private fun sendOtp() {
        val number = phone.text.toString().trim()
        if (!number.startsWith("+") || number.length < 10) { toast("Enter number with country code, e.g. +91..."); return }
        val options = PhoneAuthOptions.newBuilder(auth).setPhoneNumber(number).setTimeout(60, TimeUnit.SECONDS).setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) { auth.signInWithCredential(credential).addOnSuccessListener { signedIn() } }
                override fun onVerificationFailed(e: Exception) { toast(e.localizedMessage ?: "OTP failed") }
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) { verificationId = id; toast("OTP sent") }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    private fun verifyOtp() {
        val id = verificationId ?: run { toast("First send OTP"); return }
        val credential = PhoneAuthProvider.getCredential(id, otp.text.toString().trim())
        auth.signInWithCredential(credential).addOnSuccessListener { signedIn() }.addOnFailureListener { toast(it.localizedMessage ?: "Invalid OTP") }
    }
    private fun signedIn() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).set(mapOf("phone" to user.phoneNumber, "uid" to user.uid, "updatedAt" to FieldValue.serverTimestamp()))
        status.text = "Signed in: ${user.phoneNumber}"; listenForRequests(); toast("Login successful")
    }
    private fun sendRequest() {
        val me = auth.currentUser ?: run { toast("Login first"); return }
        val target = phone.text.toString().trim()
        if (target.isBlank() || target == me.phoneNumber) { toast("Enter another person's mobile number"); return }
        db.collection("users").whereEqualTo("phone", target).limit(1).get().addOnSuccessListener { result ->
            val recipient = result.documents.firstOrNull()
            if (recipient == null) { toast("That person must sign in to Prince Tracker first"); return@addOnSuccessListener }
            db.collection("requests").add(mapOf("fromUid" to me.uid, "fromPhone" to me.phoneNumber, "toUid" to recipient.id, "status" to "pending", "createdAt" to FieldValue.serverTimestamp()))
                .addOnSuccessListener { toast("Request sent. They must accept it.") }.addOnFailureListener { toast(it.localizedMessage ?: "Request failed") }
        }.addOnFailureListener { toast(it.localizedMessage ?: "Search failed") }
    }
    private fun listenForRequests() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("requests").whereEqualTo("toUid", uid).whereEqualTo("status", "pending").addSnapshotListener { snap, error ->
            if (error != null) return@addSnapshotListener
            if (snap != null && !snap.isEmpty) status.text = "You have ${snap.size()} pending location request(s). Tap Accept pending requests."
        }
    }
    private fun showIncomingRequests() {
        val uid = auth.currentUser?.uid ?: run { toast("Login first"); return }
        db.collection("requests").whereEqualTo("toUid", uid).whereEqualTo("status", "pending").get().addOnSuccessListener { snap ->
            if (snap.isEmpty) { toast("No pending requests"); return@addOnSuccessListener }
            val items = snap.documents.map { "${it.getString("fromPhone") ?: "Unknown"} — request location?" }.toTypedArray()
            AlertDialog.Builder(this).setTitle("Location requests").setItems(items) { _, index ->
                val doc = snap.documents[index]
                AlertDialog.Builder(this).setTitle("Share your live location?").setMessage("Only accept if you know and trust this person. You can stop sharing anytime.")
                    .setPositiveButton("Accept") { _, _ -> doc.reference.update("status", "accepted", "acceptedAt", FieldValue.serverTimestamp()).addOnSuccessListener { toast("Accepted. Now tap Start sharing.") } }
                    .setNegativeButton("Reject") { _, _ -> doc.reference.update("status", "rejected").addOnSuccessListener { toast("Request rejected") } }.show()
            }.setNegativeButton("Cancel", null).show()
        }
    }
    private fun requestLocationPermission() {
        val needed = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        permissions.launch(needed.toTypedArray())
    }
    private fun startSharingService() {
        val uid = auth.currentUser?.uid ?: run { toast("Login first"); return }
        db.collection("requests").whereEqualTo("toUid", uid).whereEqualTo("status", "accepted").get().addOnSuccessListener { requests ->
            if (requests.isEmpty) { toast("No accepted request. Ask the other person to request and accept first."); return@addOnSuccessListener }
            val intent = Intent(this, LocationShareService::class.java)
            ContextCompat.startForegroundService(this, intent)
            requests.documents.forEach { it.reference.update("sharing", true) }
            toast("Live location sharing started")
        }
    }
    private fun stopSharing() {
        stopService(Intent(this, LocationShareService::class.java))
        val uid = auth.currentUser?.uid ?: return
        db.collection("locations").document(uid).delete()
        db.collection("requests").whereEqualTo("toUid", uid).whereEqualTo("status", "accepted").get().addOnSuccessListener { s -> s.documents.forEach { it.reference.update("sharing", false, "status", "stopped") } }
        toast("Sharing stopped and current location deleted")
    }
    override fun onMapReady(googleMap: GoogleMap) { map = googleMap; googleMap.uiSettings.isZoomControlsEnabled = true }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
