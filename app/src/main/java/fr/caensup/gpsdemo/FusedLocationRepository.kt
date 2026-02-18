package fr.caensup.gpsdemo

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine

class FusedLocationRepository(
    private val context: Context
) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getLastLocationOrNull(): Location? = suspendCancellableCoroutine { cont ->

        client.lastLocation
            .addOnSuccessListener { loc -> cont.resume(loc) {} }
            .addOnFailureListener { _ -> cont.resume(null) {} }
    }
}