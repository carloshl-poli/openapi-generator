# Plano de Teste: linhas do Kauã

Texto para colar na linha "Kauã" de cada tabela do Plano de Teste (Word/Google Docs).

---

## Registro de Mudanças

| Versão | Data | Por | Descrição |
|---|---|---|---|
| 4 | 12/10/2026 | Kauã Gomes | Preenchimento da contribuição referente aos testes unitários e ao teste manual de MergedSpecBuilder. |

---

## 1.1.1 No escopo

**Módulo:** `modules/openapi-generator`  
**Classe:** `org.openapitools.codegen.config.MergedSpecBuilder`  
**Métodos principais:** `buildMergedSpec()`, `mergeSpecs(List<OpenAPI>, List<Server>)`, `mergePathItem(PathItem, PathItem, String)`, `resolveOutputVersion(List<OpenAPI>)` e `resolveOperationIdConflicts(OpenAPI)`.  
**Função:** consolidar múltiplos arquivos de especificação OpenAPI (3.0.x ou 3.1.x), informados por lista ordenada ou por varredura de diretório, em um único documento unificado. Suporta os modos `REF` (shallow merge preservando `$ref` externos) e `DEEP` (inlining completo de rotas e componentes). Implementa detecção e resolução determinística de colisões para métodos HTTP, componentes e `operationId` duplicados sob as estratégias `WARN` (mantém a primeira definição e emite alerta ou renomeia) e `FAIL` (aborta com exceção). O método é utilizado na CLI através do comando `generate` com as opções `--input-spec-files`, `--input-spec-root-directory`, `--merge-mode` e `--merge-conflict-strategy`.  
**Complexidade:** a classe possui complexidade ciclomática total de **119** segundo medição estrutural e JaCoCo (849 linhas de código). Métodos com destaque de complexidade incluem:
- `mergePathItem`: $CC = 17$;
- `parseSpecFiles`: $CC = 11$;
- `resolveOperationIdConflicts`: $CC = 11$;
- `resolveOutputVersion`: $CC = 10$;
- `propagateRootSecurityToOperations`: $CC = 8$;
- `mergeComponentMap`: $CC = 7$.  
**Regras R01–R14 consolidam:** validação de diretório ou lista vazia; distinção de modos `REF` e `DEEP`; validação sintática SemVer de versões OpenAPI; rejeição de versões maior/menor incompatíveis (3.0 vs 3.1); seleção do maior patch compatível; fusão de métodos HTTP distintos na mesma URL; resolução de método duplicado sob `WARN` e `FAIL`; detecção e renomeação determinística de `operationId` duplicados (`_2`, `_3`); deduplicação silenciosa de componentes idênticos; colisão de componentes estruturalmente distintos; propagação descendente de requisitos de segurança globais (`security`); deduplicação de parâmetros de rota por `$ref` e `name:in`; e consolidação de servidores e extensões `x-`.  
**Suíte normativa:** 28 casos (CT-MSB-01 a CT-MSB-28), implementados em JUnit 5 com `@Nested` e `@TempDir`.

---

## 1.1.2 Fora do escopo

Leitura de baixo nível e parsing sintático de strings YAML/JSON delegados ao `swagger-parser` e `Jackson`; resolução de dependências de rede e autenticações remotas complexas tratadas por `AuthParser`; geração final de código-fonte nas linguagens de destino (delegada ao `DefaultGenerator`); e interfaces de usuário ou wrappers de build além das chamadas diretas de configuração exercitadas no teste manual. Comportamentos dependentes de sistemas de arquivos externos indisponíveis ou arquivos com permissões corrompidas de sistema operacional não integram a suíte normativa.

---

## 1.2 Objetivos de Qualidade

Verificar se `MergedSpecBuilder` consolida fielmente múltiplos arquivos OpenAPI preservando a integridade das rotas, parâmetros, esquemas de dados e requisitos de segurança:
- garantia do comportamento esperado nos modos `REF` e `DEEP`;
- cumprimento das estratégias de conflito `WARN` e `FAIL`;
- validação rigorosa de versões OpenAPI, rejeitando versões malformadas ou incompatíveis (3.0 vs 3.1) e selecionando o patch correto;
- unicidade dos identificadores de operação (`operationId`) por renomeação sequencial determinística;
- preservação e propagação de autorizações de segurança para endpoints sem autenticação explícita.

---

## 1.3 Papéis e Responsabilidades

