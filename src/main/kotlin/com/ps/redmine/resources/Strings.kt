package com.ps.redmine.resources

enum class Language {
    ENGLISH,
    FRENCH
}

object Strings {
    private var currentLanguage = run {
        val envLang = System.getenv("REDMINE_LANG")?.lowercase()
        val lang = when (envLang) {
            "en" -> Language.ENGLISH
            else -> Language.FRENCH  // Default to French for any other value or if not set
        }
        lang
    }


    private val strings = mapOf(
        Language.FRENCH to mapOf(
            "configuration_title" to "Configuration",
            "configuration_error" to "Veuillez remplir tous les champs",
            "redmine_uri" to "URL Redmine",
            "username" to "Nom d'utilisateur",
            "password" to "Mot de passe",
            "show_password" to "Afficher",
            "hide_password" to "Masquer",
            "save" to "Enregistrer",
            "settings" to "Paramètres",
            "hours_max_value" to "Les heures ne peuvent pas dépasser 7.5",
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
            "issue_selection_format" to "#%d - %s",
            "time_entry_deleted" to "Saisie de temps supprimée avec succès",
            "dismiss" to "Fermer",
            "configuration_saved" to "Configuration enregistrée avec succès",
            "add_new_time_entry" to "Ajouter une nouvelle saisie de temps",
            "delete_time_entry" to "Supprimer la saisie de temps",
            "full_day" to "Journée complète",
            "loading_issues_for_project" to "Chargement des tickets ouverts pour le projet %s...",
            "loading_issues" to "Chargement des tickets...",
            "no_issues_for_project" to "Aucun ticket ouvert trouvé dans le projet %s",
            "no_issues_available" to "Aucun ticket disponible"
        ),
        Language.ENGLISH to mapOf(
            "configuration_title" to "Configuration",
            "configuration_error" to "Please fill all fields",
            "redmine_uri" to "Redmine URL",
            "username" to "Username",
            "password" to "Password",
            "show_password" to "Show",
            "hide_password" to "Hide",
            "save" to "Save",
            "settings" to "Settings",
            "hours_max_value" to "Hours cannot exceed 7.5",
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
            "issue_selection_format" to "#%d - %s",
            "time_entry_deleted" to "Time entry deleted successfully",
            "dismiss" to "Dismiss",
            "configuration_saved" to "Configuration saved successfully",
            "add_new_time_entry" to "Add new time entry",
            "delete_time_entry" to "Delete time entry",
            "full_day" to "Full Day",
            "loading_issues_for_project" to "Loading open issues for project %s...",
            "loading_issues" to "Loading issues...",
            "no_issues_for_project" to "No open issues found in project %s",
            "no_issues_available" to "No issues available"
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
