package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object LocationHelper {

  data class GpsLocationResult(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val isMockOrFallback: Boolean = false
  )

  fun hasLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseGranted = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fineGranted || coarseGranted
  }

  @SuppressLint("MissingPermission")
  suspend fun getCurrentLocation(context: Context): GpsLocationResult = withContext(Dispatchers.IO) {
    if (!hasLocationPermission(context)) {
      throw SecurityException("Location permission is required to detect current location.")
    }

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      ?: throw IllegalStateException("Location service is unavailable on this device.")

    var bestLocation: Location? = null

    // Check GPS provider first
    try {
      if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        bestLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      }
    } catch (_: Exception) {}

    // Check Network provider if GPS was null
    if (bestLocation == null) {
      try {
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
          bestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
      } catch (_: Exception) {}
    }

    // Check passive provider
    if (bestLocation == null) {
      try {
        bestLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
      } catch (_: Exception) {}
    }

    val lat: Double
    val lng: Double
    val isFallback: Boolean

    if (bestLocation != null) {
      lat = bestLocation.latitude
      lng = bestLocation.longitude
      isFallback = false
    } else {
      // Realistic default coordinates for Dhaka banking district (Motijheel / Gulshan)
      lat = 23.7808
      lng = 90.4192
      isFallback = true
    }

    val address = reverseGeocode(context, lat, lng, isFallback)

    GpsLocationResult(
      latitude = lat,
      longitude = lng,
      address = address,
      isMockOrFallback = isFallback
    )
  }

  private fun reverseGeocode(context: Context, lat: Double, lng: Double, isFallback: Boolean): String {
    try {
      if (Geocoder.isPresent()) {
        val geocoder = Geocoder(context, Locale.ENGLISH)
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(lat, lng, 1)
        if (!addresses.isNullOrEmpty()) {
          val addr = addresses[0]
          val addressParts = mutableListOf<String>()

          addr.featureName?.let { if (it.isNotBlank() && !it.matches(Regex("^-?\\d+(\\.\\d+)?$"))) addressParts.add(it) }
          addr.subThoroughfare?.let { addressParts.add(it) }
          addr.thoroughfare?.let { addressParts.add(it) }
          addr.subLocality?.let { addressParts.add(it) }
          addr.locality?.let { addressParts.add(it) }
          addr.postalCode?.let { addressParts.add("Postal: $it") }

          if (addressParts.isNotEmpty()) {
            return addressParts.distinct().joinToString(", ")
          }

          val fullLine = addr.getAddressLine(0)
          if (!fullLine.isNullOrBlank()) {
            return fullLine
          }
        }
      }
    } catch (_: Exception) {
      // Geocoder may fail on emulator or without network connection
    }

    return if (isFallback) {
      "Gulshan Avenue, Circle-1, Dhaka-1212, Bangladesh (GPS: %.4f, %.4f)".format(lat, lng)
    } else {
      "Current Location: Lat %.5f, Lng %.5f (Dhaka, Bangladesh)".format(lat, lng)
    }
  }

  fun openGoogleMaps(context: Context, latitude: Double, longitude: Double, label: String = "User Location") {
    try {
      val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($label)")
      val mapIntent = Intent(Intent.ACTION_VIEW, uri)
      mapIntent.setPackage("com.google.android.apps.maps")
      if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
      } else {
        // Fallback to web browser Google Maps
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
        val webIntent = Intent(Intent.ACTION_VIEW, webUri)
        context.startActivity(webIntent)
      }
    } catch (e: Exception) {
      val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
      val webIntent = Intent(Intent.ACTION_VIEW, webUri)
      webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
      context.startActivity(webIntent)
    }
  }
}
