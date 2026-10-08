# MedicApp

Application mobile Android de gestion des médicaments - suivi quotidien des prises, gestion des traitements et consultation des informations médicamenteuses.

![Android SDK](https://img.shields.io/badge/Android%20SDK-31%2B-green)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-purple)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-blue)
![Room](https://img.shields.io/badge/Room-2.8.3-orange)

---

## Table des Matières

- [Fonctionnalités](#fonctionnalités)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Guide d'Utilisation](#guide-dutilisation)
- [Architecture Technique](#architecture-technique)
- [Technologies Utilisées](#technologies-utilisées)
- [Structure du Projet](#structure-du-projet)
- [Configuration](#configuration)
- [Tests](#tests)
- [Licence](#licence)

---

## Fonctionnalités

### Authentification Sécurisée
- **Connexion automatique** : Persistance de session via EncryptedSharedPreferences
- **Inscription sécurisée** : Hashage des mots de passe avec PBKDF2-HMAC-SHA256 (600 000 itérations + sel). Les anciennes empreintes (SHA-1) sont recalculées automatiquement à la prochaine connexion réussie
- **Gestion multi-profils** : Création et gestion de profils familiaux

### Tableau de Bord (Home)
- **Checklist quotidienne** : Liste des médicaments à prendre aujourd'hui avec cases à cocher
- **Bouton d'urgence** : Appel du 112 ou redirection vers le signalement ANSM
- **Synthèse visuelle** : Vue d'ensemble de vos traitements actifs

### Calendrier Interactif
- **Navigation mensuelle** : Flèches gauche/droite pour changer de mois
- **Indicateurs visuels par jour** :
    - Bleu : tous les médicaments pris
    - Gris : médicaments manqués
- **Détail par jour** : Cliquez sur un jour pour voir les médicaments prévus et l'historique des prises

### Gestion des Traitements
- **Armoire à pharmacie** : Liste de tous vos traitements actifs
- **Ajout/modification** : Configuration complète (dates, doses, récurrence)
- **Rappels multiples** : Plusieurs horaires par jour pour un même médicament
- **Récurrence flexible** : Quotidien, jours spécifiques de la semaine

### Recherche de Médicaments
- **API temps réel** : Recherche dans la base de données officielle
- **Cache local** : Stockage des médicaments consultés pour un accès hors-ligne
- **Informations détaillées** : Composition, posologie, contre-indications

### Gestion des Ordonnances
- **Saisie manuelle** : Création d'ordonnances avec liste de médicaments
- **Scan OCR** : Reconnaissance optique de caractères via ML Kit
- **Scanner de documents** : Numérisation d'ordonnances papier

### Système de Rappels
- **Notifications programmées** : Alertes aux heures définies
- **Actions rapides** : Boutons "Pris" et "Reporter" (15 min) directement dans la notification
- **Persistance** : Reprogrammation automatique après redémarrage du téléphone

### Multi-Profils
- **Gestion familiale** : Gérez les médicaments de toute la famille
- **Sélecteur de profil** : Changement rapide entre les profils
- **Données isolées** : Chaque profil a ses propres traitements et rappels

### Internationalisation
- **Français** : Langue par défaut
- **Anglais** : Support complet

---

## Prérequis

- **Android Studio** : Arctic Fox (2020.3.1) ou supérieur
- **JDK** : Java 17
- **Android SDK** : API 31 (Android 12) minimum, API 35 cible
- **Appareil/Émulateur** : Android 12 ou supérieur

---

## Installation

### 1. Cloner le dépôt

```bash
git clone <url-du-repo>
cd AppMedicaleMobile
```

### 2. Configurer l'URL du serveur

Créez ou modifiez le fichier `local.properties` à la racine du projet :

```properties
sdk.dir=/chemin/vers/android/sdk
BASE_URL=http://votre-serveur-api:port
```

### 3. Compiler l'application

```bash
# Build complet
./gradlew build

# APK debug uniquement
./gradlew assembleDebug

# Installer sur un appareil connecté
./gradlew installDebug
```

### 4. Lancer les tests

```bash
# Tests unitaires
./gradlew test

# Tests intégrations (nécessite un émulateur/appareil)
./gradlew connectedAndroidTest
```

---

## Guide d'Utilisation

### Première Connexion

1. **Écran de démarrage (Splash)** : L'application vérifie si une session existe
2. **Inscription** : Si c'est votre première utilisation, créez un compte avec nom complet, email et mot de passe 
3. Pour le mot de passe (Une majuscule, un chiffre, un caractère spécial, une minuscule, 8 caractères minimum)
3. **Connexion** : Saisissez vos identifiants pour accéder à l'application
4. **Accueil** : Vous arrivez sur le tableau de bord principal

### Navigation

L'application dispose d'une **barre de navigation** en bas de l'écran avec 5 onglets :

| Icône                | Section     | Description                               |
|----------------------|-------------|-------------------------------------------|
| Accueil              | Home        | Tableau de bord et checklist quotidienne  |
| Pilule               | Traitement  | Consultations des traitements             |
| Loupe                | Recherche   | Recherche et consultation des médicaments |
| Boîte de médicaments | Ordonnances | Gestion des ordonnances                   |
| Virus                | Pathologies | Future fonctionnalitée                    |

### Utiliser le Calendrier

1. Depuis l'**Accueil**, visualisez le calendrier du mois en cours
2. Utilisez les **flèches** (< >) pour naviguer entre les mois
3. Les **points colorés** indiquent l'état des prises :
   - **Bleu** : Tous les médicaments ont été pris
   - **Gris** : Certains médicaments manquent
4. **Cliquez sur un jour** pour voir le détail des médicaments et l'historique

### Ajouter un Traitement

1. Accédez à la section **Recherche**
2. **Recherchez** le médicament souhaité dans la barre de recherche
3. **Sélectionnez** le médicament dans les résultats
4. Configurez le traitement :
   - **Dates** : Date de début et fin du traitement
   - **Récurrence** : Tous les jours ou jours spécifiques
   - **Horaires** : Une ou plusieurs heures de prise
   - **Doses** : Quantité par prise
5. **Validez** pour ajouter le traitement

### Gérer les Ordonnances

#### Saisie Manuelle
1. Accédez à **Ordonnances** > **Nouvelle ordonnance**
2. Remplissez les informations (médecin, date, etc.)
3. Ajoutez les médicaments un par un
4. Enregistrez l'ordonnance

#### Scan OCR
1. Accédez à **Ordonnances** > **Scanner**
2. Prenez une photo de l'ordonnance ou sélectionnez depuis la galerie
3. L'**OCR ML Kit** extrait automatiquement le texte
4. Vérifiez et corrigez les informations détectées
5. Validez pour créer l'ordonnance

### Gérer les Profils

1. Cliquer sur son **Profil** sélectionné dans la page **Home**
2. Pour **ajouter un profil** :
   - Cliquez sur "Ajouter un profil"
   - Renseignez son rôle (enfant, parent, ...) et son nom complet
   - Validez
3. Pour **changer de profil** :
   - Utilisez le sélecteur de profil en haut de l'écran
   - Les données affichées s'adaptent au profil sélectionné

### Page profil (paramètres)

- **Notifications** : Activer/désactiver les rappels
- **Langue** : Basculer entre Français et Anglais
- **Déconnexion** : Se déconnecter de l'application

---

## Architecture Technique

### Pattern MVVM

L'application suit le pattern **Model-View-ViewModel** :

### Injection de Dépendances

L'application utilise une **injection manuelle** via `ViewModelFactory` :

```kotlin
class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Création du database, DAOs, repositories
        // Instanciation du ViewModel approprié
    }
}

// Utilisation dans les Activities
private val viewModel: HomeViewModel by viewModels { ViewModelFactory(this) }
```

### Flux Réactif

- **StateFlow** : État de l'UI dans les ViewModels
- **Flow** : Requêtes réactives depuis Room (DAOs)
- **combine()** : Fusion de flux (ex: rappels actifs + historique)

```kotlin
// Observation dans les Activities
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            // Mise à jour de l'UI
        }
    }
}
```

### Système de Rappels

```
┌──────────────────┐
│  ReminderManager │──────► AlarmManager (Exact/Inexact)
└────────┬─────────┘
         │
         ▼
┌──────────────────────────┐
│ ReminderBroadcastReceiver│──────► Affiche Notification
└────────┬─────────────────┘
         │
         ▼
┌───────────────────────────┐
│ NotificationActionReceiver│
│  - Action "Pris"          │──────► Enregistre dans TreatmentHistory
│  - Action "Reporter"      │──────► Reprogramme +15 min
└───────────────────────────┘
         │
         ▼
┌──────────────────┐
│   BootReceiver   │──────► Reprogramme alarmes au démarrage
└──────────────────┘
```

---

## Technologies Utilisées

### Langage et SDK

| Technologie | Version | Description |
|-------------|---------|-------------|
| Kotlin | 2.2.21 | Langage principal |
| Android SDK | 31-36 | Min SDK 31 (Android 12), Target SDK 35 |
| Java | 17 | Compatibilité JVM |

### Persistance des Données

| Technologie | Version | Description |
|-------------|---------|-------------|
| Room | 2.8.3 | ORM pour SQLite avec KSP |
| EncryptedSharedPreferences | 1.1.0-alpha06 | Stockage sécurisé de session |
| GSON | 2.11.0 | Sérialisation JSON |

### Communication Réseau

| Technologie | Version | Description |
|-------------|---------|-------------|
| Ktor Client | 3.1.3 | Client HTTP asynchrone |
| OkHttp | (via Ktor) | Moteur HTTP |
| Content Negotiation | 3.1.3 | Parsing JSON automatique |

### Interface Utilisateur

| Technologie | Version | Description |
|-------------|---------|-------------|
| Material Design 3 | 1.13.0 | Composants Material |
| ConstraintLayout | 2.2.1 | Layouts flexibles |
| RecyclerView | 1.4.0 | Listes performantes |
| CardView | 1.0.0 | Cartes Material |

### Traitements en Arrière-plan

| Technologie | Version | Description |
|-------------|---------|-------------|
| WorkManager | 2.11.0 | Tâches en arrière-plan |
| AlarmManager | (Android) | Alarmes précises |

### Machine Learning

| Technologie | Version | Description |
|-------------|---------|-------------|
| ML Kit Text Recognition | 16.0.1 | OCR pour ordonnances |
| Document Scanner | 16.0.0 | Numérisation documents |

### Tests

| Technologie | Version | Description |
|-------------|---------|-------------|
| JUnit 4 | 4.13.2 | Tests unitaires |
| JUnit 5 | 5.11.3 | Tests unitaires avancés |
| Espresso | 3.7.0 | Tests UI instrumentés |
| MockK | 1.13.13 | Mocking pour Kotlin |
| Turbine | 1.2.0 | Tests de Flow |
| Robolectric | 4.14.1 | Tests Android sans émulateur |
| Coroutines Test | 1.9.0 | Tests de coroutines |

---

## Structure du Projet

```
app/src/main/java/iut/butinfo3/app_mobile/
│
├── model/                          # Couche données
│   ├── entity/                     # Entités Room (@Entity)
│   │   ├── User.kt                 # Utilisateur principal
│   │   ├── UserTreatment.kt        # Traitement utilisateur
│   │   ├── TreatmentReminder.kt    # Rappel de prise
│   │   ├── TreatmentHistory.kt     # Historique des prises
│   │   ├── MedicamentSpeResume.kt  # Spécialité (cache local)
│   │   ├── MedicamentPresDetail.kt # Présentation (cache local)
│   │   └── ...
│   │
│   ├── dao/                        # DAOs Room (Flow<T>)
│   │   ├── UserDao.kt
│   │   ├── TreatmentDao.kt
│   │   └── MedicamentDao.kt
│   │
│   ├── database/                   # Base de données
│   │   └── AppDatabase.kt          # Singleton Room (12 entités, 3 DAOs)
│   │
│   ├── repository/                 # Repositories
│   │   ├── AuthRepository.kt       # Authentification
│   │   ├── UserRepository.kt       # Gestion utilisateurs
│   │   ├── TreatmentRepository.kt  # Gestion traitements
│   │   ├── MedicamentRepository.kt # Médicaments (API + db local)
│   │   └── SessionRepository.kt    # Session active (singleton)
│   │
│   └── api/                        # API distante
│       └── MedicamentApiClient.kt  # Client Ktor
│
├── view/                           # Couche présentation
│   ├── SplashActivity.kt           # Écran de démarrage
│   ├── ConnectionActivity.kt       # Connexion (LAUNCHER)
│   ├── RegisterActivity.kt         # Inscription
│   ├── HomeActivity.kt             # Tableau de bord principal
│   ├── MedicamentActivity.kt       # Recherche médicaments
│   ├── SelectMedicamentActivity.kt # Sélection médicament
│   ├── AddTreatmentActivity.kt     # Ajout traitement
│   ├── UserTreatmentsActivity.kt   # Armoire à pharmacie
│   ├── OrdonnanceActivity.kt       # Liste ordonnances
│   ├── CreateOrdonnanceActivity.kt # Création ordonnance
│   ├── PathologieActivity.kt       # Pathologies
│   ├── AccountActivity.kt          # Paramètres compte
│   ├── AddProfileActivity.kt       # Ajout profil
│   ├── EditProfileActivity.kt      # Modification profil
│   │
│   └── adapter/                    # Adapters RecyclerView
│       ├── CalendarAdapter.kt      # Grille calendrier
│       ├── MedicamentAdapter.kt    # Liste médicaments
│       ├── TreatmentAdapter.kt     # Liste traitements
│       └── ...
│
├── view_model/                     # ViewModels (15 total)
│   ├── SplashViewModel.kt
│   ├── ConnectionViewModel.kt
│   ├── RegisterViewModel.kt
│   ├── HomeViewModel.kt
│   ├── MedicamentViewModel.kt
│   ├── SelectMedicamentViewModel.kt
│   ├── AddTreatmentViewModel.kt
│   ├── UserTreatmentViewModel.kt
│   ├── OrdonnanceViewModel.kt
│   ├── OrdonnanceScreenViewModel.kt
│   ├── AccountViewModel.kt
│   ├── AddProfileViewModel.kt
│   ├── EditProfileViewModel.kt
│   └── MainViewModel.kt
│
├── reminder/                       # Système de notifications
│   ├── ReminderManager.kt          # Gestion AlarmManager
│   ├── ReminderBroadcastReceiver.kt# Réception alarmes
│   ├── NotificationActionReceiver.kt # Actions notification
│   ├── BootReceiver.kt             # Reprogrammation au boot
│   └── DailyReminderWorker.kt      # Tâches WorkManager
│
└── utils/                          # Utilitaires
    ├── ViewModelFactory.kt         # Injection de dépendances
    ├── SecurityUtils.kt            # Hashage mots de passe
    ├── Converters.kt               # Convertisseurs Room
    ├── NavigationHelper.kt         # Navigation entre activités
    ├── RecurrenceUtils.kt          # Calcul récurrence
    └── ActivityUtils.kt            # Extensions d'activité

app/src/main/res/
├── layout/                         # Layouts XML
├── values/                         # Ressources (strings, colors, themes)
├── values-en/                      # Traductions anglaises
├── drawable/                       # Images et icônes
├── xml/
│   └── network_security_config.xml # Configuration réseau
└── ...
```

---

## Configuration

### local.properties

Ce fichier (non versionné) doit contenir :

```properties
sdk.dir=/chemin/vers/android/sdk
BASE_URL=http://10.0.2.2:4000
```

### Permissions (AndroidManifest.xml)

L'application requiert les permissions suivantes :

| Permission | Usage |
|------------|-------|
| `INTERNET` | Communication avec l'API |
| `POST_NOTIFICATIONS` | Rappels de médicaments |
| `SCHEDULE_EXACT_ALARM` | Alarmes précises |
| `RECEIVE_BOOT_COMPLETED` | Reprogrammer alarmes au démarrage |

### Configuration Réseau

Le fichier `res/xml/network_security_config.xml` autorise le trafic HTTP non chiffré uniquement vers le serveur backend :

```xml
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

---

## Tests

### Commandes

```bash
# Tous les tests unitaires
./gradlew test

# Un test spécifique
./gradlew test --tests "iut.butinfo3.app_mobile.ExampleUnitTest"

# Tests instrumentés (nécessite émulateur/appareil)
./gradlew connectedAndroidTest

# Test instrumenté spécifique
./gradlew connectedAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.class=iut.butinfo3.app_mobile.integration.AuthFlowIntegrationTest

# Rapport de couverture (si configuré)
./gradlew testDebugUnitTestCoverage
```

### Outils de Test

| Outil | Usage |
|-------|-------|
| **JUnit 4/5** | Assertions et structure de tests |
| **MockK** | Mocking d'objets Kotlin |
| **Turbine** | Test des Flow et StateFlow |
| **Espresso** | Tests d'interface utilisateur |
| **Robolectric** | Tests Android sans émulateur |
| **Coroutines Test** | Test des coroutines |
| **Room Testing** | Tests de migration base de données |

### Structure des Tests

```
app/src/test/                       # Tests unitaires
app/src/androidTest/                # Tests intégrations
```

---

---

## Sécurité et vie privée

- **Données 100 % locales** : comptes, traitements, ordonnances et profils restent sur l'appareil (Room). Le
  serveur ne fournit que les données de référence des médicaments et ne reçoit aucune donnée personnelle.
- **Aucune sauvegarde cloud ni transfert automatique** (`allowBackup=false` + règles d'extraction) : ce sont des
  données de santé, et la clé de chiffrement des préférences ne peut pas être restaurée sur un autre appareil.
- **Photos d'ordonnances** : stockées dans le stockage interne privé de l'application et supprimées avec
  l'ordonnance.
- **Notifications** : sur l'écran verrouillé, seul un message générique est affiché (ni patient, ni médicament).
- **Réseau** : le trafic HTTP en clair n'est autorisé que vers les hôtes de développement
  (`10.0.2.2`, `localhost`, `127.0.0.1`) ; utilisez HTTPS pour un vrai serveur. Aucun corps de requête n'est
  journalisé dans les versions publiées.
- **Limites connues** : la base Room n'est pas chiffrée au repos et il n'y a pas de verrouillage biométrique de
  l'application ; l'authentification est locale à l'appareil (pas de synchronisation entre appareils).