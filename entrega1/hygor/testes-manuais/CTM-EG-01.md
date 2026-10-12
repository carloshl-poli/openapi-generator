# Caso de teste manual CTM-EG-01

**Sistema:** OpenAPI Generator v7.25.0 (fork). Commit `ef964b04480889ef86b56cfae84ade8ad4c91c41`.
**Funcionalidade:** geração de documentação HTML com exemplos de resposta: `openapi-generator-cli generate -g html -i <arquivo> -o <saída>`. Para cada operação, a página gerada tem a seção *Example data* com um exemplo do corpo da resposta.
**Unidade relacionada:** `org.openapitools.codegen.examples.ExampleGenerator`, chamada por `DefaultCodegen.fromOperation` (via `generateFromResponseSchema`) para preencher `op.examples`, que o template `htmlDocs/index.mustache` exibe.

**Objetivo:** verificar, no nível do sistema, se os exemplos exibidos na documentação:
- preservam os `example` declarados na especificação;
- terminam diante de autorreferência entre modelos;
- são instâncias válidas do schema quando o valor é gerado (`maximum`, `exclusiveMinimum`, `minItems`, `type: object`).

Os resultados esperados vêm das regras R04, R07, R09 e R12 da planilha de oráculos e da especificação OpenAPI 3.0.3 (Schema Object, `example`: *"A free-form property to include an example of an instance for this schema"*; validação de `maximum` (5.2), `minimum`/`exclusiveMinimum` (5.4/5.5), `minItems` (5.11) e `type` (5.21) segundo o JSON Schema Validation wright-00, que a OAS 3.0.3 adota).

---

## Informações sobre a execução do teste

| Campo | Valor |
|---|---|
| Nome do testador | Hygor |
| Data(s) do teste | 12/10/2026 |
| Local / máquina em uso | Máquina local, macOS 26 (arm64), terminal |
| Pré-requisitos | JDK instalado (testado com OpenJDK 22.0.2); fork clonado na baseline v7.25.0; arquivos de entrada da pasta `entradas/` (ao lado deste roteiro) |
| Versões de software | OpenAPI Generator CLI 7.25.0; Java OpenJDK 22.0.2; navegador para abrir o HTML |
| Configuração necessária | Nenhuma opção adicional (`skipOperationExample` no padrão, `false`) |

### Preparação (executar uma vez antes dos passos)

1. Gerar o JAR da CLI a partir da raiz do fork:
   ```
   ./mvnw -pl modules/openapi-generator-cli -am package -DskipTests
   ```
2. Entrar na pasta `entrega1/hygor/testes-manuais/entradas/` e definir o caminho do JAR:
   ```
   J=../../../../modules/openapi-generator-cli/target/openapi-generator-cli.jar
   ```

### Arquivos de entrada (pasta `entradas/`)

| Arquivo | Conteúdo relevante |
|---|---|
| `spec-exemplos.yaml` | `GET /pets/{id}` → `Pet` (examples declarados e propriedade `parent` que referencia o próprio `Pet`); `GET /status` → objeto inline com `example` declarado; `GET /temperature` → `Reading` com `celsius` (`maximum: -5`), `level` (`minimum: 0`, `exclusiveMinimum: true`), `samples` (array com `minItems: 3`) e `metadata` (`type: object` sem propriedades) |
| `spec-objeto-livre.yaml` | `GET /health` → `type: object` sem `properties`, com `example: {status: UP, uptime: 42}` |

---

## Etapas / resultados do script de teste

