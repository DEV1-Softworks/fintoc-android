# Journal des modifications

[English](../en/changelog.md) · [Español](../es/changelog.md) · [Português](../pt/changelog.md) · [Retour au README](../../README.md)

Toutes les modifications notables sont listées ici, de la plus récente à la plus ancienne. Le format suit
[Keep a Changelog](https://keepachangelog.com), et le projet suit le [Versionnage sémantique](https://semver.org). Un
changement qui casse l'API publique augmente la version majeure. Seul ce que liste la documentation de l'API est public.

## Unreleased

Rien pour l'instant.

## 1.0.0 - 2026-10-06

La première version. Elle contient :

### Ajouté

- **Configuration du Widget.** `FintocConfiguration`, qui n'accepte que des clés publiques (`pk_test_` et `pk_live_`) et
  refuse les clés secrètes, et `FintocWidgetOptions` pour `Payments`, `Movements` et `Subscriptions`, avec un constructeur
  d'URL qui encode en pourcentage chaque valeur.
- **Événements du Widget.** `FintocWidgetEvent` (`Succeeded`, `Exited`, `Occurred`), `FintocWidgetEventType` et
  `FintocLinkIntentResult`, lus dans les redirections `fintocwidget://` du Widget par un analyseur qui ne lève jamais
  d'exception.
- **`FintocWidget` pour Compose**, avec une surcharge qui prend les options et une autre qui demande le session token à un
  fournisseur `suspend`. La WebView est durcie, seuls les hôtes de Fintoc y restent, les autres liens `https` s'ouvrent
  dans le navigateur, et un chargement échoué affiche le message propre au SDK avec une nouvelle tentative, au lieu de
  l'adresse de la page.
- **Pas de plantage sur un appareil sans WebView.** Quand Android System WebView est absent, désactivé ou en cours de
  mise à jour, le Widget affiche un message qui le dit, dans les quatre langues, avec un bouton pour réessayer, au lieu de
  laisser l'exception d'Android fermer votre application.
- **Un hôte Activity pour les apps sans Compose.** `FintocWidgetContract` et `FintocWidgetResult`. Le session token ne
  circule jamais dans l'`Intent`, l'écran se masque des captures d'écran, et il survit à une rotation.
- **`FintocHostedCheckout`**, qui ouvre un checkout hébergé par Fintoc dans une Custom Tab et indique si l'adresse revenue
  est votre adresse de succès, votre adresse d'annulation ou aucune des deux. Les valeurs secrètes de vos propres adresses
  doivent revenir.
- **Quatre langues** pour les textes du SDK (français, anglais, espagnol et portugais), selon l'appareil ou forcées avec
  `FintocConfiguration.language`.
- **Contrôles d'accessibilité** avec l'Accessibility Test Framework de Google, aux tailles de police et d'affichage par
  défaut et aux plus grandes.
- **Un conteneur Koin isolé** qui possède ce qui a un état : `Fintoc.shutdown()` libère donc tous les session tokens.
- **Une application d'exemple** qui utilise chaque partie du SDK contre le sandbox de Fintoc.
- **Publication sur Maven Central**, avec sources, documentation de l'API et signatures.

### Compatibilité

- Android 6.0 (API 23) et supérieur. Les apps doivent compiler avec `compileSdk 37`.
- Kotlin 2.2 ou supérieur. La bibliothèque est compilée au niveau de langage 2.2 et demande la bibliothèque standard 2.2.
