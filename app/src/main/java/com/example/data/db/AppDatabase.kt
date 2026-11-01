package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AnnonceEntity
import com.example.data.model.EnqueteEntity
import com.example.data.model.NotificationEntity
import com.example.data.model.OptionEntity
import com.example.data.model.PersonneEntity
import com.example.data.model.QuestionEntity
import com.example.data.model.QuestionnaireEntity
import com.example.data.model.ReponseEntity
import com.example.data.model.UserEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Safe schema migration step without dropping user data
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Safe schema migration step without dropping user data
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE users ADD COLUMN photo TEXT")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE users ADD COLUMN photo_url TEXT")
        } catch (_: Exception) {}
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE enquetes ADD COLUMN responses_json TEXT")
        } catch (_: Exception) {}
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS annonces (
                    id_annonce TEXT PRIMARY KEY NOT NULL,
                    titre TEXT NOT NULL,
                    contenu TEXT,
                    type_annonce TEXT NOT NULL DEFAULT 'TEXTE',
                    priorite TEXT NOT NULL DEFAULT 'NORMALE',
                    statut TEXT NOT NULL DEFAULT 'PUBLIEE',
                    cible_type TEXT NOT NULL DEFAULT 'TOUS',
                    date_publication TEXT,
                    date_expiration TEXT,
                    created_by INTEGER,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
            """)
        } catch (_: Exception) {}
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE annonces ADD COLUMN cible_valeur TEXT")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE annonces ADD COLUMN author_name TEXT")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE annonces ADD COLUMN author_role TEXT")
        } catch (_: Exception) {}

        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS annonce_fichiers (
                    id_fichier TEXT PRIMARY KEY NOT NULL,
                    id_annonce TEXT NOT NULL,
                    nom_fichier TEXT NOT NULL,
                    chemin_storage TEXT,
                    type_mime TEXT,
                    taille_octets INTEGER NOT NULL DEFAULT 0,
                    data_url TEXT,
                    uploaded_at TEXT NOT NULL,
                    FOREIGN KEY (id_annonce) REFERENCES annonces(id_annonce) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_annonce_fichiers_id_annonce ON annonce_fichiers(id_annonce)")
        } catch (_: Exception) {}

        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS annonce_destinataires (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    id_annonce TEXT NOT NULL,
                    id_user INTEGER NOT NULL,
                    date_envoi TEXT,
                    date_lecture TEXT,
                    statut_lecture TEXT NOT NULL DEFAULT 'NON_LU',
                    FOREIGN KEY (id_annonce) REFERENCES annonces(id_annonce) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_annonce_destinataires_id_annonce_id_user ON annonce_destinataires(id_annonce, id_user)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_annonce_destinataires_id_annonce ON annonce_destinataires(id_annonce)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_annonce_destinataires_id_user ON annonce_destinataires(id_user)")
        } catch (_: Exception) {}
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE users ADD COLUMN region TEXT NOT NULL DEFAULT 'Analamanga'")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE users ADD COLUMN matricule TEXT")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE users ADD COLUMN photo TEXT")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE users ADD COLUMN photo_url TEXT")
        } catch (_: Exception) {}
        try {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_users_matricule ON users(matricule)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_users_region ON users(region)")
        } catch (_: Exception) {}
    }
}

@Database(
    entities = [
        UserEntity::class,
        PersonneEntity::class,
        QuestionnaireEntity::class,
        QuestionEntity::class,
        OptionEntity::class,
        EnqueteEntity::class,
        ReponseEntity::class,
        NotificationEntity::class,
        AnnonceEntity::class,
        com.example.data.model.AnnonceFichierEntity::class,
        com.example.data.model.AnnonceDestinataireEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun surveyDao(): SurveyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "enquetes_terrain.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

