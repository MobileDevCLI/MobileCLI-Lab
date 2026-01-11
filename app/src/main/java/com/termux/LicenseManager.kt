package com.termux

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * LicenseManager - Handles license verification and subscription status
 *
 * This checks with the MobileCLI backend to verify:
 * 1. Device is registered
 * 2. User has active subscription (or trial)
 * 3. License hasn't expired
 */
class LicenseManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "mobilecli_license"
        private const val KEY_LICENSE_KEY = "license_key"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USER_TIER = "user_tier"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_LAST_VERIFIED = "last_verified"
        private const val KEY_OFFLINE_GRACE_DAYS = 7

        // API endpoint - update this with your actual Supabase URL
        private const val API_BASE = "https://mwxlguqukyfberyhtkmg.supabase.co/rest/v1"
        private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im13eGxndXF1a3lmYmVyeWh0a21nIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njc0OTg5ODgsImV4cCI6MjA4MzA3NDk4OH0.VdpU9WzYpTyLeVX9RaXKBP3dNNNf0t9YkQfVf7x_TA8"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Get unique device ID
     */
    fun getDeviceId(): String {
        val stored = prefs.getString(KEY_DEVICE_ID, null)
        if (stored != null) {
            return stored
        }

        // Generate a unique device ID
        val deviceId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: UUID.randomUUID().toString()
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        return deviceId
    }

    /**
     * Get stored license key
     */
    fun getLicenseKey(): String? {
        return prefs.getString(KEY_LICENSE_KEY, null)
    }

    /**
     * Get user tier (free, pro, team)
     */
    fun getUserTier(): String {
        return prefs.getString(KEY_USER_TIER, "free") ?: "free"
    }

    /**
     * Check if license is valid (can work offline with grace period)
     */
    fun isLicenseValid(): Boolean {
        val licenseKey = getLicenseKey() ?: return true // No license = free tier (allowed)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        val lastVerified = prefs.getLong(KEY_LAST_VERIFIED, 0)

        // If never verified, need to verify online
        if (lastVerified == 0L) return false

        // Check if within offline grace period
        val daysSinceVerified = (System.currentTimeMillis() - lastVerified) / (1000 * 60 * 60 * 24)
        if (daysSinceVerified > KEY_OFFLINE_GRACE_DAYS) {
            // Need to verify online
            return false
        }

        // Check expiration
        if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
            return false
        }

        return true
    }

    /**
     * Check if user has Pro access
     */
    fun hasProAccess(): Boolean {
        val tier = getUserTier()
        return tier == "pro" || tier == "team"
    }

    /**
     * Verify license with server
     */
    suspend fun verifyLicense(): LicenseResult = withContext(Dispatchers.IO) {
        val licenseKey = getLicenseKey()

        // No license key = free tier, always valid
        if (licenseKey == null) {
            return@withContext LicenseResult(
                valid = true,
                tier = "free",
                message = "Free tier - upgrade for Pro features"
            )
        }

        try {
            val deviceId = getDeviceId()
            val url = URL("$API_BASE/rpc/verify_license")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("apikey", SUPABASE_ANON_KEY)
            connection.setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            connection.doOutput = true

            val body = JSONObject().apply {
                put("p_license_key", licenseKey)
                put("p_device_id", deviceId)
            }

            connection.outputStream.use { os ->
                os.write(body.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                val valid = json.optBoolean("valid", false)
                val tier = json.optString("tier", "free")
                val expiresAt = json.optString("expires_at", "")

                if (valid) {
                    // Store verification result
                    prefs.edit().apply {
                        putString(KEY_USER_TIER, tier)
                        putLong(KEY_LAST_VERIFIED, System.currentTimeMillis())
                        if (expiresAt.isNotEmpty()) {
                            // Parse ISO date and store as millis
                            try {
                                val expMillis = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                                    .parse(expiresAt)?.time ?: 0
                                putLong(KEY_EXPIRES_AT, expMillis)
                            } catch (e: Exception) {
                                // Ignore parse errors
                            }
                        }
                        apply()
                    }

                    return@withContext LicenseResult(
                        valid = true,
                        tier = tier,
                        message = "License verified"
                    )
                } else {
                    val error = json.optString("error", "Invalid license")
                    return@withContext LicenseResult(
                        valid = false,
                        tier = "free",
                        message = error
                    )
                }
            } else {
                // Server error - use cached status if available
                if (isLicenseValid()) {
                    return@withContext LicenseResult(
                        valid = true,
                        tier = getUserTier(),
                        message = "Offline mode - using cached license"
                    )
                }
                return@withContext LicenseResult(
                    valid = false,
                    tier = "free",
                    message = "Could not verify license"
                )
            }
        } catch (e: Exception) {
            // Network error - use cached status if available
            if (isLicenseValid()) {
                return@withContext LicenseResult(
                    valid = true,
                    tier = getUserTier(),
                    message = "Offline mode - using cached license"
                )
            }
            return@withContext LicenseResult(
                valid = false,
                tier = "free",
                message = "Network error: ${e.message}"
            )
        }
    }

    /**
     * Register device with license key
     */
    suspend fun registerDevice(licenseKey: String): LicenseResult = withContext(Dispatchers.IO) {
        try {
            val deviceId = getDeviceId()
            val deviceName = android.os.Build.MODEL

            // Store license key
            prefs.edit().putString(KEY_LICENSE_KEY, licenseKey).apply()

            // Verify it
            return@withContext verifyLicense()
        } catch (e: Exception) {
            return@withContext LicenseResult(
                valid = false,
                tier = "free",
                message = "Registration failed: ${e.message}"
            )
        }
    }

    /**
     * Clear license (logout)
     */
    fun clearLicense() {
        prefs.edit().clear().apply()
    }

    /**
     * License verification result
     */
    data class LicenseResult(
        val valid: Boolean,
        val tier: String,
        val message: String
    )
}
