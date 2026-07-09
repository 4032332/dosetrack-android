package com.robbrown.dosetrack.ui.medications

import androidx.lifecycle.ViewModel
import com.robbrown.dosetrack.data.entity.Medication
import com.robbrown.dosetrack.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Backs the Medications screen: exposes the active-medication list and the
 * add/soft-delete operations, delegating all business rules (free-tier cap,
 * soft-delete-before-permanent-delete) to [MedicationRepository].
 *
 * [medications] is exposed as a plain (not shared/`stateIn`) [Flow]: Room re-runs the
 * query on every fresh collection, so it always reflects the current DB state without
 * the staleness/timing pitfalls of a hot `StateFlow` cache. The UI collects it with
 * `collectAsStateWithLifecycle`.
 */
@HiltViewModel
class MedicationsViewModel @Inject constructor(
    private val repository: MedicationRepository,
) : ViewModel() {

    val medications: Flow<List<Medication>> = repository.observeActiveMedications()

    suspend fun addMedication(medication: Medication, isPro: Boolean): Result<Unit> =
        repository.addMedication(medication, isPro)

    suspend fun softDelete(medication: Medication) = repository.softDelete(medication)
}
