package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Table QUESTIONNAIRES (Schéma Supabase / PostgreSQL)
 */
@Entity(
    tableName = "questionnaires",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id_user"],
            childColumns = ["created_by"],
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["created_by"]),
        Index(value = ["statut"])
    ]
)
data class QuestionnaireEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_questionnaire")
    val idQuestionnaire: Long = 0,
    val titre: String,
    val description: String? = null,
    val version: String = "v1.0",
    val statut: String = "PUBLIE", // BROUILLON, PUBLIE, ARCHIVE
    @ColumnInfo(name = "created_by")
    val createdBy: Long = 1,
    @ColumnInfo(name = "created_at")
    val createdAt: String = System.currentTimeMillis().toString(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = System.currentTimeMillis().toString()
)

/**
 * Modèle de suivi direct en temps réel pour Supabase (PUBLIÉ vs BROUILLON)
 */
data class SupabaseQuestionnaireStatus(
    val idQuestionnaire: Long,
    val titre: String,
    val description: String? = null,
    val version: String = "v1.0",
    val statut: String = "PUBLIÉ",
    val isPublished: Boolean = true,
    val questionsCount: Int = 0,
    val updatedAt: String = ""
)

