# Plano de Teste: linhas do Hygor

Texto para colar na linha "Hygor" de cada tabela do Plano de Teste (Google Docs). 

---

## Registro de Mudanças

| Versão | Data | Por | Descrição |
|---|---|---|---|
| 4 | 12/10/2026 | Hygor | Preenchimento da contribuição referente aos testes unitários e ao teste manual de ExampleGenerator. |

---

## 1.1.1 No escopo

**Módulo:** modules/openapi-generator
**Classe:** org.openapitools.codegen.examples.ExampleGenerator
**Métodos (interface pública):** generate(Map, List, Schema), generate(Map, List, String) e generateFromResponseSchema(String, Schema, Set). Por meio deles são exercitados os métodos privados resolvePropertyToExample, resolveModelToExample, resolveAllOfSchemaProperties e randomNumber.
**Função:** gerar os exemplos de corpo de resposta (JSON/XML) de cada operação a partir dos schemas da especificação. DefaultCodegen.fromOperation guarda esses exemplos em op.examples, que aparecem na documentação gerada (html, html2, confluence) e nos stubs de servidor (Spring, WireMock, ASP.NET, Kotlin).
**Complexidade:** segundo o JaCoCo, a classe tem complexidade ciclomática 112, e resolvePropertyToExample tem 32 (uma cadeia de 14 tipos de schema, com recursão para arrays, mapas e $ref).
**Regras R01–R13 consolidam:** estrutura da saída e tipos de mídia; precedência de exemplos explícitos e declarados; generateFromResponseSchema; reprodutibilidade (semente fixa); restrições numéricas (minimum/maximum/exclusive*); precedência em strings e booleans; arrays (maxItems/minItems, limite de 5); mapas; formatos fixos e tipos numéricos; modelos, $ref e ciclos; composição allOf/oneOf.
**Suíte normativa:** 54 casos (CT-EG-01 a CT-EG-54), 80 invocações JUnit, implementados em JUnit 5 com @Nested e @ParameterizedTest.

## 1.1.2 Fora do escopo

XmlExampleGenerator, exercitado só na medida em que define se há saída XML; algoritmos de ModelUtils (isXxxSchema, getSchema, getSimpleRef); leitura do YAML/JSON pelo swagger-parser; InlineModelResolver e o restante de DefaultCodegen.fromOperation (cobertos só pelo caso manual); templates de cada gerador; requestBodyExamples. Os casos sem resultado esperado fundamentado em documentação ficaram fora da suíte normativa e estão registrados na aba "Fora da suíte normativa" da planilha:
- E1: o exemplo gerado é gravado no schema de entrada;
- E2: strings ignoram minLength/maxLength/pattern;
- E3: oneOf sem alternativa válida gera "null";
- E4: array sem items;
- E5: multipleOf;
- E6: nome de modelo nulo gera a mensagem "Custom MIME type example not yet supported: application/json".

## 1.2 Objetivos de Qualidade

Verificar se os exemplos gerados são instâncias válidas do schema de origem, como exige a definição de example na OAS 3.0.3 ("an example of an instance for this schema"):
- respeito a minimum/maximum/exclusive*, minItems/maxItems e type;
- preservação dos exemplos explícitos e declarados, sem reinterpretação;
- seleção correta do tipo de mídia.

Também avaliar a robustez diante de $ref inexistente e de ciclos (diretos, indiretos e via allOf) e a reprodutibilidade da saída, já que o gerador usa números pseudoaleatórios com semente fixa.

## 1.3 Papéis e Responsabilidades

Responsável por ExampleGenerator:
- escolha da classe e medição da complexidade com JaCoCo;
- levantamento das regras R01–R13 e dos oráculos (OAS 3.0.3, JSON Schema Validation wright-00, comentários do código e issues anteriores do projeto);
- matriz de casos e implementação em JUnit 5 (@Nested, @ParameterizedTest);
- execução, medição de cobertura e registro PASS/FAIL;
- triagem e relato das divergências no GitHub Issues;
- elaboração e execução do caso manual CTM-EG-01 (geração de documentação HTML), registrado no TestLink como oag-3, execução 19, pela conta hygor.

---

