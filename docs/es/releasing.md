# Publicar una versión

[English](../en/releasing.md) · [Français](../fr/releasing.md) · [Português](../pt/releasing.md) · [Volver al README](../../README.md)

Esta página explica cómo una persona mantenedora publica `mx.dev1.fintoc:fintoc-sdk` en
[Maven Central](https://central.sonatype.com). Nada secreto está en el repositorio: tus credenciales viven en tu propio
`~/.gradle/gradle.properties` o en variables de entorno.

## Configuración inicial (una sola vez)

1. Crea una cuenta en el [Central Portal](https://central.sonatype.com).
2. **Verifica el namespace** `mx.dev1` en el portal (Namespaces). Demuestra, con un registro DNS, que eres dueño del dominio
   `dev1.mx`. Las coordenadas `mx.dev1.fintoc` viven bajo él. Un namespace como `io.github.dev1-softworks` no necesitaría
   dominio, pero cambiaría las coordenadas de la librería, así que decídelo antes de la primera versión.
3. **Genera un user token** en el portal (Account y luego Generate User Token). Es un usuario y una contraseña para la
   subida, no los que usas para iniciar sesión.
4. **Crea una llave GPG** que firme los artefactos, y publica su parte pública en un servidor de llaves que Maven Central
   lea, como `keyserver.ubuntu.com`:

```bash
gpg --full-generate-key
gpg --keyserver keyserver.ubuntu.com --send-keys <id de la llave>
gpg --export-secret-keys --armor <id de la llave>    # imprime la llave privada: no la dejes en logs ni chats
```

5. Dale a Gradle las credenciales, en tu `gradle.properties` de usuario:

```properties
# ~/.gradle/gradle.properties, never in the repository
mavenCentralUsername=<token username>
mavenCentralPassword=<token password>
signingInMemoryKey=<armored secret key, on one line or with \n for the line breaks>
signingInMemoryKeyPassword=<password of the key>
```

En un servidor de CI usa los mismos nombres como variables de entorno con el prefijo `ORG_```kotlin
dependencies {
    implementation("mx.dev1.fintoc:fintoc-sdk:<version>")
}
```_PROJECT_`, por ejemplo
`ORG_```kotlin
dependencies {
    implementation("mx.dev1.fintoc:fintoc-sdk:<version>")
}
```_PROJECT_mavenCentralUsername`.

## Versionado

La versión es `VERSION_NAME` en `gradle.properties`. El proyecto sigue el [Versionado Semántico](https://semver.org). Entre
versiones termina en `-SNAPSHOT`. Maven Central nunca acepta la misma versión dos veces, así que un error se corrige con
una versión nueva. Mientras la versión mayor sea 0, una versión menor puede cambiar la API pública: el
[registro de cambios](changelog.md) lo indica cuando ocurre.

## Pasos de la publicación

Siguen el Git flow de este proyecto, donde `master` es producción.

1. Crea la rama `release/<versión>` desde `develop`.
2. Pon `VERSION_NAME` en la versión, sin `-SNAPSHOT`, y mueve las notas de `Unreleased` de los cuatro registros de cambios
   bajo la versión nueva.
3. Ejecuta todo, con un teléfono conectado y desbloqueado:
   `./gradlew testDebugUnitTest connectedDebugAndroidTest jacocoDebugCoverageReport jacocoDebugCoverageVerification lintDebug lintRelease assembleRelease`.
4. Haz una [prueba en seco](#prueba-en-seco) y corrige lo que muestre.
5. Abre un pull request de `release/<versión>` a `master`, y fusiónalo.
6. En `master`, etiqueta el commit `v<versión>` y sube la etiqueta.
7. Publica. Esto sube, firma y libera:

```bash
./gradlew :fintoc-sdk:publishAndReleaseToMavenCentral
```

   Para ver la subida en el portal antes de que salga, ejecuta `publishToMavenCentral` y pulsa Publish allí.
8. Fusiona `master` de vuelta en `develop` con un pull request que ponga `VERSION_NAME` en el siguiente `-SNAPSHOT`.
9. Crea un release de GitHub desde la etiqueta, con las notas del registro de cambios.

## Qué se publica

| Archivo | Qué es |
|---|---|
| `fintoc-sdk-<versión>.aar` | La librería. |
| `fintoc-sdk-<versión>.pom` y `.module` | Metadatos para Maven y para Gradle. El POM nombra a los desarrolladores, la licencia y el repositorio, que Central exige, y la descripción dice que el SDK no es oficial. |
| `fintoc-sdk-<versión>-sources.jar` | Las fuentes en Kotlin. |
| `fintoc-sdk-<versión>-javadoc.jar` | La documentación de la API, generada por Dokka a partir del KDoc. Solo aparece la API pública. |
| `*.asc` | Una firma GPG de cada archivo anterior. |

## Prueba en seco

Publicar en una carpeta local no necesita credenciales y no firma nada, así que es seguro hacerlo en cualquier momento.
Muestra exactamente qué se subiría:

```bash
./gradlew :fintoc-sdk:publishToMavenLocal -Dmaven.repo.local=/tmp/fintoc-m2
```

Después comprueba lo que ve un consumidor: crea una app vacía con `compileSdk 37`, apunta sus repositorios a esa carpeta y
agrega la librería. Vale la pena hacer tres comprobaciones:

- **Compila y construye con R8**, usando la API pública que cambiaste.
- **Su Kotlin no se fuerza hacia arriba.** Mira la biblioteca estándar de Kotlin que Gradle resuelve para la app. Debe ser la
  de la app, no la de este build.
- **El manifiesto se fusiona.** El manifiesto fusionado de la app tiene el permiso `INTERNET` y la Activity del SDK.

Para comprobar las firmas con una llave desechable, define `signingInMemoryKey` con una llave de prueba en el mismo comando,
y verifica cada `.asc` con `gpg --verify`.

## Si el portal rechaza la subida

El portal explica cada fallo. Los habituales son un namespace sin verificar, una llave pública que aún no tiene ningún
servidor de llaves, una firma, un jar de javadoc o de fuentes que falta, un POM sin desarrolladores o sin licencia, y una
versión que ya existe. Corrige la causa, cambia la versión si se consumió una, y publica de nuevo.