Responsável por `MergedSpecBuilder`:
- escolha da classe e medição da complexidade ciclomática com JaCoCo;
- levantamento das regras R01–R14 e fundamentação dos oráculos a partir da especificação OpenAPI 3.0/3.1 e documentação da CLI;
- projeto da matriz de casos e implementação em JUnit 5 (`@Nested`, `@TempDir`);
- execução, medição de cobertura e registro dos resultados PASS/FAIL;
- elaboração e execução do caso de teste manual CTM-MSB-01 (caso TestLink `oag-3`) pelo comando `generate` da CLI;
- consolidação dos artefatos e integração das evidências na documentação da entrega.

---

## 2.1 Visão Geral

**Base:** regras R01–R14 fundamentadas na especificação OpenAPI 3.0.3 e 3.1.0 (seções OpenAPI Object, Paths Object, Components Object e Operation Object), no Javadoc da classe `MergedSpecBuilder`, e no comportamento documentado das opções `--merge-mode` e `--merge-conflict-strategy` no comando `generate` da CLI.  
**Projeto:** abordagem caixa-preta combinada com análise estrutural:
- particionamento em classes de equivalência: modos `REF` vs `DEEP`; estratégias `WARN` vs `FAIL`; formatos de entrada YAML vs JSON; especificações com/sem sobreposição de rotas; componentes com estruturas idênticas vs divergentes;
- análise de valor-limite: listas de entrada com 0, 1 e múltiplos arquivos; versões OpenAPI com patches distintos; operações compartilhando `operationId` repetido 2 e 3 vezes;
- cobertura estrutural: garantia de cobertura de todos os ramos condicionais nos métodos `mergePathItem`, `resolveOperationIdConflicts` e `resolveOutputVersion`.  
**Dublês de teste:** a suíte combina testes de integração local utilizando diretórios temporários (`@TempDir`) para verificar I/O e serialização com testes rápidos em memória sobre o método `mergeSpecs(...)`, dispensando mocks pesados e exercitando a lógica real do domínio.  
**Execução:** suíte implementada em JUnit 5 (`MergedSpecBuilderJUnitTest.java`) e executada via plugin Maven Surefire, com medição de cobertura JaCoCo.

---

## 2.2 Fases de Teste

| Integrante | Teste unitário | Teste manual |
|---|---|---|
| Kauã | 28 casos normativos em JUnit 5 (com `@Nested` e `@TempDir`); 28 executados; **28 PASS e 0 FAIL**. | Caso CTM-MSB-01 (caso TestLink `oag-3`): mescla de especificações e geração de código via CLI (`--merge-mode` DEEP/REF, `--merge-conflict-strategy` WARN/FAIL e validação de versão); **7/7 passos executados; resultado PASS**. |

---

## 2.3 Triagem de Erros

| Integrante | Descrição |
|---|---|
| Kauã | Todos os 28 casos de teste unitários normativos e todos os 7 passos do teste manual foram executados com sucesso (**100% PASS**), confirmando conformidade estrita da classe `MergedSpecBuilder` com os requisitos R01–R14, com a especificação OpenAPI 3.0/3.1 e com o comportamento documentado da interface de linha de comando. Não foram observadas divergências funcionais nem defeitos de implementação nessa baseline (v7.25.0). Por conseguinte, nenhuma issue de defeito precisou ser aberta no repositório oficial para esta unidade. |

---

## 2.4 Critérios de Suspensão e Requisitos de Retomada

*(Observação: Esta seção no documento do Word é um texto geral e compartilhado do grupo, definindo a política coletiva da equipe para suspensão da execução diante de indisponibilidade de ambiente/build e retomada após saneamento. Não há linha individual por integrante).*

---

## 2.5 Completude do Teste

