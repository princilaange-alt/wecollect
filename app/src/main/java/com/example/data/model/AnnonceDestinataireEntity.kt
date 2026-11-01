package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "annonce_destinataires",
    foreignKeys = [
        ForeignKey(
            entity = AnnonceEntity::class,
            parentColumns = ["id_annonce"],
            childColumns = ["id_annonce"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["id_annonce", "id_user"], unique = true),
        Index(value = ["id_annonce"]),
        Index(value = ["id_user"])
    ]
)
data class AnnonceDestinataireEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "id_annonce")
    val idAnnonce: String,

    @ColumnInfo(name = "id_user")
    val idUser: Long,

    @ColumnInfo(name = "date_envoi")
    val dateEnvoi: String? = null,

    @ColumnInfo(name = "date_lecture")
    val dateLecture: String? = null,

    @ColumnInfo(name = "statut_lecture")
    val statutLecture: String = "NON_LU" // NON_LU, LU
)
