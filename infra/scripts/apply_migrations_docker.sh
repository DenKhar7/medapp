#!/bin/bash
set -e  # Arrêter en cas d'erreur

# Configuration
DATABASES_TABLES=("med_db_active" "med_db_temp")
DATABASES_VIEWS=("med_db_views")
MIGRATIONS_DIR="/migrations"

# Les migrations de schéma sont une tâche d'administration : compte root du conteneur MariaDB (variable
# MARIADB_ROOT_PASSWORD déjà présente dans le conteneur). Le mot de passe est passé via MYSQL_PWD pour ne
# pas apparaître dans la liste des processus.
DB_USER="root"
DB_PASSWORD="${MARIADB_ROOT_PASSWORD}"

export MYSQL_PWD="$DB_PASSWORD"

echo "Démarrage des migrations de schéma"


# Fonction : Déterminer le scope d'une migration
get_migration_scope() {
    local filename=$1
    
    # Extraire la partie après V001__
    local scope_part=$(echo "$filename" | sed 's/^V[0-9]*__//' | cut -d'_' -f1)
    
    case "$scope_part" in
        tables|data|index)
            echo "tables"
            ;;
        views)
            echo "views"
            ;;
        *)
            # Par défaut, considérer comme migration de tables
            echo "tables"
            ;;
    esac
}

# Fonction : Obtenir les bases cibles selon le scope
get_target_databases() {
    local scope=$1
    
    if [ "$scope" = "views" ]; then
        echo "${DATABASES_VIEWS[@]}"
    else
        echo "${DATABASES_TABLES[@]}"
    fi
}


# Fonction : Vérifier si une migration est déjà appliquée
is_migration_applied() {
    local version=$1
    local db_name=$2

    count=$(mariadb -u "$DB_USER" "$db_name" -sN -e \
        "SELECT COUNT(*) FROM schema_migrations WHERE version = '$version';")

    if [ "$count" -gt 0 ]; then
        return 0  # Déjà appliquée
    else
        return 1  # Pas encore appliquée
    fi
}

# Fonction : Appliquer une migration sur une DB
apply_migration() {
    local file=$1
    local version=$2
    local description=$3
    local db_name=$4

    echo "Application de $version sur $db_name..."

    # Calculer le checksum
    checksum=$(sha256sum "$file" | cut -d' ' -f1)

    # Appliquer la migration
    mariadb -u "$DB_USER" "$db_name" < "$file"

    # Enregistrer dans schema_migrations
    mariadb -u "$DB_USER" "$db_name" -e \
        "INSERT INTO schema_migrations (version, description, checksum)
         VALUES ('$version', '$description', '$checksum');"

    echo "$version appliquée avec succès"
}

# PHASE 2 : Application des migrations
echo "Application des migrations"
echo ""

# Vérifier que le dossier migrations existe et contient des fichiers
if [ ! -d "$MIGRATIONS_DIR" ]; then
    echo "Le dossier $MIGRATIONS_DIR n'existe pas"
    exit 1
fi

migration_count=$(ls $MIGRATIONS_DIR/V*.sql 2>/dev/null | wc -l)
if [ "$migration_count" -eq 0 ]; then
    echo "Aucune migration à appliquer (aucun fichier V*.sql trouvé)"
    exit 0
fi

# Traiter chaque migration dans l'ordre
for migration_file in $(ls $MIGRATIONS_DIR/V*.sql | sort); do
    filename=$(basename "$migration_file" .sql)
    version=$(echo "$filename" | cut -d'_' -f1)
    description=$(echo "$filename" | cut -d'_' -f3- | tr '_' ' ')
    
    # Déterminer le scope de cette migration
    scope=$(get_migration_scope "$filename")
    target_dbs=$(get_target_databases "$scope")
    
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Migration : $version"
    echo "Description : $description"
    echo "Scope : $scope"
    echo "Cibles : $target_dbs"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    # Appliquer sur chaque base cible
    for DB_NAME in $target_dbs; do
        if is_migration_applied "$version" "$DB_NAME"; then
            echo "$version déjà appliquée sur $DB_NAME, skip"
        else
            apply_migration "$migration_file" "$version" "$description" "$DB_NAME"
        fi
    done
    
    echo ""
done
echo "Toutes les migrations sont à jour"
