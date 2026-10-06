# Registro de mudanças

[English](../en/changelog.md) · [Español](../es/changelog.md) · [Français](../fr/changelog.md) · [Voltar ao README](../../README.md)

Todas as mudanças notáveis estão listadas aqui, da mais recente para a mais antiga. O formato segue o
[Keep a Changelog](https://keepachangelog.com), e o projeto segue o [Versionamento Semântico](https://semver.org). Uma
mudança que quebre a API pública sobe a versão maior. Só é pública o que a documentação da API lista.

## Unreleased

A primeira versão será a `1.0.0`. Ela contém:

### Adicionado

- **Configuração do Widget.** `FintocConfiguration`, que só aceita chaves públicas (`pk_test_` e `pk_live_`) e recusa as
  secretas, e `FintocWidgetOptions` para `Payments`, `Movements` e `Subscriptions`, com um construtor de URL que codifica
  com porcentagem cada valor.
- **Eventos do Widget.** `FintocWidgetEvent` (`Succeeded`, `Exited`, `Occurred`), `FintocWidgetEventType` e
  `FintocLinkIntentResult`, lidos dos redirecionamentos `fintocwidget://` do Widget por um interpretador que nunca lança
  exceções.
- **`FintocWidget` para Compose**, com uma sobrecarga que recebe as opções e outra que pede o session token a um provedor
  `suspend`. A WebView é reforçada, só os hosts da Fintoc ficam dentro dela, os demais links `https` abrem no navegador, e
  um carregamento que falha mostra a mensagem própria do SDK com uma nova tentativa, em vez do endereço da página.
- **Um host com Activity para apps sem Compose.** `FintocWidgetContract` e `FintocWidgetResult`. O session token nunca
  viaja no `Intent`, a tela se oculta das capturas de tela, e sobrevive a uma rotação.
- **`FintocHostedCheckout`**, que abre um checkout hospedado pela Fintoc em uma Custom Tab e diz se o endereço que voltou é
  o seu endereço de sucesso, o de cancelamento ou nenhum. Os valores secretos dos seus próprios endereços devem voltar.
- **Quatro idiomas** para os textos do SDK (português, inglês, espanhol e francês), seguindo o dispositivo ou forçados com
  `FintocConfiguration.language`.
- **Verificações de acessibilidade** com o Accessibility Test Framework do Google, nos tamanhos de fonte e de tela padrão e
  nos maiores.
- **Um contêiner Koin isolado** que é dono do que tem estado, então `Fintoc.shutdown()` libera todos os session tokens.
- **Um app de exemplo** que usa cada parte do SDK contra o sandbox da Fintoc.
- **Publicação no Maven Central**, com fontes, documentação da API e assinaturas.

### Compatibilidade

- Android 6.0 (API 23) e superior. Os apps devem compilar com `compileSdk 37`.
- Kotlin 2.2 ou superior. A biblioteca é compilada no nível de linguagem 2.2 e pede a biblioteca padrão 2.2.
