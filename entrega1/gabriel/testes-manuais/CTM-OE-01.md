# Caso de teste manual CTM-OE-01

**Sistema:** OpenAPI Generator v7.25.0 (fork). Commit `ef964b04480889ef86b56cfae84ade8ad4c91c41`.
**Funcionalidade:** comando `validate` da interface de linha de comando (CLI): `openapi-generator-cli validate -i <arquivo> [--recommend]`.
**Unidade relacionada:** `org.openapitools.codegen.validations.oas.OpenApiEvaluator#validate(OpenAPI)`, chamada pelo comando.

**Objetivo:** verificar, no nível do sistema, se o comando `validate` reporta corretamente:
- especificações válidas;
- erros estruturais;
- recomendações (avisos de boas práticas), que só devem aparecer com `--recommend`;
- tags duplicadas.

Os resultados esperados vêm de `docs/usage.md` (seção *validate*), das regras R02, R03, R09 e R11 da planilha de oráculos e da especificação OpenAPI 3.0 ("Each tag name in the list MUST be unique").

---

## Informações sobre a execução do teste

| Campo | Valor |
|---|---|
| Nome do testador | [preencher] |
| Data(s) do teste | 12/10/2026 |
| Local / máquina em uso | Máquina local, Linux (amd64), terminal bash |
| Pré-requisitos | JDK instalado (testado com OpenJDK 25); fork clonado na baseline v7.25.0; arquivos de entrada da pasta `entradas/` (ao lado deste roteiro) |
| Versões de software | OpenAPI Generator CLI 7.25.0; Java OpenJDK 25.0.4.1; SO Linux 7.0.0-38-generic |
| Configuração necessária | Nenhuma propriedade de sistema `openapi.generator.rule.*` definida (configuração padrão) |

### Preparação (executar uma vez antes dos passos)

1. Gerar o JAR da CLI, a partir da raiz do fork:
   ```
   ./mvnw -pl modules/openapi-generator-cli -am package -DskipTests
   ```
2. Entrar na pasta `entrega1/gabriel/testes-manuais/entradas/` e definir o caminho do JAR:
   ```
   J=../../../../modules/openapi-generator-cli/target/openapi-generator-cli.jar
   ```
3. Logo após cada comando, executar `echo $?` para registrar o código de saída.

### Arquivos de entrada (pasta `entradas/`)

| Arquivo | Conteúdo relevante |
|---|---|
| `spec-valida.yaml` | Especificação correta: GET /pets referenciando o esquema Pet |
| `spec-sem-info.yaml` | Especificação sem o campo obrigatório `info` |
| `spec-recomendacoes.yaml` | Especificação válida, com um esquema não utilizado (`Orphan`) e um GET /pets/search com request body |
| `spec-tags-duplicadas.yaml` | Especificação com as tags `[pet, pet, store]` |

---

## Etapas / resultados do script de teste