| Passo | Etapa / entrada do teste | Resultados esperados | Resultados reais | Requisitos validados | Passou / Falhou |
|---|---|---|---|---|---|
| 1 | `java -jar $J generate -g html -i spec-exemplos.yaml -o /tmp/ctm-eg-html` e abrir `/tmp/ctm-eg-html/index.html` | Código de saída 0; `index.html` criado com as operações `GET /pets/{id}`, `GET /status` e `GET /temperature`, cada uma com a seção *Example data* | Código de saída 0; `index.html` criado com as 3 operações e as 3 seções *Example data* | Ambiente; R12 (geração termina mesmo com `Pet.parent → Pet`) | PASSOU |
| 2 | Em `GET /pets/{id}`, ler *Example data* | `Content-Type: application/json`; JSON com `"id" : 10` e `"name" : "Rex"` (examples declarados); a geração não entra em laço por causa de `parent` | `{"id" : 10, "name" : "Rex"}`; `parent` omitido (recursão cortada) | R04, R12 | PASSOU |
| 3 | Em `GET /status`, ler *Example data* | O `example` declarado aparece intacto: `{"id" : 1, "name" : "Rex"}`, com `Content-Type: application/json` | `{"id" : 1, "name" : "Rex"}`, `Content-Type: application/json` | R03, R04 | PASSOU |
| 4 | Em `GET /temperature`, ler os campos `celsius` e `level` | `celsius` ≤ -5 (`maximum: -5`) e `level` > 0 (`minimum: 0` exclusivo) | `"celsius" : 0` e `"level" : 0`: os dois valores violam o schema | R07 (CT-EG-24, CT-EG-25) | **FALHOU** |
| 5 | Em `GET /temperature`, ler o campo `samples` | Array com pelo menos 3 itens (`minItems: 3`) | `"samples" : [ 7, 7 ]`: 2 itens | R09 (CT-EG-29) | **FALHOU** |
| 6 | Em `GET /temperature`, ler o campo `metadata` | Um objeto JSON (`type: object`), por exemplo `{ }` | `"metadata" : "{}"`: uma **string** com o texto `{}` | R12 (CT-EG-48) | **FALHOU** |
| 7 | `java -jar $J generate -g html -i spec-objeto-livre.yaml -o /tmp/ctm-eg-html2` e ler *Example data* de `GET /health` | Código de saída 0; `Content-Type: application/json`; `{"status" : "UP", "uptime" : 42}` | Código de saída 0; `Content-Type: application/json`; `{"status" : "UP", "uptime" : 42}` | R03, R04 (contraparte de CT-EG-45) | PASSOU |

**Resultado geral do caso:** FALHOU. Os sete passos foram executados: quatro passaram e três falharam (4, 5 e 6). As falhas reproduzem pela CLI as divergências F1, F2 e F4 dos casos unitários (ver issues).

**Notas:**
- Os valores numéricos "aleatórios" são reprodutíveis: `ExampleGenerator` usa uma semente fixa (`new Random("ExampleGenerator".hashCode())`). Por isso o passo 4 dá sempre o mesmo resultado.
- O passo 7 mostra que a divergência F3 (CT-EG-45: chaves de um `example` objeto tratadas como tipos de mídia) **não** se reproduz pela CLI. O swagger-parser entrega o `example` como `ObjectNode` do Jackson, e não como `java.util.Map`, então o fluxo defeituoso só ocorre quando a biblioteca é usada programaticamente (geradores customizados, plugins).

---

## Registro

Caso **oag-3**, versão 1, registrado no TestLink na suíte **Hygor - Exemplos de resposta na documentação (ExampleGenerator)**, projeto **OpenAPI Generator - Grupo 6**, plano **Plano de Testes Manuais**. Autor e testador: `hygor`. Registro da execução: **19**, build **OpenAPI Generator v7.25.0**, resultado **Failed**.

O arquivo [`oag-3-exemplos-html.xml`](oag-3-exemplos-html.xml) preserva a definição completa importada, com os identificadores confirmados no TestLink (`internalid=139`, `externalid=3`, versão 1). O download XML do servidor veio incompleto, com 758 bytes e uma seção CDATA sem fechamento; por isso a definição completa foi mantida e os identificadores foram acrescentados, em vez de conservar o download inválido.

Execução: 12/10/2026, build OpenAPI Generator v7.25.0, resultado **FALHOU** (passos 4, 5 e 6).

Evidências:
- [`evidencias/testlink-execucao-oag-3.png`](evidencias/testlink-execucao-oag-3.png): histórico da execução 19, testador `hygor`, resultado Failed e anexo no TestLink;
- [`../../testlink/resultados-testlink.xls`](../../testlink/resultados-testlink.xls): relatório coletivo Test Results Flat, incluindo oag-1, oag-2 e oag-3;
- [`evidencias/ctm-eg-01-html.png`](evidencias/ctm-eg-01-html.png): captura da página gerada no passo 1;
- [`evidencias/html-spec-exemplos.html`](evidencias/html-spec-exemplos.html) e [`evidencias/html-spec-objeto-livre.html`](evidencias/html-spec-objeto-livre.html): páginas geradas;
- [`evidencias/log-geracao-spec-exemplos.txt`](evidencias/log-geracao-spec-exemplos.txt) e [`evidencias/log-geracao-spec-objeto-livre.txt`](evidencias/log-geracao-spec-objeto-livre.txt): saída da CLI.

## Issues

- F1 [#25234](https://github.com/OpenAPITools/openapi-generator/issues/25234) (passo 4), F2 [#25235](https://github.com/OpenAPITools/openapi-generator/issues/25235) (passo 5), F4 [#25237](https://github.com/OpenAPITools/openapi-generator/issues/25237) (passo 6). F3 [#25236](https://github.com/OpenAPITools/openapi-generator/issues/25236) não se reproduz pela CLI (passo 7).
