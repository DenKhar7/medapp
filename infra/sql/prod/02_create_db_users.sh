#!/bin/bash
# Crée les comptes applicatifs de MariaDB au PREMIER démarrage (volume vide), après les scripts 01_*.sql.
#
#  - MARIADB_USER  : compte des jobs d'import. Peut créer/modifier/supprimer des tables dans
#                    med_db_active, med_db_temp et med_db_archive, mais n'a AUCUN droit sur med_db_views
#                    ni sur les autres bases, et n'a pas ALL PRIVILEGES.
#  - API_DB_USER   : compte de l'API. Lecture seule (SELECT) sur med_db_views uniquement.
#
# Le mot de passe root n'est utilisé que dans ce script et n'est jamais transmis au conteneur du serveur.
# NB : ce fichier est "sourcé" par l'entrypoint officiel : ne pas y activer `set -e` / `set -u`.

for var in MARIADB_ROOT_PASSWORD MARIADB_USER MARIADB_PASSWORD API_DB_USER API_DB_PASSWORD; do
    if [ -z "${!var}" ]; then
        echo "02_create_db_users: la variable $var est obligatoire" >&2
        exit 1
    fi
done

# Les valeurs sont insérées dans du SQL : on les restreint à des caractères sûrs.
for var in MARIADB_USER MARIADB_PASSWORD API_DB_USER API_DB_PASSWORD; do
    if ! [[ "${!var}" =~ ^[A-Za-z0-9_.@+-]+$ ]]; then
        echo "02_create_db_users: $var ne doit contenir que des lettres, chiffres et _ . @ + -" >&2
        exit 1
    fi
done

mariadb --protocol=socket -uroot -p"${MARIADB_ROOT_PASSWORD}" <<EOSQL
CREATE USER IF NOT EXISTS '${MARIADB_USER}'@'%' IDENTIFIED BY '${MARIADB_PASSWORD}';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON med_db_active.*  TO '${MARIADB_USER}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON med_db_temp.*    TO '${MARIADB_USER}'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, DROP, ALTER, INDEX, REFERENCES ON med_db_archive.* TO '${MARIADB_USER}'@'%';

CREATE USER IF NOT EXISTS '${API_DB_USER}'@'%' IDENTIFIED BY '${API_DB_PASSWORD}';
GRANT SELECT ON med_db_views.* TO '${API_DB_USER}'@'%';

FLUSH PRIVILEGES;
EOSQL
