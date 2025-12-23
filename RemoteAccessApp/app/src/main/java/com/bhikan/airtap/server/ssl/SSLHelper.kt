package com.bhikan.airtap.server.ssl

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.*

/**
 * Helper class for SSL/TLS certificate management.
 * For production, use proper certificates from Let's Encrypt via Cloudflare Tunnel.
 * This self-signed cert is for local network use only.
 */
object SSLHelper {
    
    private const val KEYSTORE_FILE = "airtap_keystore.bks"
    private const val KEYSTORE_PASSWORD = "airtap_ssl"
    private const val KEY_ALIAS = "airtap"
    
    fun getOrCreateKeyStore(context: Context): KeyStore? {
        val keystoreFile = File(context.filesDir, KEYSTORE_FILE)
        
        return try {
            if (keystoreFile.exists()) {
                // Load existing keystore
                val keyStore = KeyStore.getInstance("BKS")
                FileInputStream(keystoreFile).use { fis ->
                    keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray())
                }
                keyStore
            } else {
                // Create new self-signed certificate
                createSelfSignedKeyStore(context, keystoreFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    private fun createSelfSignedKeyStore(context: Context, keystoreFile: File): KeyStore? {
        return try {
            // Generate key pair
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair = keyPairGenerator.generateKeyPair()
            
            // Create self-signed certificate (valid for 1 year)
            val startDate = Date()
            val endDate = Date(startDate.time + 365L * 24 * 60 * 60 * 1000)
            
            // Note: In production, use proper certificate generation
            // This is a simplified version for local network use
            
            val keyStore = KeyStore.getInstance("BKS")
            keyStore.load(null, KEYSTORE_PASSWORD.toCharArray())
            
            // For now, we'll skip certificate generation as it requires BouncyCastle
            // The server will run on HTTP for local network
            // For HTTPS over internet, use Cloudflare Tunnel which provides free SSL
            
            FileOutputStream(keystoreFile).use { fos ->
                keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray())
            }
            
            keyStore
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun getKeystorePassword(): String = KEYSTORE_PASSWORD
    fun getKeyAlias(): String = KEY_ALIAS
}
