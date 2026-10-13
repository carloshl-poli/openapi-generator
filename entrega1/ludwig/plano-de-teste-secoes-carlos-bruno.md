# Plano de Teste: linhas do Carlos Bruno

Texto para colar na linha "Carlos Bruno" de cada tabela do Plano de Teste (Google Docs).

---

## Registro de Mudanças

| Versão | Data | Por | Descrição |
|---|---|---|---|
| 1 | 13/10/2026 | Carlos Bruno | Preenchimento da contribuição referente aos testes unitários e ao teste manual de OpenAPINormalizer. |

---

## 1.1.1 No escopo

**Módulo:** modules/openapi-generator
**Classe:** org.openapitools.codegen.OpenAPINormalizer
**Métodos (interface pública):** o construtor `OpenAPINormalizer(OpenAPI, Map)`, o `getRule(String)`, o `normalizeSchema(Schema, Set)` e o `normalize()` (esse último é `protected`, mas como o teste fica no mesmo pacote dá para chamar direto). Através deles acabam sendo exercitados o `processRules`, o `normalizeOperation`, o `normalizeComponentsSchemas`, o `normalizeProperties`, o `normalizeOneOf`, o `normalizeAnyOf` e os vários `process*` de cada regra.
**Função:** é a classe que aplica as regras do `--openapi-normalizer` na especificação antes de gerar qualquer código. A ideia é entregar para os geradores uma spec equivalente, porém mais simples: junta `oneOf`/`anyOf` redundantes, apaga enum que não diz nada, arruma as tags e tira o que está marcado como interno (`x-internal`).
**Complexidade:** medi com o JaCoCo e a classe deu complexidade ciclomática 509 no total, com o `normalizeSchema` sozinho batendo 29 — ele é basicamente um despachante gigante (array, oneOf, anyOf, allOf, composed, properties, map, boolean, integer, simple...), com recursão nos compostos. Foi justamente esse método que me convenceu a escolher a classe: tem cara de lugar onde um teste sistemático encontra coisa interessante.
**Regras R00–R06:** R00 cobre o processamento das regras em si (valores padrão, como escreve cada valor, nome inválido, DISABLE_ALL); R01 é o SIMPLIFY_BOOLEAN_ENUM; R02 o SIMPLIFY_ONEOF_ANYOF; R03 o SIMPLIFY_ONEOF_ANYOF_ENUM; R04 o KEEP_ONLY_FIRST_TAG_IN_OPERATION; R05 junta SET_TAGS_FOR_ALL_OPERATIONS e SET_TAGS_TO_OPERATIONID; R06 é o REMOVE_X_INTERNAL.
**Suíte normativa:** 46 casos (CT-NO-01 a CT-NO-46), que viram 47 invocações JUnit por causa de um parametrizado com 2 linhas. Tudo em JUnit 5, com @Nested separando por regra e @ParameterizedTest onde fazia sentido. A primeira rodada (CT-NO-01 a CT-NO-36) cobre uma regra por vez; a segunda (CT-NO-37 a CT-NO-46) foi guiada pela cobertura e mira os ramos estruturais do `normalizeSchema` (array, map, $ref puro, schema nulo, oneOf de 6 tipos, $ref com sibling, `x-internal` em string) e interações entre regras.

## 1.1.2 Fora do escopo

