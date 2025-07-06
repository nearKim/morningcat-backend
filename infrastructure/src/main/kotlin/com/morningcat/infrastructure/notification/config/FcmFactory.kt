package com.morningcat.infrastructure.notification.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import java.io.ByteArrayInputStream

object FcmFactory {
    fun createFirebaseMessaging(config: FcmConfig): FirebaseMessaging {
        val firebaseApp = initializeFirebaseApp(config)
        return FirebaseMessaging.getInstance(firebaseApp)
    }

    private fun initializeFirebaseApp(config: FcmConfig): FirebaseApp {
        // Check if app already exists to avoid duplicate initialization
        val appName = "morningcat-fcm"

        return FirebaseApp.getApps().find { it.name == appName }
            ?: createNewFirebaseApp(config, appName)
    }

    private fun createNewFirebaseApp(
        config: FcmConfig,
        appName: String,
    ): FirebaseApp {
        val serviceAccountJson = buildServiceAccountJson(config)
        val credentials =
            GoogleCredentials.fromStream(
                ByteArrayInputStream(serviceAccountJson.toByteArray()),
            )

        val options =
            FirebaseOptions
                .builder()
                .setCredentials(credentials)
                .setProjectId(config.projectId)
                .build()

        return FirebaseApp.initializeApp(options, appName)
    }

    private fun buildServiceAccountJson(config: FcmConfig): String {
        // Build service account JSON from config
        // In production, this would typically come from a secure credential store
        return """
            {
              "type": "service_account",
              "project_id": "${config.projectId}",
              "private_key_id": "${config.privateKeyId}",
              "private_key": "${config.privateKey}",
              "client_email": "${config.clientEmail}",
              "client_id": "${config.clientId}",
              "auth_uri": "https://accounts.google.com/o/oauth2/auth",
              "token_uri": "https://oauth2.googleapis.com/token",
              "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
              "client_x509_cert_url": "https://www.googleapis.com/robot/v1/metadata/x509/${config.clientEmail}"
            }
            """.trimIndent()
    }
}
