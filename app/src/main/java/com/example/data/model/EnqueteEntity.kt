package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Table ENQUETES (Schéma Supabase / PostgreSQL)
 * Lie une personne identifiée (id_personne) à un questionnaire (id_questionnaire) et à un enquêteur (id_enqueteur).
 */
@Entity(
    tableName = "enquetes",
    foreignKeys = [
        ForeignKey(
            entity = PersonneEntity::class,
            parentColumns = ["id_personne"],
            childColumns = ["id_personne"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionnaireEntity::class,
            parentColumns = ["id_questionnaire"],
            childColumns = ["id_questionnaire"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id_user"],
            childColumns = ["id_enqueteur"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["id_personne", "id_questionnaire"], unique = true),
        Index(value = ["id_personne"]),
        Index(value = ["id_questionnaire"]),
        Index(value = ["id_enqueteur"]),
        Index(value = ["statut"]),
        Index(value = ["sync_status"])
    ]
)
data class EnqueteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_enquete")
    val idEnquete: Long = 0,
    @ColumnInfo(name = "id_personne")
    val idPersonne: Long,
    @ColumnInfo(name = "id_questionnaire")
    val idQuestionnaire: Long,
    @ColumnInfo(name = "id_enqueteur")
    val idEnqueteur: Long,
    @ColumnInfo(name = "date_enquete")
    val dateEnquete: String,
    val statut: String = "TERMINEE", // EN_COURS, TERMINEE, VALIDEE, REJETE
    @ColumnInfo(name = "sync_status")
    val syncStatus: String = "EN_ATTENTE", // EN_ATTENTE, SYNCHRONISEE, ERREUR
    @ColumnInfo(name = "responses_json")
    val responsesJson: String? = null, // JSON string for dynamic survey responses
    @ColumnInfo(name = "created_at")
    val createdAt: String = System.currentTimeMillis().toString(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = System.currentTimeMillis().toString()
)