Deixei de fora de propósito: FILTER e SECURITY_SCHEMES_FILTER (têm uma mini-linguagem de filtros própria, daria outra entrega); NORMALIZE_31SPEC (é o mundo do OpenAPI 3.1, precisaria de fixtures próprias); REPLACE_ONE_OF_BY_DISCRIMINATOR_MAPPING (mexe com herança e discriminator, e o oráculo acaba dependendo do gerador); ADD_UNSIGNED_TO_INTEGER_WITH_INVALID_MAX_VALUE; o resto do allOf avançado (REFACTOR_ALLOF_WITH_PROPERTIES_ONLY); e SORT_MODEL_PROPERTIES, SET_*_NULLABLE e FIX_DUPLICATED_OPERATIONID. Registrei tudo isso na aba "Fora-da-suite" da planilha (E1–E8) com o motivo de cada um. Duas ressalvas honestas: o ADD_UNSIGNED (E4) eu deixei de fora por recorte mesmo ele tendo teste upstream — os valores-limite 2^32-1/2^64-1 mereceriam casos próprios; e o REFACTOR_ALLOF_WITH_PROPERTIES_ONLY (E5) vem ligado por padrão mas não montei oráculo dedicado para ele, então fica como trabalho futuro. Vale dizer: a cobertura da classe como um todo ficou baixa por causa desse recorte, e isso é esperado — o que importava era cobrir bem as 6 regras escolhidas.

## 1.2 Objetivos de Qualidade

O que eu queria responder com os testes: as regras fazem exatamente o que o `docs/customization.md` promete? Em particular:
- com tudo desligado (ou DISABLE_ALL), a spec passa ilesa;
- as simplificações de oneOf/anyOf e de enum não perdem informação;
- a reescrita de tags só mexe no que foi pedido (primeira tag, ou o valor fixo);
- tirar o `x-internal` não quebra nada ao redor (operação sem extensions, schema sem o campo).

E, de quebra, ver se a classe aguenta bem entradas esquisitas: nome de regra que não existe, operação sem tag nenhuma, schema sem enum.

## 1.3 Papéis e Responsabilidades

Fiquei responsável pelo OpenAPINormalizer de ponta a ponta:
- escolher a classe e medir a complexidade com o JaCoCo;
- levantar as regras R00–R06 e os oráculos (a fonte foi o `docs/customization.md` mais os comentários no próprio código);
- montar a matriz e implementar a suíte em JUnit 5 (@Nested, @ParameterizedTest);
- rodar, medir cobertura e anotar PASS/FAIL;
- montar e rodar o caso manual CTM-NO-01 (reemitindo a spec com o gerador `openapi` pela CLI), documentado em texto.

---

## 2.1 Visão Geral

**Base:** as regras R00–R06. A fonte principal foi o `docs/customization.md`, que tem um exemplo de CLI para cada regra do normalizador, mais os comentários em cima de cada constante em `OpenAPINormalizer.java:64-174` (eles explicam bem a intenção da regra). Para os testes que partem de arquivo, usei os YAMLs de fixture que já existem em `modules/openapi-generator/src/test/resources/3_0/` — antes de normalizar eu sempre confiro a pré-condição (ex.: o enum tem 2 itens mesmo?), para o teste não passar "por acidente".

**Projeto:** teste caixa-preta pela interface pública, com:
- partição em classes de equivalência: regra ligada/desligada/ausente; enum com 0, 1 ou 2 valores; oneOf/anyOf com 1 ou N sub-schemas, só-de-enum vs misto; operação com 0, 1 ou N tags, com ou sem operationId, com ou sem extensions;
- análise de valor-limite: enum unitário `[true]` (será que apaga também? apaga); anyOf unitário; string vazia no SET_TAGS_FOR_ALL_OPERATIONS (descobri que é tratada como "regra inativa").

Os parametrizados (@ParameterizedTest com @CsvSource) cobrem uma família inteira num método só e contam como um caso normativo cada — o CT-NO-31, por exemplo, roda 2 vezes (2 e 3 tags). Depois da primeira execução (36/36 PASS), rodei o relatório do JaCoCo e fiz a segunda rodada (CT-NO-37 a CT-NO-46) em cima dos ramos não cobertos que tinham comportamento esperado documentável — mesmo esquema que o Hygor usou (CT-EG-41 em diante). Os ramos que sobraram são das regras E1–E8, fora do recorte.

