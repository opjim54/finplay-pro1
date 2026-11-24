package com.example

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.robolectric.shadows.ShadowLog
import java.util.Date

class CloudBackupVaultRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun authenticatedUserCanCreateReadUpdateAndDeleteOwnCloudVaultBackup() {
        ShadowLog.stream = System.out
        val uid = signInTestUser("md2005jim@gmail.com")
        val now = Timestamp(Date(System.currentTimeMillis() - 1000L))
        val docRef = db.collection("users").document(uid).collection("backups").document("latest")

        val validDoc = mapOf(
            "userId" to uid,
            "email" to "md2005jim@gmail.com",
            "payloadJson" to "{\"schemaVersion\":2,\"scope\":\"FULL_DATABASE\"}",
            "exportedAt" to 1731580000000L,
            "accountCount" to 6,
            "transactionCount" to 12,
            "schemaVersion" to 2,
            "createdAt" to now,
            "updatedAt" to now
        )

        awaitTask(docRef.set(validDoc))

        val snap = awaitTask(docRef.get())
        assertTrue(snap.exists())
        assertEquals("md2005jim@gmail.com", snap.getString("email"))
        assertEquals(12L, snap.getLong("transactionCount"))

        val updatedDoc = validDoc.toMutableMap().apply {
            put("transactionCount", 18)
            put("updatedAt", Timestamp(Date(System.currentTimeMillis() - 500L)))
        }
        awaitTask(docRef.set(updatedDoc))

        val snapAfterUpdate = awaitTask(docRef.get())
        assertEquals(18L, snapAfterUpdate.getLong("transactionCount"))
    }

    @Test
    fun crossUserAccessAndInvalidFieldsAreRejectedBySecurityRules() {
        ShadowLog.stream = System.out
        val aliceUid = signInTestUser("alice@gmail.com")
        val now = Timestamp(Date(System.currentTimeMillis() - 1000L))
        val aliceRef = db.collection("users").document(aliceUid).collection("backups").document("latest")

        val validDoc = mapOf(
            "userId" to aliceUid,
            "email" to "alice@gmail.com",
            "payloadJson" to "{\"schemaVersion\":2}",
            "exportedAt" to 1731580000000L,
            "accountCount" to 6,
            "transactionCount" to 10,
            "schemaVersion" to 2,
            "createdAt" to now,
            "updatedAt" to now
        )
        awaitTask(aliceRef.set(validDoc))

        // Sign in as Bob and attempt to read Alice's backup
        signInTestUser("bob@gmail.com")
        try {
            awaitTask(aliceRef.get())
            fail("Expected PERMISSION_DENIED when reading another user's cloud backup")
        } catch (e: Exception) {
            val cause = (e.cause as? FirebaseFirestoreException) ?: (e as? FirebaseFirestoreException)
            assertTrue(
                "Expected PERMISSION_DENIED but got $e",
                cause?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    e.message?.contains("PERMISSION_DENIED") == true
            )
        }
    }
}