## 2.1 Visão Geral

**Base:** regras R01–R13. A fonte principal é a OAS 3.0.3:
- Schema Object, example: "A free-form property to include an example of an instance for this schema";
- Media Type Object: o example "SHALL override the example provided by the schema";
- validação de maximum, minimum, exclusiveMinimum, minItems e type segundo o JSON Schema Validation wright-00 (5.2–5.5, 5.11, 5.21), adotado pela OAS 3.0.

As regras de estrutura e precedência vêm dos comentários do código ("use a fixed seed to make the 'random' numbers reproducible", "avoid memory issues by limiting to max. 5 items", "assume application/json"), do ExampleGeneratorTest existente e de issues já corrigidas (#17494, #17497, #18443, #18622, #23966). As regras não constituem uma documentação oficial completa da classe.

**Projeto:** caixa-preta pela interface pública, com:
- partição em classes de equivalência: tipo de schema, tipo de mídia, presença de example/default/enum, origem do valor e forma do modelo (simples, $ref, ciclo, allOf, oneOf);
- análise de valor-limite: maxItems ausente/0/1/5/6/100; minimum = maximum; maximum negativo; minimum 0 exclusivo; data perto da meia-noite UTC.

Casos parametrizados (@ParameterizedTest com @CsvSource/@MethodSource) cobrem as famílias de entradas com um único método e contam como um caso normativo cada. Depois de medir a cobertura, a suíte foi complementada com casos guiados pelos ramos não cobertos (CT-EG-41 a CT-EG-48), cada um justificado por uma regra funcional. Uma segunda rodada (CT-EG-49 a CT-EG-54) cobriu os ramos que ainda faltavam e tinham comportamento esperado documentável; os ramos sem fonte ficaram fora (E3, E6).

**Dublês de teste:** não foram necessários nesta entrega. ExampleGenerator recebe por construtor o mapa de schemas e o OpenAPI, que são objetos de modelo simples construídos diretamente no teste. As dependências estáticas (ModelUtils, Json) e o XmlExampleGenerator criado com "new" não podem ser substituídos sem refatoração. Isso limita o isolamento e fica como ponto para a Entrega 2 (Mockito com mockStatic ou extração de interface, como pede a issue #7523).

**Execução:** casos implementados em JUnit 5 a partir da matriz, com os oráculos definidos antes da execução e mantidos diante das divergências. Na primeira execução, CT-EG-34 falhou por erro do próprio teste: esperava o campo "next" com valor null, mas Json.pretty omite campos nulos. A asserção foi corrigida para "next ausente ou nulo", sem mudar o oráculo (a recursão deve ser cortada). A cobertura foi medida com JaCoCo.

## 2.2 Fases de Teste

| Teste unitário | Teste manual |
|---|---|
| 54 casos normativos (80 invocações) em JUnit 5; 54 executados; 48 PASS e 6 FAIL. | Caso CTM-EG-01: documentação HTML gerada pelo comando generate -g html; 7/7 passos executados; 4 PASS e 3 FAIL (passos 4, 5 e 6, mesmas divergências F1, F2 e F4); registrado no TestLink como oag-3, execução 19, pela conta hygor. |

## 2.3 Triagem de Erros

Foram observadas nove falhas, agrupadas em quatro famílias:
- seis casos unitários: CT-EG-24, com duas invocações, CT-EG-25, CT-EG-29, CT-EG-45, CT-EG-48 e CT-EG-52;
- três passos do caso manual.

**F1: restrições numéricas.** CT-EG-24 esperava valor ≤ -5 para maximum -5 (sem minimum), mas foi gerado 0 (integer) e -0,4004 (number). CT-EG-25 esperava valor > 0 para integer com minimum 0 exclusivo, mas foi gerado 0. O passo 4 de CTM-EG-01 reproduziu os dois pela CLI.
- Causa: randomNumber calcula random * max quando só há max, o que dá (max, 0] para max negativo, e exclusiveMinimum/exclusiveMaximum nunca são lidos.
- Classificação: defeito; severidade média, porque o exemplo aparece em documentação e stubs de servidor e falha na validação do próprio schema; prioridade média. Issue #25234 (https://github.com/OpenAPITools/openapi-generator/issues/25234).

**F2: minItems ignorado.** CT-EG-29 esperava ao menos 3 itens, mas foram gerados 2. O passo 5 reproduziu pela CLI.
- Causa: o comprimento usa só maxItems (padrão 2).
- Classificação: defeito; severidade média; prioridade média. Issue #25235 (https://github.com/OpenAPITools/openapi-generator/issues/25235).

**F3: example objeto reinterpretado como tipos de mídia.** CT-EG-45 esperava um exemplo application/json com {id: 1, name: Rex}, mas foram gerados dois exemplos com contentType "name" e "id".
- Causa: o ramo de tipo primitivo de generateFromResponseSchema faz cast do example (java.util.Map) para o mapa {tipo de mídia → exemplo}.
- Na triagem, a divergência não se reproduziu pela CLI (passo 7), porque o swagger-parser entrega o example como ObjectNode do Jackson. Afeta o uso programático da biblioteca.
- Classificação: defeito; severidade baixa; prioridade baixa. Issue #25236 (https://github.com/OpenAPITools/openapi-generator/issues/25236).

**F4: object gerado com o tipo errado.** CT-EG-48 esperava um objeto JSON para a propriedade type: object sem properties, mas foi gerada a string "{}"; o passo 6 reproduziu pela CLI. CT-EG-52 esperava um objeto JSON para um modelo de nível superior type: object sem properties, mas foi gerado null; pela CLI esse modelo é "desembrulhado" e a operação fica sem exemplo, então só o uso programático é afetado.
- Causa: resolvePropertyToExample devolve o literal String "{}" (o mesmo acontece com $ref inexistente) e resolveModelToExample devolve null para modelo sem properties.
- Classificação: defeito; severidade baixa; prioridade baixa. Issue #25237 (https://github.com/OpenAPITools/openapi-generator/issues/25237).

Os oráculos dos seis casos foram mantidos após a execução. O código de ExampleGenerator na baseline é idêntico ao do branch master, então as issues foram abertas no repositório oficial e não duplicam issues existentes (busca feita em 12/10/2026).

## 2.5 Completude do Teste

**Automático.**
- Critério: cobertura das regras R01–R13, execução de todos os 54 casos normativos e registro das divergências. Os casos ambíguos foram separados da suíte normativa (E1–E6).
- Situação registrada: 54/54 executados (80 invocações); 48 PASS e 6 FAIL, em quatro famílias reportadas.
- Cobertura JaCoCo da classe, só com esta suíte: 97% das instruções (1140/1173), 90% dos desvios (179/198), 97% das linhas (257/266) e 100% dos métodos (13/13). resolvePropertyToExample sozinho: complexidade 32.
- Ramos não cobertos (19): proteções inalcançáveis pela interface pública (Json.pretty e toXml nunca devolvem null; getSchemaItems nunca devolve null em OAS 3.0; allOf nulo depois de isAllOf); property == null em resolvePropertyToExample, que os chamadores não produzem; e os casos E3, E6 e de mapa com name, deixados fora por falta de oráculo.

**Manual.**
- Critério: execução dos sete passos do caso CTM-EG-01. O caso gera a documentação HTML de duas especificações e confere o example de cada operação: controle com examples declarados e autorreferência, example inline, restrições numéricas, minItems, type object e objeto livre.
- Situação registrada: executado na build v7.25.0; 7/7 passos; 4 PASS e 3 FAIL; resultado final FAIL.
- Os passos 4, 5 e 6 reproduzem F1, F2 e F4 e por isso não geraram issues novas. O passo 7 mostrou que F3 não se manifesta pela CLI.
- Execução registrada no TestLink como oag-3 (execução 19), pela conta hygor, e documentada no roteiro CTM-EG-01.md, com testlink-execucao-oag-3.png, ctm-eg-01-html.png, as páginas HTML geradas e os logs da CLI.

## 2.6 Atividades do projeto, estimativas e cronograma

| Data | Atividade |
|---|---|
| 12/10 | Avaliação das classes candidatas (contagem de desvios), escolha de ExampleGenerator e medição da complexidade com JaCoCo |
| 12/10 | Levantamento das regras R01–R13 e dos oráculos; busca de issues anteriores no repositório oficial |
| 12/10 | Elaboração da matriz, implementação da suíte ExampleGeneratorJUnitTest.java, execução e medição de cobertura; segunda rodada guiada pela cobertura (CT-EG-49 a CT-EG-54): 48 PASS, 6 FAIL |
| 12/10 | Triagem das quatro famílias e abertura das issues #25234 a #25237 no repositório oficial |
| 12/10 | Elaboração e execução do caso manual CTM-EG-01 com a CLI gerada a partir do fork; registro no TestLink como oag-3, execução 19 |
| 12/10 | Integração dos artefatos no Plano de Teste e no README |

---

## 3 Entregáveis de Teste

**Testes unitários:**
- `modules/openapi-generator/src/test/java/org/openapitools/codegen/examples/ExampleGeneratorJUnitTest.java`
- dependência de teste `junit-jupiter-params` adicionada em `modules/openapi-generator/pom.xml`, necessária para @ParameterizedTest

**Oráculos, casos e resultados:** `entrega1/hygor/planilha-oraculos-matriz-ExampleGenerator.xlsx`. Abas: Resumo, Oráculos (R01–R13), Matriz de casos (54 casos), Defeitos (F1–F4) e Fora da suíte normativa (E1–E6).

**Incidentes:** issues #25234 (F1), #25235 (F2), #25236 (F3) e #25237 (F4), no repositório oficial do OpenAPI Generator. O texto está em `entrega1/hygor/issues-ExampleGenerator.md`.

**Teste manual:**
- caso TestLink oag-3, "Exemplos de resposta na documentação HTML gerada (CTM-EG-01)", versão 1; execução 19 pela conta hygor, resultado Failed;
- roteiro: `entrega1/hygor/testes-manuais/CTM-EG-01.md`;
- definição completa do caso com identificadores do TestLink: `entrega1/hygor/testes-manuais/oag-3-exemplos-html.xml`.

**Evidências manuais:**
- `entrega1/hygor/testes-manuais/evidencias/`: testlink-execucao-oag-3.png, ctm-eg-01-html.png, as páginas HTML geradas e os logs da CLI;
- relatório coletivo Test Results Flat: `entrega1/testlink/resultados-testlink.xls`;
- arquivos de entrada em `entrega1/hygor/testes-manuais/entradas/`.

**Evidências unitárias:** relatórios do Surefire e do JaCoCo gerados ao executar a suíte (`modules/openapi-generator/target/surefire-reports/` e `modules/openapi-generator/target/site/jacoco/`).

## 4.1 Ferramentas de Teste

- **TestLink:** Hygor, cenário CTM-EG-01, caso oag-3, execução 19, no projeto "OpenAPI Generator - Grupo 6" (http://147.15.86.250:8080/).

- **JUnit 5 Params:** @ParameterizedTest com @CsvSource e @MethodSource; @Nested para agrupar os casos por regra.

## 4.2 Ambiente de Teste

- **Unidade:** ExampleGenerator, no módulo modules/openapi-generator.
- **Sistema operacional:** macOS 26 (arm64). IDE/editor: terminal.
- **Build:** Maven Wrapper do projeto (./mvnw); cobertura com JaCoCo 0.8.12, já configurado no pom.xml.
- **Java/JDK:** OpenJDK 22.0.2 na execução; o projeto compila no nível Java 11 (source/target 11).
- **Framework de testes:** JUnit 5.10.2 (junit-jupiter-api e junit-jupiter-params).
- **Execução:**
  ```
  ./mvnw -pl modules/openapi-generator -am test -Dtest=ExampleGeneratorJUnitTest -Dsurefire.failIfNoSpecifiedTests=false
  ```
- **Revisão executada:** OpenAPI Generator v7.25.0, commit ef964b04480889ef86b56cfae84ade8ad4c91c41.
- **Caso manual:** CLI gerada com `./mvnw -pl modules/openapi-generator-cli -am package -DskipTests` (openapi-generator-cli.jar 7.25.0) e executada no terminal; páginas abertas no Google Chrome.
