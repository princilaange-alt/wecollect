package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Table QUESTIONS (Schéma Supabase / PostgreSQL)
 */
@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = QuestionnaireEntity::class,
            parentColumns = ["id_questionnaire"],
            childColumns = ["id_questionnaire"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["id_questionnaire"]),
        Index(value = ["id_questionnaire", "ordre"])
    ]
)
data class QuestionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_question")
    val idQuestion: Long = 0,
    @ColumnInfo(name = "id_questionnaire")
    val idQuestionnaire: Long,
    val libelle: String,
    val type: String, // TEXTE, NOMBRE, DATE, CHOIX_UNIQUE, CHOIX_MULTIPLE
    val obligatoire: Boolean = false,
    val ordre: Int = 1,
    @ColumnInfo(name = "est_code_unique")
    val estCodeUnique: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: String = System.currentTimeMillis().toString(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: String = System.currentTimeMillis().toString()
)

