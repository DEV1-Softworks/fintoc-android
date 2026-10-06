# Arquitetura

[English](../en/architecture.md) · [Español](../es/architecture.md) · [Français](../fr/architecture.md) · [Voltar ao README](README.md)

Esta página explica como o projeto está organizado. Você não precisa de experiência prévia em Android para acompanhá-la.

## Visão geral

O repositório é um build do Gradle com dois módulos:

- **`fintoc-sdk`** é uma *biblioteca Android*: código que outros apps incluem como dependência. É o que é publicado no Maven.
- **`app`** é um *aplicativo Android* que usa o SDK como um cliente faria. Também serve como exemplo vivo.

```mermaid
flowchart LR
    subgraph repo["repositório fintoc-android"]
        app["app\n(aplicativo de exemplo)"]
        sdk["fintoc-sdk\n(biblioteca publicada)"]
    end
    app -->|"implementation(project)"| sdk
    host["App do cliente"] -->|"implementation('mx.dev1.fintoc:fintoc-sdk')"| sdk
```

## Arquitetura limpa

Todos os módulos seguem as mesmas três camadas. As dependências apontam apenas **para dentro**, em direção ao domínio.

```mermaid
flowchart TB
    presentation["presentation\ntelas Jetpack Compose, view models"]
    domain["domain\nmodelos, casos de uso, interfaces de repositório\n(Kotlin puro, sem Android)"]
    data["data\ncliente HTTP, DTOs, implementações de repositório"]
    di["di\nmódulos Koin que ligam as camadas"]

    presentation --> domain
    data --> domain
    di -.->|cria| presentation
    di -.->|cria| data
```

| Camada | Conhece | Nunca deve conhecer |
|---|---|---|
| `domain` | Biblioteca padrão do Kotlin, corrotinas | Android, Koin, HTTP, Compose |
| `data` | `domain` | `presentation`, Compose |
| `presentation` | `domain` | `data` |
| `di` | todas as camadas | — |

Hoje existem a camada `domain` (pacotes `domain.widget` e `domain.security`), o pacote `di` e o ponto de entrada
público. As demais camadas são criadas conforme as funcionalidades chegam, sob o pacote base `mx.dev1.fintoc.sdk`.

## API pública e modo de API explícita

O SDK ativa o **modo de API explícita** do Kotlin. Toda declaração é `internal`, a menos que seja marcada como `public`
de propósito, então a superfície pública da biblioteca é sempre intencional. Hoje ela é:

| Tipo | Função |
|---|---|
| `Fintoc` | Ponto de entrada: `initialize`, `shutdown`, `isInitialized`. |
| `FintocConfiguration` | Configurações: `publicKey` (somente `pk_test_` ou `pk_live_`; chaves secretas `sk_` são recusadas) e `environment`, deduzido do prefixo. Seu `toString()` oculta a chave. |
| `FintocEnvironment` | `TEST` ou `LIVE`. |
| `FintocWidgetOptions` | O que o Widget deve fazer: `Payments`, `Movements` ou `Subscriptions`. Cada um valida seus dados e oculta seus tokens no `toString()`. |
| `FintocCountry`, `FintocHolderType` | Valores aceitos para `country` e `holder_type`. |
| `FintocWidgetEvent` | O que o Widget informa: `Succeeded`, `Exited` ou `Occurred`. |
| `FintocWidgetEventType` | Os eventos que a Fintoc documenta, como `OPENED` ou `PAYMENT_ERROR`. |
| `FintocLinkIntentResult` | O `exchangeToken` de uma conta bancária conectada com `Movements`. Seu `toString()` oculta o token. |

## Configuração do Widget

O Widget da Fintoc é uma página web que o SDK exibe em um WebView. Ele lê suas configurações da query string da URL,
então o SDK transforma seu `FintocConfiguration` e seus `FintocWidgetOptions` nessa URL.

```mermaid
flowchart LR
    key["FintocConfiguration\npublicKey"] --> builder["FintocWidgetUrlBuilder"]
    options["FintocWidgetOptions\nPayments | Movements | Subscriptions"] --> builder
    builder --> url["https://webview.fintoc.com/widget.html\n?public_key=…&product=…"]
```

| Opções | Produto | Parâmetros enviados depois de `public_key` e `product` |
|---|---|---|
| `Payments(sessionToken)` | `payments` (SPEI no México, transferência bancária no Chile) | `session_token` |
| `Movements(holderType, country, linkToken?, webhookUrl?)` | `movements` | `holder_type`, `country`, `link_token`, `webhook_url` |
| `Subscriptions(widgetToken, holderType, country)` | `subscriptions` | `holder_type`, `country`, `widget_token` |

Regras de segurança aplicadas ao criar os objetos:

- Valores vazios, com espaços ou que comecem com `sk_` são recusados. As mensagens de erro nunca repetem o valor
  recusado.
- `webhookUrl` deve ser uma URL `https` absoluta.
- Os valores são codificados com porcentagem (RFC 3986), então um token não pode adicionar nem substituir parâmetros.
- `toString()` oculta os tokens.

A URL sempre termina com `_on_event=true`, que pede ao Widget que informe seus eventos ao app.

## Eventos do Widget

O Widget responde navegando a WebView para endereços que começam com `fintocwidget://`. O SDK nunca deixa a WebView
abrir esses endereços: entrega cada um ao `FintocWidgetRedirectParser`, que o transforma em um `FintocWidgetEvent`.

