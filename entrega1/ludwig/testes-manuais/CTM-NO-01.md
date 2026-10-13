# Caso de teste manual CTM-NO-01

**Sistema:** OpenAPI Generator v7.25.0 (fork). Commit `ef964b04480889ef86b56cfae84ade8ad4c91c41`.
**Funcionalidade:** as regras do normalizador OpenAPI chamadas pela CLI: `openapi-generator-cli generate -g openapi -i <arquivo> -o <saída> --openapi-normalizer <REGRA>=<valor>`. Usei o gerador `openapi` de propósito: em vez de gerar código em alguma linguagem, ele cospe de volta a própria especificação já normalizada, então dá para conferir o efeito de cada regra olhando o YAML/JSON de saída.
**Unidade relacionada:** `org.openapitools.codegen.OpenAPINormalizer`, que o `DefaultGenerator`/`DefaultCodegen` aplica na spec antes de gerar qualquer coisa quando o `--openapi-normalizer` é passado.

**Objetivo:** mostrar, no nível do sistema (pela CLI de verdade, não só no teste unitário), que as regras fazem o que prometem:
- as tags da operação ficam como pedido (`KEEP_ONLY_FIRST_TAG_IN_OPERATION`, `SET_TAGS_FOR_ALL_OPERATIONS`);
- o `boolean` com enum e o `oneOf` de enums unitários são simplificados (`SIMPLIFY_BOOLEAN_ENUM`, `SIMPLIFY_ONEOF_ANYOF_ENUM`);
- o `x-internal` some (`REMOVE_X_INTERNAL`).

Cada passo liga só a sua regra e desliga explicitamente as de simplificação (`SIMPLIFY_BOOLEAN_ENUM`, `SIMPLIFY_ONEOF_ANYOF`, `SIMPLIFY_ONEOF_ANYOF_ENUM` — as três vêm ligadas por padrão e, sem isso, contaminariam a saída umas das outras). Assim a spec gerada em cada passo mostra *só* o efeito da regra testada.

Os resultados esperados vêm das regras R01, R03, R04, R05 e R06 da planilha de oráculos e do `docs/customization.md` (a seção do openapi-normalizer, que tem um exemplo de cada regra).

---

## Informações sobre a execução do teste

| Campo | Valor |
|---|---|
| Nome do testador | Carlos Bruno |
| Data(s) do teste | 13/10/2026 |
| Local / máquina em uso | Máquina local, Arch Linux (x86_64), shell fish |
| Pré-requisitos | JDK 17 instalado (pacman `jdk17-openjdk`); fork clonado na baseline v7.25.0; arquivo de entrada da pasta `entradas/` (ao lado deste roteiro) |
| Versões de software | OpenAPI Generator CLI 7.25.0; Java OpenJDK 17.0.20.1 |
| Configuração necessária | Nenhuma além das regras `--openapi-normalizer` de cada passo |

### Preparação (executar uma vez antes dos passos)

1. Gerar o JAR da CLI a partir da raiz do fork:
   ```
   ./mvnw -pl modules/openapi-generator-cli -am package -DskipTests
   ```
2. Entrar na pasta `entrega1/ludwig/testes-manuais/entradas/` e definir o caminho do JAR:
   ```
   set -x J ../../../../modules/openapi-generator-cli/target/openapi-generator-cli.jar
   ```

### Arquivo de entrada (pasta `entradas/`)

| Arquivo | Conteúdo relevante |
|---|---|
| `spec-normalizer.yaml` | `GET /pets/{id}` com tags `[pet, basic]` e schema `Pet` (`active`: boolean com enum `[true, false]`); `GET /admin/status` com `x-internal: true`; schema `Status`: `oneOf` de enums unitários `[UP]`/`[DOWN]` |

---

## Etapas / resultados do script de teste