| Passo | Etapa / entrada do teste | Resultados esperados | Resultados reais | Requisitos validados | Passou / Falhou |
|---|---|---|---|---|---|
| 1 | `java -jar $J version` | Imprime `7.25.0`; código de saída 0 | Imprimiu `7.25.0`; código de saída 0 | Ambiente (baseline) | PASSOU |
| 2 | `java -jar $J validate -i spec-valida.yaml` | Imprime `Validating spec (spec-valida.yaml)` e `No validation issues detected.`; nenhuma seção `Errors:` ou `Warnings:`; código de saída 0 | Imprimiu `Validating spec (spec-valida.yaml)` e `No validation issues detected.`; sem `Errors:` nem `Warnings:`; código de saída 0 | docs/usage.md (exemplo de especificação válida) | PASSOU |
| 3 | `java -jar $J validate -i spec-sem-info.yaml` | Seção `Errors:` com `- attribute info is missing`; linha final `[error] Spec has 1 errors.`; código de saída 1 | `Errors:` com `- attribute info is missing`; `[error] Spec has 1 errors.`; código de saída 1 | docs/usage.md (exemplo de especificação inválida) | PASSOU |
| 4 | `java -jar $J validate -i spec-recomendacoes.yaml` (**sem** `--recommend`) | Nenhuma recomendação listada: não aparecem `Unused model: Orphan` nem o aviso de GET/HEAD com request body; imprime `No validation issues detected.`; código de saída 0 | `Warnings:` com `- Unused model: Orphan`; linha final `[info] Spec has 1 recommendation(s).`; código de saída 0. O aviso de GET/HEAD com body **não** apareceu (correto), mas o de esquema não utilizado apareceu | R03; docs/usage.md (recomendações são opcionais, ativadas por `--recommend`) | **FALHOU** |
| 5 | `java -jar $J validate -i spec-recomendacoes.yaml --recommend` | Seção `Warnings:` com exatamente dois itens, em qualquer ordem: `Unused model: Orphan` e `While technically allowed, GET/HEAD with request body may indicate programming error, and is considered an anti-pattern.`; linha final `[info] Spec has 2 recommendation(s).`; código de saída 0 | `Warnings:` com `- Unused model: Orphan` e `- While technically allowed, GET/HEAD with request body may indicate programming error, and is considered an anti-pattern.`; `[info] Spec has 2 recommendation(s).`; código de saída 0 | R02, R09 | PASSOU |
| 6 | `java -jar $J validate -i spec-tags-duplicadas.yaml` | Seção `Errors:` com `- attribute tags.pet is repeated`; seção `Warnings:` com `Duplicated tag(s): pet`, sem citar `store`; linha final `[error] Spec has 1 errors.`; código de saída 1 | `Errors:` com `- attribute tags.pet is repeated`; `Warnings:` com `- Duplicated tag(s): pet` (store não citado); `[error] Spec has 1 errors.`; código de saída 1 | R11; especificação OpenAPI 3.0 (nomes de tags únicos) | PASSOU |

**Resultado geral do caso:** FALHOU. Os seis passos foram executados: cinco passaram e o passo 4 falhou. A falha do passo 4 é a mesma divergência do caso unitário CT-OE-07, já reportada na issue [#25232](https://github.com/OpenAPITools/openapi-generator/issues/25232).

**Notas:**
- A ordem dos itens dentro de `Errors:` e `Warnings:` não é garantida (o comando usa `HashSet`). Por isso os passos 5 e 6 aceitam qualquer ordem.
- O passo 4 é a contraparte, no nível do sistema, do caso unitário CT-OE-07 (issue [#25232](https://github.com/OpenAPITools/openapi-generator/issues/25232), "OpenApiEvaluator emits 'Unused model' warnings even when recommendations are disabled").

---

## Registro no TestLink

Cadastrado e executado no TestLink do grupo (http://147.15.86.250:8080/) como **oag-2**:
- Projeto: "OpenAPI Generator - Grupo 6"
- Suíte: "Gabriel - Validação de especificações (comando validate)"
- Plano: "Plano de Testes Manuais"
- Build: "OpenAPI Generator v7.25.0"
- Execução: 12/10/2026, status **Failed** (passo 4)

Evidências: [`evidencias/testlink-execucao-oag-2.png`](evidencias/testlink-execucao-oag-2.png) (registro da execução) e [`oag-2-validate-cli.xml`](oag-2-validate-cli.xml) (caso exportado do TestLink).

## Diferenças entre este roteiro e o cadastro no TestLink

- **Título no TestLink:** "Validar especificações com o comando validate da CLI (CTM-OE-01)".
- **Diretório de execução:** no TestLink, os comandos são executados a partir da raiz do repositório, com o JAR em `modules/openapi-generator-cli/target/` e os arquivos de `entradas/` copiados para `/tmp/oag-validate/`. Por isso a linha `Validating spec (...)` exibe o caminho `/tmp/oag-validate/...`. Os passos, as entradas e os resultados esperados são os mesmos deste roteiro.
- **Requisitos:** as regras R02, R03, R09 e R11 aparecem no texto de cada resultado esperado ("Base: ..."). Elas não foram vinculadas como requisitos do TestLink, que mostra "Requirements: None".
- **Tipo de execução:** Manual.
