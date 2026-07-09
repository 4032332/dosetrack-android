package com.robbrown.dosetrack

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.robbrown.dosetrack.data.DoseTrackDatabase
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the Room schema is valid: the database opens and exposes all three DAOs.
 * Uses an in-memory database so no state persists between runs.
 */
@RunWith(AndroidJUnit4::class)
class DoseTrackDatabaseTest {

    @Test
    fun database_opens_and_exposes_daos() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, DoseTrackDatabase::class.java).build()

        assertNotNull(db.medicationDao())
        assertNotNull(db.scheduleDao())
        assertNotNull(db.doseLogDao())

        // Forces schema creation; throws if any entity/converter is misconfigured.
        db.openHelper.writableDatabase
        db.close()
    }
}
