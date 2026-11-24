package com.example

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.AiFailoverOrchestrator
import com.example.data.backup.ExportManager
import com.example.data.local.AppDatabase
import com.example.data.local.ItemReminderEntity
import com.example.data.local.KeepNoteEntity
import com.example.data.local.TaskItemEntity
import com.example.data.local.TaskPriority
import com.example.data.local.TaskRepeat
import com.example.data.repository.FinanceRepository
import com.example.domain.NoteBlock
import com.example.domain.NoteJsonCodec
import com.example.domain.SmartToolsProEngine
import com.example.domain.TaskRepeatEngine
import com.example.domain.TaskSubtaskItem
import com.example.widget.BalanceSummaryWidget
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.IOException
import java.net.SocketTimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProUpgradeFeaturesTest {

    @After
    fun idleLooper() {
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `smart tools pro financial security and dev helpers compute accurately`() {
        val ci = SmartToolsProEngine.calculateCompoundInterest(
            principal = 10000.0,
            annualRatePercent = 12.0,
            years = 2.0,
            compoundsPerYear = 12,
            monthlyContribution = 500.0
        )
        assertEquals(22000.0, ci.totalContributed, 0.01)
        assertTrue(ci.finalBalance > 26000.0)
        assertTrue(ci.totalInterestEarned > 4000.0)

        val split = SmartToolsProEngine.calculateSplitBill(
            billAmount = 2000.0,
            tipPercent = 10.0,
            taxPercent = 5.0,
            peopleCount = 4
        )
        assertEquals(200.0, split.tipAmount, 0.01)
        assertEquals(100.0, split.taxAmount, 0.01)
        assertEquals(2300.0, split.grandTotal, 0.01)
        assertEquals(575.0, split.perPerson, 0.01)

        val disc = SmartToolsProEngine.calculateDiscountVat(
            originalPrice = 1000.0,
            discountPercent = 20.0,
            extraCouponPercent = 10.0,
            vatPercent = 15.0
        )
        assertEquals(280.0, disc.discountSaved, 0.01)
        assertEquals(720.0, disc.priceAfterDiscount, 0.01)
        assertEquals(108.0, disc.vatAmount, 0.01)
        assertEquals(828.0, disc.finalPayable, 0.01)

        val pwd = SmartToolsProEngine.generateStrongPassword(length = 20)
        assertEquals(20, pwd.password.length)
        assertTrue(pwd.entropyBits >= 90)
        assertEquals("Fortress", pwd.strengthLabel)

        val sha = SmartToolsProEngine.computeHash("hello", "SHA-256")
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", sha)

        val encodedUrl = SmartToolsProEngine.urlEncode("smart finance & life")
        assertEquals("smart finance & life", SmartToolsProEngine.urlDecode(encodedUrl).getOrThrow())

        val b64 = SmartToolsProEngine.encodeTextBase64("Smart Finance Pro")
        assertEquals("Smart Finance Pro", SmartToolsProEngine.decodeTextBase64(b64).getOrThrow())

        val regexMatches = SmartToolsProEngine.testRegexMatches("\\d+", "Inv #102 and #409").getOrThrow()
        assertEquals(listOf("102", "409"), regexMatches)
    }

    @Test
    fun `task repeat engine handles custom interval and bounds cleanly`() {
        val base = 1_700_000_000_000L
        val next3Days = TaskRepeatEngine.advanceDueAt(
            currentDueAt = base,
            repeatRule = TaskRepeat.CUSTOM,
            repeatInterval = 3,
            repeatUnit = "DAY"
        )
        assertNotNull(next3Days)
        assertEquals(base + 3L * 86_400_000L, next3Days!!)

        assertTrue(TaskRepeatEngine.shouldSpawnNextOccurrence(TaskRepeat.DAILY, next3Days, null, 2))
        assertFalse(TaskRepeatEngine.shouldSpawnNextOccurrence(TaskRepeat.DAILY, next3Days, null, 1))
        assertFalse(TaskRepeatEngine.shouldSpawnNextOccurrence(TaskRepeat.DAILY, next3Days, base + 86_400_000L, 5))
        assertEquals(1, TaskRepeatEngine.decrementedCount(2))
    }

    @Test
    fun `ai failover orchestrator detects rate limit and quota failures`() {
        assertTrue(AiFailoverOrchestrator.isSwitchableFailure(SocketTimeoutException("timed out")))
        assertTrue(AiFailoverOrchestrator.isSwitchableFailure(IOException("HTTP 429 Too Many Requests")))
        assertTrue(AiFailoverOrchestrator.isSwitchableFailure(IllegalStateException("insufficient_quota on key")))
        assertTrue(AiFailoverOrchestrator.isSwitchableFailure(IllegalStateException("HTTP 402 billing credit exhausted")))
        assertFalse(AiFailoverOrchestrator.isSwitchableFailure(IllegalArgumentException("Malformed image prompt")))
    }

    @Test
    fun `notes versions task lists reminders and v4 backup round trip work cleanly`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = FinanceRepository(context)
        repo.ensureInitialized()
        val uniqueUsername = "prouser_${System.currentTimeMillis()}"
        val userId = repo.registerUser(uniqueUsername, "pass123", "Pro User", "Pet", "Cat").getOrThrow().id

        // 7 default task lists seeded for new user
        val lists = repo.observeTaskLists(userId).first()
        assertTrue(lists.size >= 7)
        assertTrue(lists.any { it.name == "University" })

        // Note versioning records snapshots on save
        val blocks = NoteJsonCodec.encodeBlocks(listOf(NoteBlock(type = "CODE", content = "val x = 1")))
        val noteId = repo.saveKeepNote(
            KeepNoteEntity(
                ownerUserId = userId,
                title = "Lab Notes",
                content = "First draft",
                blocksJson = blocks,
                isLocked = true
            ),
            saveVersionSnapshot = true
        )
        repo.saveKeepNote(
            KeepNoteEntity(
                id = noteId,
                ownerUserId = userId,
                title = "Lab Notes v2",
                content = "Second draft",
                blocksJson = blocks,
                isLocked = true
            ),
            saveVersionSnapshot = true
        )
        val versions = repo.observeNoteVersions(noteId).first()
        assertTrue(versions.size >= 2)

        // Recurring task toggles cleanly and advances next occurrence while saving completed history
        val dueNow = System.currentTimeMillis() + 86_400_000L
        val subs = NoteJsonCodec.encodeSubtasks(listOf(TaskSubtaskItem(title = "Research", done = true)))
        val taskId = repo.saveTaskItem(
            TaskItemEntity(
                ownerUserId = userId,
                title = "Weekly Audit",
                priority = TaskPriority.HIGH,
                dueAt = dueNow,
                repeatRule = TaskRepeat.WEEKLY,
                subtasksJson = subs
            )
        )
        val savedTask = repo.observeTaskItems(userId).first().first { it.id == taskId }
        repo.toggleTaskCompleted(savedTask)
        val updatedTasks = repo.observeTaskItems(userId).first()
        assertTrue(updatedTasks.any { it.title == "Weekly Audit" && it.isCompleted })
        assertTrue(updatedTasks.any { it.id == taskId && !it.isCompleted && (it.dueAt ?: 0L) > dueNow })

        // Multiple reminders + v4 backup snapshot round-trip
        repo.saveItemReminder(
            ItemReminderEntity(
                ownerUserId = userId,
                targetType = "TASK",
                targetId = taskId,
                titleSnapshot = "Weekly Audit Alert",
                triggerAt = dueNow - 900_000L,
                offsetMinutes = 15,
                repeatType = "WEEKLY"
            )
        )
        val snap = repo.buildFullDatabaseSnapshot()
        assertEquals(4, snap.schemaVersion)
        assertTrue(snap.taskLists.any { it.name == "University" })
        assertTrue(snap.noteVersions.any { it.noteId == noteId })
        assertTrue(snap.itemReminders.any { it.titleSnapshot == "Weekly Audit Alert" })

        val roundTrip = ExportManager.jsonToSnapshot(ExportManager.snapshotToJson(snap)).getOrThrow()
        assertTrue(roundTrip.noteVersions.any { it.titleSnapshot == "Lab Notes v2" })
        assertTrue(roundTrip.itemReminders.any { it.offsetMinutes == 15 })

        // Clean up shared state
        repo.logout()
    }

    @Test
    fun `in memory room database builds cleanly with version 3 schema`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        assertNotNull(memDb.noteVersionDao())
        assertNotNull(memDb.taskListDao())
        assertNotNull(memDb.itemReminderDao())
        memDb.close()
    }

    @Test
    fun `widget new transaction deep link intent starts main activity cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(BalanceSummaryWidget.EXTRA_WIDGET_ACTION, BalanceSummaryWidget.ACTION_NEW_TRANSACTION)
        }
        val controller = Robolectric.buildActivity(MainActivity::class.java, launchIntent).setup()
        assertNotNull(controller.get())
        shadowOf(Looper.getMainLooper()).idle()
        controller.pause().stop().destroy()
        shadowOf(Looper.getMainLooper()).idle()
    }
}