**Dublês de teste:** não usei nenhum. O construtor recebe o OpenAPI e o mapa de regras, e os dois são fáceis de montar na mão (objeto de modelo ou `TestUtils.parseSpec`). O isolamento total esbarra nos estáticos (`ModelUtils`), que não dá para trocar sem refatorar — deixo isso como ponto para a Entrega 2, igual ao caso do ExampleGenerator.

**Execução:** escrevi os casos a partir da matriz, com o oráculo definido antes de rodar. Resultado da primeira execução: 36/36 PASS, sem precisar corrigir teste nem oráculo. Cobertura medida com o JaCoCo logo em seguida.

## 2.2 Fases de Teste

| Teste unitário | Teste manual |
|---|---|
| 46 casos normativos (47 invocações) em JUnit 5; 46 executados; 47/47 PASS e 0 FAIL. | Caso CTM-NO-01: pego a spec de entrada, reemito ela com o gerador `openapi` (`generate -g openapi --openapi-normalizer ...`, uma regra por passo mais um controle) e confiro tags, enum simplificado e sumiço do `x-internal`; 6/6 passos executados; 6 PASS e 0 FAIL; documentado em texto (o TestLink do grupo está nos casos oag-1 e oag-2; o meu foi o oag-4). |

## 2.3 Triagem de Erros

Não houve o que triar: 46 casos unitários PASS e 6 passos manuais PASS, então não abri nenhuma issue. Dito isso, anotei dois comportamentos que o professor pode achar curiosos (não são defeitos, são decisões do código): o SIMPLIFY_BOOLEAN_ENUM apaga até enum unitário `[true]` — faz sentido, um enum booleano de um valor só não informa nada; e as três simplificações vêm ligadas por padrão, então em cada passo do manual desliguei explicitamente as que não estavam sob teste para mostrar só o efeito da regra da vez (detalhe na seção Notas do CTM-NO-01).

## 2.5 Completude do Teste

**Automático.**
- Critério: cobrir as regras R00–R06, rodar os 46 casos e registrar qualquer divergência (não houve). O que era de outro recorte foi para a aba "Fora-da-suite" (E1–E8).
- Situação: 46/46 executados (47 invocações); 47/47 PASS e 0 FAIL.
- Cobertura JaCoCo da classe, só com a minha suíte: 45% das instruções (2063/4560), 35% dos desvios (294/847), 46% das linhas (512/1109) e 62 de 84 métodos. O número parece baixo, mas é o preço do recorte em 6 de ~25 regras — e, ponto importante, os métodos que eu testei estão quase todos em 100%: `getRule`, `skipNormalization`, `normalizeArraySchema`, `normalizeMapSchema`, `normalizeSimpleSchema`, `processRemoveXInternalFromOperation`, `processSetTagsForAllOperations` e `processSetTagsToOperationId` zeraram os desvios; `processKeepOnlyFirstTagInOperation` 7/8; `processSimplifyBooleanEnum` 6/8; `normalizeOneOf`/`normalizeAnyOf` 7/8; `normalizeSchema` 39/56 (complexidade 29).
- O que não foi coberto: ramos das regras E1–E8 (filtros, 31SPEC, discriminator-mapping, unsigned/allOf avançado, sort/nullable/fix-operationId).

**Manual.**
- Critério: rodar os seis passos do CTM-NO-01. Cada passo reemite a spec com uma regra e confere o efeito (tags, boolean sem enum, oneOf fundido, sem `x-internal`), mais um controle que mostra o "antes".
- Situação: rodei na build v7.25.0; 6/6 passos; 6 PASS e 0 FAIL; resultado final PASSOU.
- Está tudo no roteiro CTM-NO-01.md, com as 6 specs geradas e os 6 logs da CLI em `evidencias/`.

## 2.6 Atividades do projeto, estimativas e cronograma