| Passo | Etapa / entrada do teste | Resultados esperados | Resultados reais | Requisitos validados | Passou / Falhou |
|---|---|---|---|---|---|
| 1 | `java -jar $J generate -g openapi -i spec-normalizer.yaml -o /tmp/ctm-no-keep --openapi-normalizer KEEP_ONLY_FIRST_TAG_IN_OPERATION=true,SIMPLIFY_BOOLEAN_ENUM=false,SIMPLIFY_ONEOF_ANYOF=false,SIMPLIFY_ONEOF_ANYOF_ENUM=false` e abrir a spec gerada (`spec-normalizada-keep.json`) | Código de saída 0; `GET /pets/{id}` com tags `[pet]`; resto intacto (enum, oneOf, `x-internal`) | Código de saída 0; tags `[pet]`; `active` com enum, `Status` com oneOf, `x-internal` presente | R04 (CT-NO-19) | PASSOU |
| 2 | `java -jar $J generate -g openapi -i spec-normalizer.yaml -o /tmp/ctm-no-settags --openapi-normalizer SET_TAGS_FOR_ALL_OPERATIONS=core,SIMPLIFY_BOOLEAN_ENUM=false,SIMPLIFY_ONEOF_ANYOF=false,SIMPLIFY_ONEOF_ANYOF_ENUM=false` e abrir a spec gerada | Código de saída 0; as duas operações com tags `[core]`; resto intacto | Código de saída 0; tags `[core]` nas duas; resto intacto | R05 (CT-NO-23) | PASSOU |
| 3 | `java -jar $J generate -g openapi -i spec-normalizer.yaml -o /tmp/ctm-no-bool --openapi-normalizer SIMPLIFY_BOOLEAN_ENUM=true,SIMPLIFY_ONEOF_ANYOF=false,SIMPLIFY_ONEOF_ANYOF_ENUM=false` e ler `Pet.active` | Código de saída 0; `active` segue `type: boolean` sem `enum`; tags, oneOf e `x-internal` intactos | Código de saída 0; `active` sem `enum`; resto intacto | R01 (CT-NO-06) | PASSOU |
| 4 | `java -jar $J generate -g openapi -i spec-normalizer.yaml -o /tmp/ctm-no-enum --openapi-normalizer SIMPLIFY_ONEOF_ANYOF_ENUM=true,SIMPLIFY_BOOLEAN_ENUM=false,SIMPLIFY_ONEOF_ANYOF=false` e ler `Status` | Código de saída 0; `Status` sem `oneOf`, com `enum: [UP, DOWN]`; resto intacto | Código de saída 0; `Status` com `enum: [UP, DOWN]` (+ `x-enum-descriptions`); resto intacto | R03 (CT-NO-15) | PASSOU |
| 5 | `java -jar $J generate -g openapi -i spec-normalizer.yaml -o /tmp/ctm-no-xint --openapi-normalizer REMOVE_X_INTERNAL=true,SIMPLIFY_BOOLEAN_ENUM=false,SIMPLIFY_ONEOF_ANYOF=false,SIMPLIFY_ONEOF_ANYOF_ENUM=false` e ler a spec gerada | Código de saída 0; nenhuma ocorrência de `x-internal`; resto intacto | Código de saída 0; sem `x-internal`; resto intacto | R06 (CT-NO-26) | PASSOU |
| 6 | Controle com as três simplificações desligadas: `java -jar $J generate -g openapi -i spec-normalizer.yaml -o /tmp/ctm-no-base --openapi-normalizer SIMPLIFY_BOOLEAN_ENUM=false,SIMPLIFY_ONEOF_ANYOF=false,SIMPLIFY_ONEOF_ANYOF_ENUM=false` e ler a spec gerada (`spec-normalizada-base.json`) | Código de saída 0; `GET /pets/{id}` mantém tags `[pet, basic]`; `Pet.active` mantém `enum: [true, false]`; `Status` mantém `oneOf`; `x-internal` presente | Código de saída 0; tags, enum, oneOf e `x-internal` intactos | R04/R01/R03/R06 (contrapartes de CT-NO-22, CT-NO-08, CT-NO-13, CT-NO-28) | PASSOU |

