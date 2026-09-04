package com.example.linee_langer.domain.models

import com.example.linee_langer.R

enum class LangerGoal(
    val id: String,             // chiave usata in Firestore e DataStore
    val titleRes: Int,          // stringa localizzata del titolo
    val subtitleRes: Int,       // stringa localizzata del sottotitolo
    val descriptionRes: Int,    // stringa localizzata della descrizione
    val iconRes: Int            // drawable dell'icona
) {
    SURGICAL_PLANNING(
        id          = "surgical_planning",
        titleRes    = R.string.goal_surgical_planning_title,
        subtitleRes = R.string.goal_surgical_planning_sub,
        descriptionRes = R.string.goal_surgical_planning_desc,
        iconRes     = R.drawable.ic_analysis
    ),
    SCAR_MINIMIZATION(
        id          = "scar_minimization",
        titleRes    = R.string.goal_scar_minimization_title,
        subtitleRes = R.string.goal_scar_minimization_sub,
        descriptionRes = R.string.goal_scar_minimization_desc,
        iconRes     = R.drawable.ic_profile
    ),
    PREOPERATIVE_EVAL(
        id          = "preoperative_eval",
        titleRes    = R.string.goal_preoperative_eval_title,
        subtitleRes = R.string.goal_preoperative_eval_sub,
        descriptionRes = R.string.goal_preoperative_eval_desc,
        iconRes     = R.drawable.ic_camera
    ),
    STUDY_RESEARCH(
        id          = "study_research",
        titleRes    = R.string.goal_study_research_title,
        subtitleRes = R.string.goal_study_research_sub,
        descriptionRes = R.string.goal_study_research_desc,
        iconRes     = R.drawable.ic_star
    ),
    HEALING_MONITOR(
        id          = "healing_monitor",
        titleRes    = R.string.goal_healing_monitor_title,
        subtitleRes = R.string.goal_healing_monitor_sub,
        descriptionRes = R.string.goal_healing_monitor_desc,
        iconRes     = R.drawable.ic_home
    ),
    PATIENT_CONSULT(
        id          = "patient_consult",
        titleRes    = R.string.goal_patient_consult_title,
        subtitleRes = R.string.goal_patient_consult_sub,
        descriptionRes = R.string.goal_patient_consult_desc,
        iconRes     = R.drawable.ic_profile
    );

    companion object {

        // Converte Set<LangerGoal> in lista di ID stringa per la persistenza
        fun toIds(goals: Set<LangerGoal>): List<String> =
            goals.map { it.id }
    }
}