```mermaid
sequenceDiagram
    participant Widget as Página do Widget (na WebView)
    participant View as Cliente da WebView (próxima funcionalidade)
    participant Parser as FintocWidgetRedirectParser
    participant App as Seu app

    Widget->>View: navega para fintocwidget://event/opened?timestamp=…
    View->>Parser: isRedirect(url) e parse(url)
    Parser-->>View: FintocWidgetEvent.Occurred, ou null se inesperado
    View->>App: callback do evento
```

| Redirecionamento do Widget | Evento |
|---|---|
| `fintocwidget://succeeded` | `Succeeded`. Com `Movements` também traz `?object=link_intent&exchange_token=…&id=…`, que vira `linkIntent`. |
| `fintocwidget://exit` | `Exited`: o usuário fechou o Widget sem concluir. |
| `fintocwidget://event/{name}?timestamp=…` | `Occurred(name, timestampMillis, metadata)`. `type` é o `FintocWidgetEventType` correspondente, ou `null` quando a Fintoc adicionou um evento que este SDK ainda não conhece. |

Regras de segurança do interpretador:

- Ele trabalha sobre o texto bruto e nunca lança exceções. Um esquema incorreto, uma ação desconhecida ou um nome de
  evento estranho resultam em `null`, então a página nunca pode derrubar seu app.
- Os nomes de evento só podem usar letras, dígitos, `_`, `.` e `-`, com até 64 caracteres. Redirecionamentos com mais
  de 8.192 caracteres e parâmetros a partir do 65º são ignorados.
- O Widget não codifica o que envia, então a decodificação é tolerante: só as sequências `%XX` bem formadas são
  decodificadas.
- A primeira ocorrência de uma chave vence. Um `&` solto dentro de um valor não pode substituir um `exchange_token`
  anterior.
- Valores que o Widget escreveu como `null`, `undefined` ou `[object Object]` são descartados.
- `FintocLinkIntentResult.toString()` oculta o `exchangeToken`, e `Occurred.toString()` lista as chaves dos metadados,
  mas não os valores.

> **Um evento não é prova de pagamento.** Um usuário, ou um dispositivo comprometido, pode falsificar o que uma WebView
> informa. Envie o `exchangeToken` ao **seu backend**, o único lugar que pode trocá-lo, e confirme os pagamentos com os
> webhooks da Fintoc antes de entregar um pedido.

## Injeção de dependências com um contêiner Koin isolado

O Koin é o framework de injeção de dependências. O SDK cria o **seu próprio** contêiner em vez de usar o global do
Koin. Assim, ele nunca colide com uma instância do Koin iniciada pelo app hospedeiro.

```mermaid
sequenceDiagram
    participant Host as App hospedeiro
    participant Fintoc as Fintoc (object)
    participant Container as FintocKoinContainer
    participant Koin as KoinApplication isolada

    Host->>Fintoc: initialize(context, configuration)
    Fintoc->>Container: fecha o contêiner anterior, se houver
    Fintoc->>Container: FintocKoinContainer(context, configuration)
    Container->>Koin: koinApplication { modules(coreModule) }
    Note over Koin: Registra Context (contexto da aplicação)<br/>e FintocConfiguration
    Host->>Fintoc: shutdown()
    Fintoc->>Container: close()
```

O código interno obtém suas dependências com `Fintoc.requireKoin()`. Se o app hospedeiro esquecer de chamar `initialize`,
a chamada falha imediatamente com uma mensagem explicando o que fazer.

## Decisões de build

| Decisão | Valor | Motivo |
|---|---|---|
| Kotlin | 2.4.20 | Versão estável atual. O Android Gradle Plugin 9 traz suporte a Kotlin integrado, então nenhum plugin de Kotlin separado é aplicado. |
| Android Gradle Plugin | 9.4.1 (exige Gradle 9.6.0) | Versão estável atual. |
| `minSdk` | 23 (Android 6.0) | O nível mais baixo que o Jetpack Compose suporta, para alcançar o maior número de dispositivos. Não chame APIs posteriores à API 23 (como `java.time`) sem uma guarda ou desugaring. |
| `compileSdk` | 37 | Exigido pelo Compose BOM 2026.09.00. |
| `targetSdk` (app de exemplo) | 36 | O nível atualmente exigido pelo Google Play. |
| Jetpack Compose | BOM 2026.09.00 | UI com Compose como primeira opção. O XML só é usado onde a plataforma exige (manifest, tema da janela). |
| Bytecode Java | 11 | Permite que o maior número possível de apps hospedeiros consuma a biblioteca. |
| Versões | `gradle/libs.versions.toml` | Um único lugar para todas as versões de dependências. |

## Testes e cobertura

Existem três tipos de testes, e o relatório de cobertura combina todos eles:

| Tipo | Local | Ferramentas | Executa em |
|---|---|---|---|
| Unitários | `src/test` | JUnit 4, Mockito, Robolectric | Seu computador |
| Instrumentados | `src/androidTest` | AndroidX Test, Espresso, testes de UI do Compose | Dispositivo ou emulador |
| Cobertura | `gradle/jacoco-coverage.gradle.kts` | JaCoCo | Combina ambos |

O build falha quando a cobertura de linhas ou de instruções de um módulo cai abaixo de **80%**. Veja
[Como contribuir](contributing.md) para os comandos exatos.
