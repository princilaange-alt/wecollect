package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "annonce_fichiers",
    foreignKeys = [
        ForeignKey(
            entity = AnnonceEntity::class,
            parentColumns = ["id_annonce"],
            childColumns = ["id_annonce"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["id_annonce"])
    ]
)
data class AnnonceFichierEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_fichier")
    val idFichier: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "id_annonce")
    val idAnnonce: String,

    @ColumnInfo(name = "nom_fichier")
    val nomFichier: String,

    @ColumnInfo(name = "chemin_storage")
    val cheminStorage: String? = null,

    @ColumnInfo(name = "type_mime")
    val typeMime: String? = null,

    @ColumnInfo(name = "taille_octets")
    val tailleOctets: Long = 0L,

    @ColumnInfo(name = "data_url")
    val dataUrl: String? = null,

    @ColumnInfo(name = "uploaded_at")
    val uploadedAt: String = System.currentTimeMillis().toString()
)
