package com.robbrown.dosetrack.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robbrown.dosetrack.data.DoseTrackDatabase
import com.robbrown.dosetrack.data.entity.Medication
import com.robbrown.dosetrack.data.entity.Schedule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScheduleDaoTest {

    private lateinit var db: DoseTrackDatabase
    private lateinit var medicationDao: MedicationDao
    private lateinit var dao: ScheduleDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, DoseTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        medicationDao = db.medicationDao()
        dao = db.scheduleDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun medication(id: String) {
        medicationDao.insert(
            Medication(id = id, name = "Metformin", dosage = "500mg", unit = "pill")
        )
    }

    private fun schedule(id: String, medicationId: String, days: List<Int> = listOf(1, 3, 5)) =
        Schedule(
            id = id,
            medicationId = medicationId,
            hour = 8,
            minute = 30,
            daysOfWeek = days,
            frequency = "daily",
            notificationIds = listOf("notif-$id"),
        )

    @Test
    fun insert_then_getById_preservesListFields() = runTest {
        medication("med1")
        val sched = schedule("s1", "med1", days = listOf(1, 3, 5))
        dao.insert(sched)

        val loaded = dao.getById("s1")

        assertEquals(sched, loaded)
        assertEquals(listOf(1, 3, 5), loaded?.daysOfWeek)
        assertEquals(listOf("notif-s1"), loaded?.notificationIds)
    }

    @Test
    fun observeForMedication_returnsOnlySchedulesForThatMedication() = runTest {
        medication("med1")
        medication("med2")
        dao.insert(schedule("s1", "med1"))
        dao.insert(schedule("s2", "med1"))
        dao.insert(schedule("s3", "med2"))

        val forMed1 = dao.observeForMedication("med1").first()

        assertEquals(setOf("s1", "s2"), forMed1.map { it.id }.toSet())
    }

    @Test
    fun deletingMedication_cascadeDeletesItsSchedules() = runTest {
        medication("med1")
        dao.insert(schedule("s1", "med1"))

        medicationDao.delete(medicationDao.getById("med1")!!)

        assertTrue(dao.observeForMedication("med1").first().isEmpty())
    }
}
