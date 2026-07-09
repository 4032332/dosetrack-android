package com.robbrown.dosetrack.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robbrown.dosetrack.data.DoseTrackDatabase
import com.robbrown.dosetrack.data.entity.DoseLog
import com.robbrown.dosetrack.data.entity.Medication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DoseLogDaoTest {

    private lateinit var db: DoseTrackDatabase
    private lateinit var medicationDao: MedicationDao
    private lateinit var dao: DoseLogDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, DoseTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        medicationDao = db.medicationDao()
        dao = db.doseLogDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun medication(id: String) {
        medicationDao.insert(
            Medication(id = id, name = "Metformin", dosage = "500mg", unit = "pill")
        )
    }

    private fun log(id: String, medicationId: String, scheduledAt: Long, status: String = "taken") =
        DoseLog(id = id, medicationId = medicationId, scheduledAt = scheduledAt, status = status)

    @Test
    fun insert_then_getById_returnsTheLog() = runTest {
        medication("med1")
        val entry = log("l1", "med1", scheduledAt = 1000L)
        dao.insert(entry)

        assertEquals(entry, dao.getById("l1"))
    }

    @Test
    fun observeForMedication_returnsLogsNewestFirst() = runTest {
        medication("med1")
        dao.insert(log("l1", "med1", scheduledAt = 1000L))
        dao.insert(log("l2", "med1", scheduledAt = 3000L))
        dao.insert(log("l3", "med1", scheduledAt = 2000L))

        val logs = dao.observeForMedication("med1").first()

        assertEquals(listOf("l2", "l3", "l1"), logs.map { it.id })
    }

    @Test
    fun observeInRange_excludesLogsOutsideTheWindow() = runTest {
        medication("med1")
        dao.insert(log("early", "med1", scheduledAt = 500L))
        dao.insert(log("inRange", "med1", scheduledAt = 1500L))
        dao.insert(log("late", "med1", scheduledAt = 2500L))

        val inRange = dao.observeInRange(startInclusive = 1000L, endInclusive = 2000L).first()

        assertEquals(listOf("inRange"), inRange.map { it.id })
    }
}
