package com.opentune.data

import android.content.Context
import androidx.core.content.edit
import com.opentune.BuildConfig
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * The personal edition's Secret: asked once on a phone, then remembered.
 *
 * Only a salted PBKDF2 hash of it is in the app, made at build time from the
 * OPENTUNE_PERSONAL_SECRET secret (app/build.gradle.kts), so the Secret
 * can't be read out of the public APK. A build without one (the standard
 * edition, or a local build) has no lock. Changing the Secret asks again.
 */
object AppLock {
    private const val PREFS = "lock"
    private const val K_UNLOCKED = "unlocked"
    private const val ITERATIONS = 200_000

    private val _locked = MutableStateFlow(false)
    val locked: StateFlow<Boolean> = _locked.asStateFlow()

    fun init(context: Context) {
        val hash = BuildConfig.SECRET_HASH
        _locked.value = hash.isNotEmpty() && prefs(context).getString(K_UNLOCKED, null) != hash
    }

    /** Whether [secret] is the Secret; unlocks for good when it is. */
    suspend fun unlock(context: Context, secret: String): Boolean {
        val hash = BuildConfig.SECRET_HASH
        if (hash.isEmpty()) return true
        val right = withContext(Dispatchers.Default) { matches(secret, BuildConfig.SECRET_SALT, hash) }
        if (right) {
            prefs(context).edit { putString(K_UNLOCKED, hash) }
            _locked.value = false
        }
        return right
    }

    /** PBKDF2-HMAC-SHA256 of [secret] with [salt] (hex), as hex; compared in constant time. */
    internal fun matches(secret: String, salt: String, hash: String): Boolean =
        MessageDigest.isEqual(derive(secret, salt).toByteArray(), hash.toByteArray())

    internal fun derive(secret: String, salt: String): String {
        val spec = PBEKeySpec(secret.trim().toCharArray(), salt.chunked(2).map { it.toInt(16).toByte() }.toByteArray(), ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
