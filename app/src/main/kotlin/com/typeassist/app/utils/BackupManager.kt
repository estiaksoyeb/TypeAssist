package com.typeassist.app.utils

import android.content.Context
import com.google.gson.GsonBuilder
import com.typeassist.app.data.AppConfig
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupManager {

    private const val MAGIC_HEADER = "TABAK"
    private const val VERSION: Byte = 1
    private const val FLAG_PLAIN: Byte = 0
    private const val FLAG_ENCRYPTED: Byte = 1
    
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128 // bits

    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun generateFileName(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        return "TypeAssist_${sdf.format(Date())}.tabak"
    }

    /**
     * exports the config to a byte array in the .tabak format
     */
    fun exportBackup(config: AppConfig, password: String?): ByteArray {
        val json = gson.toJson(config)
        val jsonBytes = json.toByteArray(StandardCharsets.UTF_8)
        
        // 1. Compress
        val compressed = compress(jsonBytes)
        
        val output = ByteArrayOutputStream()
        // Header
        output.write(MAGIC_HEADER.toByteArray(StandardCharsets.UTF_8))
        output.write(VERSION.toInt())

        if (!password.isNullOrBlank()) {
            // Encrypted Path
            output.write(FLAG_ENCRYPTED.toInt())
            
            // Generate Salt & Key
            val salt = ByteArray(SALT_SIZE)
            SecureRandom().nextBytes(salt)
            val key = deriveKey(password, salt)
            
            // Generate IV
            val iv = ByteArray(IV_SIZE)
            SecureRandom().nextBytes(iv)
            
            // Encrypt
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, spec)
            val cipherText = cipher.doFinal(compressed)
            
            // Write Salt, IV, CipherText
            output.write(salt)
            output.write(iv)
            output.write(cipherText)
            
        } else {
            // Plain Path
            output.write(FLAG_PLAIN.toInt())
            // Just write the compressed data
            output.write(compressed)
        }
        
        return output.toByteArray()
    }

    /**
     * Imports the backup. Throws Exception if invalid or password incorrect.
     * Returns a pair of (AppConfig?, Boolean). Boolean is true if password is required but not provided.
     */
    fun importBackup(input: InputStream, password: String?): Pair<AppConfig?, Boolean> {
        val bytes = input.readBytes()
        if (bytes.size < 5) throw IllegalArgumentException("Invalid file format")
        
        val header = String(bytes.copyOfRange(0, 5), StandardCharsets.UTF_8)
        if (header != MAGIC_HEADER) throw IllegalArgumentException("Not a TypeAssist backup file")
        
        val version = bytes[5] // currently unused, for future compat
        val flag = bytes[6]
        
        var payloadOffset = 7
        
        val plainBytes: ByteArray
        
        if (flag == FLAG_ENCRYPTED) {
            if (password.isNullOrBlank()) {
                return Pair(null, true) // Signal that password is required
            }
            
            // Read Salt
            val salt = bytes.copyOfRange(payloadOffset, payloadOffset + SALT_SIZE)
            payloadOffset += SALT_SIZE
            
            // Read IV
            val iv = bytes.copyOfRange(payloadOffset, payloadOffset + IV_SIZE)
            payloadOffset += IV_SIZE
            
            // Ciphertext is the rest
            val cipherText = bytes.copyOfRange(payloadOffset, bytes.size)
            
            try {
                val key = deriveKey(password, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(TAG_SIZE, iv)
                cipher.init(Cipher.DECRYPT_MODE, key, spec)
                plainBytes = cipher.doFinal(cipherText)
            } catch (e: Exception) {
                throw SecurityException("Incorrect password or corrupted file")
            }
        } else {
            plainBytes = bytes.copyOfRange(payloadOffset, bytes.size)
        }
        
        // Decompress
        val jsonBytes = decompress(plainBytes)
        val json = String(jsonBytes, StandardCharsets.UTF_8)
        
        return Pair(gson.fromJson(json, AppConfig::class.java), false)
    }
    
    // Check if file is encrypted without decrypting
    fun isEncrypted(input: InputStream): Boolean {
         val headerBytes = ByteArray(7)
         if (input.read(headerBytes) < 7) return false
         val header = String(headerBytes.copyOfRange(0, 5), StandardCharsets.UTF_8)
         if (header != MAGIC_HEADER) return false
         return headerBytes[6] == FLAG_ENCRYPTED
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    private fun compress(data: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        GZIPOutputStream(output).use { it.write(data) }
        return output.toByteArray()
    }

    private fun decompress(data: ByteArray): ByteArray {
        GZIPInputStream(ByteArrayInputStream(data)).use { 
            return it.readBytes() 
        }
    }
}
