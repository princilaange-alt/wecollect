package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Table NOTIFICATIONS (Schéma Supabase / PostgreSQL)
 * Gère les notifications système et les alertes d'enquêtes/synchronisation.
 */
@Entity(
    tableName = "notifications",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id_user"],
            childColumns = ["id_user"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EnqueteEntity::class,
            parentColumns = ["id_enquete"],
            childColumns = ["id_enquete"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = QuestionnaireEntity::class,
            parentColumns = ["id_questionnaire"],
            childColumns = ["id_questionnaire"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["id_user"]),
        Index(value = ["id_enquete"]),
        Index(value = ["id_questionnaire"]),
        Index(value = ["id_user", "lu"]),
        Index(value = ["created_at"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_notification")
    val idNotification: Long = 0,
    @ColumnInfo(name = "id_user")
    val idUser: Long,
    @ColumnInfo(name = "type")
    val type: String, // NOUVELLE_ENQUETE, QUESTIONNAIRE_PUBLIE, SYNCHRONISATION, SYNCHRONISATION_ERREUR, DOUBLON_DETECTE, ENQUETE_VALIDEE, ENQUETE_REJETEE, SYSTEME
    @ColumnInfo(name = "titre")
    val titre: String,
    @ColumnInfo(name = "message")
    val message: String,
    @ColumnInfo(name = "id_enquete")
    val idEnquete: Long? = null,
    @ColumnInfo(name = "id_questionnaire")
    val idQuestionnaire: Long? = null,
    @ColumnInfo(name = "lu")
    val lu: Boolean = false,
    @ColumnInfo(name = "lu_at")
    val luAt: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: String = System.currentTimeMillis().toString()
)
