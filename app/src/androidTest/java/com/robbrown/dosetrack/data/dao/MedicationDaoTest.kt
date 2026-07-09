package com.robbrown.dosetrack.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robbrown.dosetrack.data.DoseTrackDatabase
import com.robbrown.dosetrack.data.entity.Medication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicationDaoTest {

    private lateinit var db: DoseTrackDatabase
    private lateinit var dao: MedicationDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, DoseTrackDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.medicationDao()
    }

    @After
    fun tearDown() = db.close()

    private fun medication(id: String, name: String = "Metformin", active: Boolean = true, sort: Int = 0) =
        Medication(id = id, name = name, dosage = "500mg", unit = "pill", isActive = active, sortOrder = sort)

    @Test
    fun insert_then_getById_returnsTheMedication() = runTest {
        val med = medication("m1", name = "Metformin")
        dao.insert(med)

        assertEquals(med, dao.getById("m1"))
    }

    @Test
    fun getById_unknownId_returnsNull() = runTest {
        assertNull(dao.getById("nope"))
    }

    @Test
    fun observeActive_returnsOnlyActive_sortedBySortOrder() = runTest {
        dao.insert(medication("a", name = "Bravo", active = true, sort = 2))
        dao.insert(medication("b", name = "Alpha", active = true, sort = 1))
        dao.insert(medication("c", name = "Inactive", active = false, sort = 0))

        val active = dao.observeActive().first()

        assertEquals(listOf("b", "a"), active.map { it.id })
    }
}
