# Registro de cambios

[English](../en/changelog.md) · [Français](../fr/changelog.md) · [Português](../pt/changelog.md) · [Volver al README](../../README.md)

Aquí se listan todos los cambios notables, del más reciente al más antiguo. El formato sigue
[Keep a Changelog](https://keepachangelog.com), y el proyecto sigue el [Versionado Semántico](https://semver.org). Un
cambio que rompa la API pública sube la versión mayor. Solo es pública lo que lista la documentación de la API.

## Unreleased

La primera versión será la `1.0.0`. Contiene:

### Agregado

- **Configuración del Widget.** `FintocConfiguration`, que solo acepta llaves públicas (`pk_test_` y `pk_live_`) y rechaza las
  secretas, y `FintocWidgetOptions` para `Payments`, `Movements` y `Subscriptions`, con un constructor de URL que codifica
  con porcentaje cada valor.
- **Eventos del Widget.** `FintocWidgetEvent` (`Succeeded`, `Exited`, `Occurred`), `FintocWidgetEventType` y
  `FintocLinkIntentResult`, leídos de las redirecciones `fintocwidget://` del Widget por un intérprete que nunca lanza
  excepciones.
- **`FintocWidget` para Compose**, con una sobrecarga que recibe las opciones y otra que pide el session token a un
  proveedor `suspend`. El WebView está endurecido, solo los hosts de Fintoc se quedan dentro, los demás enlaces `https` se
  abren en el navegador, y una carga fallida muestra el mensaje propio del SDK con un reintento en lugar de la dirección
  de la página.
- **Un host con Activity para apps sin Compose.** `FintocWidgetContract` y `FintocWidgetResult`. El session token nunca
  viaja en el `Intent`, la pantalla se oculta de las capturas, y sobrevive a una rotación.
- **`FintocHostedCheckout`**, que abre un checkout alojado por Fintoc en una Custom Tab e indica si la dirección que volvió
  es tu dirección de éxito, tu dirección de cancelación o ninguna. Los valores secretos de tus propias direcciones deben
  volver.
- **Cuatro idiomas** para los textos del SDK (español, inglés, francés y portugués), siguiendo el dispositivo o forzados con
  `FintocConfiguration.language`.
- **Pruebas de accesibilidad** con el Accessibility Test Framework de Google, con los tamaños de fuente y de pantalla por
  defecto y los más grandes.
- **Un contenedor Koin aislado** que es dueño de lo que tiene estado, así que `Fintoc.shutdown()` libera todos los session
  tokens.
- **Una app de ejemplo** que usa cada parte del SDK contra el sandbox de Fintoc.
- **Publicación en Maven Central**, con fuentes, documentación de la API y firmas.

### Compatibilidad

- Android 6.0 (API 23) y superior. Las apps deben compilar con `compileSdk 37`.
- Kotlin 2.2 o superior. La librería se compila con el nivel de lenguaje 2.2 y pide la biblioteca estándar 2.2.
