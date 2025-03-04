package com.ps.redmine.resources

enum class Language {
    ENGLISH,
    FRENCH
}

object Strings {
    private var currentLanguage = run {
        val envLang = System.getenv("REDMINE_LANG")?.lowercase()
        println("[DEBUG_LOG] Environment REDMINE_LANG: $envLang")

        val lang = when (envLang) {
            "en" -> Language.ENGLISH
            else -> Language.FRENCH  // Default to French for any other value or if not set
        }
        println("[DEBUG_LOG] Selected language: $lang")
        lang
    }


    private val strings = mapOf(
        Language.FRENCH to mapOf(
            "window_title" to "Suivi du temps Redmine",
            "project_label" to "Projet",
            "hours_label" to "Heures",
            "issue_label" to "Ticket",
            "activity_label" to "Activité",
            "comments_label" to "Commentaires",
            "select_issue_placeholder" to "Sélectionner un ticket",
            "add_entry" to "Ajouter (⌘S)",
            "update_entry" to "Mettre à jour (⌘S)",
            "cancel" to "Annuler (Esc)",
            "discard" to "Abandonner",
            "continue_editing" to "Continuer l'édition",
            "discard_changes_title" to "Abandonner les modifications ?",
            "discard_changes_message" to "Vous avez des modifications non sauvegardées. Êtes-vous sûr de vouloir les abandonner ?",
            "keyboard_shortcuts" to "Raccourcis clavier : ⌘S - Sauvegarder, Esc - Annuler",
            "today" to "Aujourd'hui",
            "today_shortcut" to "Aujourd'hui (Alt+T)",
            "set_to_today" to "Définir à aujourd'hui",
            "close" to "Fermer",
            "date_label" to "Date",
            "add_time_entry" to "Ajouter une saisie de temps",
            "edit_time_entry" to "Modifier la saisie de temps",
            "select_project_first" to "Sélectionnez d'abord un projet pour voir ses tickets",
            "new_entry" to "Nouvelle saisie",
            "previous" to "Précédent",
            "next" to "Suivant",
            "nav_previous" to "← Précédent",
            "nav_next" to "Suivant →",
            "nav_help" to "Alt+← Précédent | Alt+→ Suivant",
            "total_hours" to "Total des heures :",
            "error_loading_entries" to "Erreur lors du chargement des saisies de temps : %s",
            "entry_created" to "Saisie de temps créée avec succès",
            "entry_updated" to "Saisie de temps mise à jour avec succès",
            "operation_error" to "L'opération a peut-être réussi, mais il y a eu une erreur : %s",
            "cancel_shortcut" to "Annuler (Esc)",
            "set_to_today" to "Définir à aujourd'hui",
            "dropdown_up" to "▲",
            "dropdown_down" to "▼",
            "clear_button" to "✕",
            "clear_button_description" to "Effacer",
            "invalid_number" to "Veuillez saisir un nombre valide",
            "hours_must_be_positive" to "Les heures doivent être supérieures à 0",
            "no_projects" to "Aucun projet disponible",
            "no_activities" to "Aucune activité disponible",
            "showing_issue" to "Affichage actuel du ticket #%d",
            "hours_format" to "%.1f h",
            "char_count" to "%d/255",
            "total_hours_format" to "%.1f",
            "issue_item_format" to "• #%d %s",
            "comment_item_format" to "• %s",
            "issue_selection_format" to "#%d - %s"
        ),
        Language.ENGLISH to mapOf(
            "window_title" to "Redmine Time Tracking",
            "project_label" to "Project",
            "hours_label" to "Hours",
            "issue_label" to "Issue",
            "activity_label" to "Activity",
            "comments_label" to "Comments",
            "select_issue_placeholder" to "Select an issue",
            "add_entry" to "Add Entry (⌘S)",
            "update_entry" to "Update Entry (⌘S)",
            "cancel" to "Cancel (Esc)",
            "discard" to "Discard",
            "continue_editing" to "Continue Editing",
            "discard_changes_title" to "Discard Changes?",
            "discard_changes_message" to "You have unsaved changes. Are you sure you want to discard them?",
            "keyboard_shortcuts" to "Keyboard shortcuts: ⌘S - Save, Esc - Cancel",
            "today" to "Today",
            "today_shortcut" to "Today (Alt+T)",
            "set_to_today" to "Set to Today",
            "close" to "Close",
            "date_label" to "Date",
            "add_time_entry" to "Add Time Entry",
            "edit_time_entry" to "Edit Time Entry",
            "select_project_first" to "Select a project first to see its issues",
            "new_entry" to "New entry",
            "previous" to "Previous",
            "next" to "Next",
            "nav_previous" to "← Previous",
            "nav_next" to "Next →",
            "nav_help" to "Alt+← Previous | Alt+→ Next",
            "total_hours" to "Total Hours:",
            "error_loading_entries" to "Error loading time entries: %s",
            "entry_created" to "Time entry created successfully",
            "entry_updated" to "Time entry updated successfully",
            "operation_error" to "Operation might have succeeded, but there was an error: %s",
            "cancel_shortcut" to "Cancel (Esc)",
            "set_to_today" to "Set to Today",
            "dropdown_up" to "▲",
            "dropdown_down" to "▼",
            "clear_button" to "✕",
            "clear_button_description" to "Clear",
            "invalid_number" to "Please enter a valid number",
            "hours_must_be_positive" to "Hours must be greater than 0",
            "no_projects" to "No projects available",
            "no_activities" to "No activities available",
            "showing_issue" to "Currently showing issue #%d",
            "hours_format" to "%.1f h",
            "char_count" to "%d/255",
            "total_hours_format" to "%.1f",
            "issue_item_format" to "• #%d %s",
            "comment_item_format" to "• %s",
            "issue_selection_format" to "#%d - %s"
        )
    )

    operator fun get(key: String): String {
        val result = if (currentLanguage == Language.FRENCH) {
            // For French, try French first, then fall back to English
            strings[Language.FRENCH]?.get(key) ?: strings[Language.ENGLISH]?.get(key) ?: key
        } else {
            // For English, try English first, then fall back to French
            strings[Language.ENGLISH]?.get(key) ?: strings[Language.FRENCH]?.get(key) ?: key
        }
        return result
    }
}
