package com.example.data.model

/**
 * Ny satan'ny fifandraisana sy fampitoviana angona (Synchronisation) amin'ny Supabase:
 * - SYNCHRONISE: Vita soa aman-tsara ny fampitoviana, mitovy tanteraka ny ao amin'ny APK sy ny ao amin'ny Supabase.
 * - EN_COURS: Misy fampidirana na fandefasana angona mandeha amin'izao fotoana izao (temps réel).
 * - HORS_LIGNE: Tsy misy fifandraisana aterineto, voatahiry eo an-toerana ao amin'ny SQLite Room ny angona.
 * - ERREUR: Nisy olana teo am-pampitoviana (olana ara-tambajotra na serivera).
 */
enum class GlobalSyncState(
    val code: String,
    val label: String,
    val description: String
) {
    SYNCHRONISE(
        code = "SYNCHRONISE",
        label = "Synchronisé",
        description = "Données à jour avec Supabase"
    ),
    EN_COURS(
        code = "EN_COURS",
        label = "Synchronisation en cours",
        description = "Transfert et écoute temps réel..."
    ),
    HORS_LIGNE(
        code = "HORS_LIGNE",
        label = "Hors ligne",
        description = "Mode local actif (SQLite Room)"
    ),
    ERREUR(
        code = "ERREUR",
        label = "Erreur",
        description = "Erreur lors de la synchronisation"
    );

    fun isOnline(): Boolean = this == SYNCHRONISE || this == EN_COURS
}