| Integrante | Tipo de Teste | Critério e situação de completude |
|---|---|---|
| Kauã | Automático | **Critério:** cobertura das regras normativas R01–R14, execução de todos os 28 casos normativos da suíte `MergedSpecBuilderJUnitTest` e cobertura abrangente dos desvios condicionais das rotinas complexas da classe (`mergePathItem`, `resolveOperationIdConflicts`, `resolveOutputVersion` e `parseSpecFiles`).<br><br>**Situação registrada:** 28/28 casos executados; **28 PASS e 0 FAIL** (100% de sucesso). Cobertura JaCoCo da classe `MergedSpecBuilder`: **91% das instruções (1.565 / 1.717)**, **79% dos desvios/branches (144 / 182)**, 87% das linhas (349 / 399) e 91% dos métodos (21 / 23). Ramos residuais não cobertos correspondem a salvaguardas defensivas contra exceções genéricas de I/O e checagens nulas inalcançáveis pela interface pública. |
| Kauã | Manual | **Critério:** execução completa dos 7 passos do caso manual CTM-MSB-01 (suíte TestLink `oag-3`), exercitando na CLI real a mescla de rotas e componentes em modo `DEEP`, apontadores em modo `REF`, tratamento de conflito de rotas sob `WARN` e `FAIL`, e rejeição de versões incompatíveis (OAS 3.0 vs 3.1).<br><br>**Situação registrada:** caso CTM-MSB-01 executado na build OpenAPI Generator v7.25.0 (`openapi-generator-cli.jar`); **7/7 passos executados com sucesso; resultado final PASS**. Especificações consolidadas geradas com êxito (`api_unificada.yaml` e `api_ref.yaml`), logs capturados com sucesso demonstrando o comportamento correto de warnings e abortamento controlado por exceção. Execução documentada no roteiro `CTM-MSB-01.md`, com todas as evidências registradas na pasta `evidencias/` e caso exportado no formato XML do TestLink (`oag-3-merged-spec-cli.xml`). |

---

## 2.6 Atividades do projeto, estimativas e cronograma

- **12/10:** Escolha da classe `MergedSpecBuilder`, medição de complexidade ciclomática JaCoCo, delimitação do escopo e levantamento das regras R01–R14.
- **12/10:** Elaboração da matriz de casos e definição dos oráculos normativos.
- **12/10:** Implementação da suíte `MergedSpecBuilderJUnitTest.java` em JUnit 5 com 28 casos normativos.
- **12/10:** Execução dos testes, coleta de cobertura e registro dos resultados PASS/FAIL.
- **12/10:** Elaboração, execução e registro das evidências do teste manual CTM-MSB-01 (caso TestLink `oag-3`) com a CLI do OpenAPI Generator.
- **12/10:** Integração das seções no Plano de Teste e consolidação dos artefatos na pasta `entrega1/kaua/`.

---

## 3. Entregáveis de Teste

- **Testes unitários:** `MergedSpecBuilderJUnitTest.java` (`modules/openapi-generator/src/test/java/org/openapitools/codegen/config/`).
- **Oráculos, casos e resultados:** `planilha-oraculos-matriz-MergedSpecBuilder.xlsx`: 14 regras e 28 casos normativos.
- **Teste manual:** caso TestLink `oag-3`, *"Mesclar especificações e gerar código com o comando generate da CLI (CTM-MSB-01)"*; roteiro em `CTM-MSB-01.md`.
- **Evidências do teste manual:** arquivos de entrada (`spec1-users.yaml`, `spec2-orders.yaml`, `spec-conflito-metodo.yaml`, `spec-incompativel-31.yaml`), arquivo consolidado de saída `api_unificada.yaml`, classes Java geradas e logs de terminal.
- **Evidências unitárias:** relatórios do Maven Surefire e JaCoCo.
- **Localização no repositório:** `entrega1/kaua/`.

---

## 4.1 Ferramentas de Teste

- **TestLink:** caso `oag-3`, cenário CTM-MSB-01 ("Mesclar especificações e gerar código com o comando generate da CLI"), atribuído a Kauã Gomes.

---

## 4.2 Ambiente de Teste

- **Unidade:** `MergedSpecBuilder.buildMergedSpec()`, módulo `modules/openapi-generator`.
- **Sistema operacional:** Windows 11 / Linux (amd64).
- **IDE:** VS Code / IntelliJ IDEA.
- **Build:** Maven Wrapper do projeto (`./mvnw.cmd` / `./mvnw`).
- **Java/JDK:** OpenJDK 17 ou 21 (nível de compilação do projeto: Java 11).
- **Framework de testes:** JUnit 5.10.2.
- **Comando de execução unitária:**  
  `./mvnw.cmd test -pl modules/openapi-generator -am "-Dtest=MergedSpecBuilderJUnitTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- **Revisão executada:** OpenAPI Generator v7.25.0, commit `ef964b04480889ef86b56cfae84ade8ad4c91c41`.
- **Caso manual:** CLI empacotada gerando `openapi-generator-cli.jar` e executada via terminal com as opções `--input-spec-files`, `--merged-file-output-dir`, `--merge-mode` e `--merge-conflict-strategy`.