| Data | Atividade |
|---|---|
| 13/10 | Olhei as classes candidatas, escolhi o OpenAPINormalizer e medi a complexidade com o JaCoCo |
| 13/10 | Levantei as regras R00–R06 e os oráculos (`docs/customization.md` + comentários do código) |
| 13/10 | Montei a matriz, implementei a suíte OpenAPINormalizerJUnitTest.java (precisei adicionar o `junit-jupiter-params` no pom, igual ao Hygor fez), rodei: 36 PASS, 0 FAIL; segunda rodada guiada pela cobertura (CT-NO-37 a CT-NO-46): 47/47 PASS, 0 FAIL |
| 13/10 | Montei e rodei o caso manual CTM-NO-01 com a CLI compilada do fork; cadastrei e executei o oag-4 no TestLink |
| 13/10 | Juntei os artefatos no Plano de Teste e no README |

---

## 3 Entregáveis de Teste

**Testes unitários:**
- `modules/openapi-generator/src/test/java/org/openapitools/codegen/OpenAPINormalizerJUnitTest.java`
- dependência de teste `junit-jupiter-params` no `modules/openapi-generator/pom.xml`, sem a qual o @ParameterizedTest não compila (a mesma adição que o Hygor fez para o ExampleGenerator)

**Oráculos, casos e resultados:** `entrega1/ludwig/planilha-oraculos-matriz-OpenAPINormalizer.xlsx`. Abas: Resumo, Oraculos (R00–R06), Matriz (46 casos, com colunas separadas de esperado, obtido e data de execução), Defeitos (vazia — não houve) e Fora-da-suite (E1–E8, com o motivo de cada exclusão). A planilha foi gerada pelo script `entrega1/ludwig/gerar-planilha.py` (openpyxl), versionado junto para auditoria.

**Incidentes:** nenhum observado; nenhuma issue aberta.

**Teste manual:**
- caso "Regras do normalizador OpenAPI via CLI (CTM-NO-01)";
- roteiro: `entrega1/ludwig/testes-manuais/CTM-NO-01.md`;
- versão do caso no formato de importação do TestLink: `entrega1/ludwig/testes-manuais/oag-4-normalizer.xml` (foi esse arquivo que usei para cadastrar o caso).

**Evidências manuais:**
- `entrega1/ludwig/testes-manuais/evidencias/`: 6 specs normalizadas + 6 logs da CLI;
- entrada em `entrega1/ludwig/testes-manuais/entradas/` (spec-normalizer.yaml).

**Evidências unitárias:** relatórios do Surefire e do JaCoCo que saem ao rodar a suíte (`modules/openapi-generator/target/surefire-reports/` e `modules/openapi-generator/target/site/jacoco/`).

## 4.1 Ferramentas de Teste

- **JUnit 5 Params:** @ParameterizedTest com @CsvSource; @Nested para separar os casos por regra (espelhei a organização do teste do Hygor para o grupo manter um padrão).

## 4.2 Ambiente de Teste

- **Unidade:** OpenAPINormalizer, no módulo modules/openapi-generator.
- **Sistema operacional:** Arch Linux (x86_64). Shell: fish.
- **Build:** Maven Wrapper do projeto (./mvnw); cobertura com JaCoCo 0.8.12, que já vem configurado no pom.xml.
- **Java/JDK:** OpenJDK 17.0.20.1; o projeto compila em nível Java 11 (source/target 11), então roda em qualquer JDK 11+.
- **Framework de testes:** JUnit 5.10.2 (junit-jupiter-api e junit-jupiter-params).
- **Execução:**
  ```
  ./mvnw -pl modules/openapi-generator test -Dtest=OpenAPINormalizerJUnitTest -Dsurefire.failIfNoSpecifiedTests=false
  ```
- **Revisão executada:** OpenAPI Generator v7.25.0, commit ef964b04480889ef86b56cfae84ade8ad4c91c41.
- **Caso manual:** CLI compilada com `./mvnw -pl modules/openapi-generator-cli -am package -DskipTests` (openapi-generator-cli.jar 7.25.0), rodada no terminal; a spec de saída foi conferida com `python3 -c` (parse do JSON gerado).