**Resultado geral do caso:** PASSOU. Os seis passos foram executados: seis passaram e nenhum falhou.

**Notas:**
- O isolamento de cada passo foi conferido de verdade: como as três regras de simplificação vêm ligadas por padrão, cada comando desliga explicitamente as que não estão sob teste. Na spec gerada de cada passo, só o efeito da regra da vez aparece — todo o resto fica idêntico ao controle (passo 6), que é o "antes" fiel.
- No passo 4, o `Status` fundido ganha também um `x-enum-descriptions` com os títulos originais (`up`/`down`); a informação dos títulos não se perde na fusão.

---

## Registro no TestLink

Cadastrado e executado no TestLink do grupo (http://147.15.86.250:8080/) como **oag-4**:
- Projeto: "OpenAPI Generator - Grupo 6" (id 118, prefixo `oag`)
- Suíte: "Carlos Bruno - Regras do normalizador (comando generate)" (id 148)
- Caso: "Regras do normalizador OpenAPI via CLI (CTM-NO-01)" (id 149, `oag-4`, versão 1)
- Plano: "Plano de Testes Manuais" (id 128)
- Build: "OpenAPI Generator v7.25.0" (id 6)
- Execução: id 20, 13/10/2026, status **Passed**, 6/6 passos Passed, duração 10 min

O plano atualizado está em [`resultados-testlink.xls`](../../testlink/resultados-testlink.xls).

**Nota sobre o arquivo:** o TestLink 1.9.20 não oferece exportação xls desse relatório, então o `resultados-testlink.xls` é montado no LibreOffice a partir dos dados reais do plano (é assim desde a versão do Gabriel — o Author "Unknown Creator" é o padrão do LibreOffice). Os valores da linha oag-4 (suíte, caso, build, resultado, data, executor, notas) são os da execução id 20, verificáveis via API do TestLink.

## Diferenças entre este roteiro e o cadastro no TestLink

- **Diretório de execução:** no TestLink, os comandos partem da raiz do repositório com o JAR em `modules/openapi-generator-cli/target/` e a entrada em `entrega1/ludwig/testes-manuais/entradas/`. Os passos e resultados esperados são os mesmos deste roteiro.
- **Tipo de execução:** Manual.

## Registro em texto

Caso documentado em texto, como o enunciado permite para os casos além do cenário no TestLink. O arquivo [`oag-4-normalizer.xml`](oag-4-normalizer.xml) traz o mesmo caso no formato de importação do TestLink (e foi o arquivo usado para cadastrá-lo, via API do TestLink).

Execução: 13/10/2026, build OpenAPI Generator v7.25.0, resultado **PASSOU** (6/6).

Evidências:
- [`evidencias/log-geracao-keep.txt`](evidencias/log-geracao-keep.txt), [`evidencias/log-geracao-settags.txt`](evidencias/log-geracao-settags.txt), [`evidencias/log-geracao-bool.txt`](evidencias/log-geracao-bool.txt), [`evidencias/log-geracao-enum.txt`](evidencias/log-geracao-enum.txt), [`evidencias/log-geracao-xint.txt`](evidencias/log-geracao-xint.txt), [`evidencias/log-geracao-base.txt`](evidencias/log-geracao-base.txt): saídas da CLI;
- [`evidencias/spec-normalizada-keep.json`](evidencias/spec-normalizada-keep.json), [`evidencias/spec-normalizada-settags.json`](evidencias/spec-normalizada-settags.json), [`evidencias/spec-normalizada-bool.json`](evidencias/spec-normalizada-bool.json), [`evidencias/spec-normalizada-enum.json`](evidencias/spec-normalizada-enum.json), [`evidencias/spec-normalizada-xint.json`](evidencias/spec-normalizada-xint.json), [`evidencias/spec-normalizada-base.json`](evidencias/spec-normalizada-base.json): specs geradas pelo gerador `openapi` (formato `openapi.json` renomeado por passo).
