# COS482 — Entrega 1

Artefatos da Entrega 1 da disciplina COS482 — Qualidade de Software,
realizada sobre o OpenAPI Generator v7.25.0.

**Baseline:** `ef964b04480889ef86b56cfae84ade8ad4c91c41`

## Integrantes

### Carlos

#### Testes unitários

- [IgnoreLineParserTest.java](../modules/openapi-generator/src/test/java/org/openapitools/codegen/ignore/rules/IgnoreLineParserTest.java)
- [Oráculos e matriz de testes](carlos/Oraculos-e-Matriz-de-Testes-IgnoreLineParser.xlsx)

Resultado da suíte: **33 casos executados — 28 PASS e 5 FAIL**.

#### Teste manual

Caso: processamento de `.openapi-generator-ignore`.

- [Especificação exportada do TestLink](carlos/testes-manuais/oag-1-openapi-generator-ignore.xml)
- [Evidências de execução](carlos/testes-manuais/evidencias/)

Resultado: **PASS — 6/6 passos executados com sucesso**.

### Gabriel

#### Testes unitários

- [OpenApiEvaluatorJUnitTest.java](../modules/openapi-generator/src/test/java/org/openapitools/codegen/validations/oas/OpenApiEvaluatorJUnitTest.java)
- [Oráculos e matriz de testes](gabriel/planilha-oraculos-matriz-OpenApiEvaluator.xlsx)

Resultado da suíte: **28 casos executados — 26 PASS e 2 FAIL**.

Cobertura JaCoCo de `validate(OpenAPI)`: 100% das instruções e 100% dos desvios.

#### Teste manual

Caso: comando `validate` da CLI (oag-2, CTM-OE-01).

- [Roteiro do caso](gabriel/testes-manuais/CTM-OE-01.md)
- [Especificação exportada do TestLink](gabriel/testes-manuais/oag-2-validate-cli.xml)
- [Arquivos de entrada](gabriel/testes-manuais/entradas/)
- [Evidências de execução](gabriel/testes-manuais/evidencias/)

Resultado: **FAIL — 6/6 passos executados; 5 PASS e 1 FAIL (passo 4)**.

#### Issues

- [#25232](https://github.com/OpenAPITools/openapi-generator/issues/25232): recomendação "Unused model" emitida com as recomendações desligadas (CT-OE-07 e passo 4 de oag-2).
- [#25233](https://github.com/OpenAPITools/openapi-generator/issues/25233): default numérico igual a um item do enum, mas com escala diferente, acusado como fora do enum (CT-OE-14).

### Hygor

#### Testes unitários

- [ExampleGeneratorJUnitTest.java](../modules/openapi-generator/src/test/java/org/openapitools/codegen/examples/ExampleGeneratorJUnitTest.java)
- [Oráculos e matriz de testes](hygor/planilha-oraculos-matriz-ExampleGenerator.xlsx)

Classe sob teste: `org.openapitools.codegen.examples.ExampleGenerator` (complexidade ciclomática 112 na classe; 32 em `resolvePropertyToExample`).

Resultado da suíte: **54 casos executados (80 invocações JUnit): 48 PASS e 6 FAIL**.

Cobertura JaCoCo da classe: 97% das instruções e 90% dos desvios.

#### Teste manual

Caso TestLink **oag-3**: exemplos de resposta na documentação HTML gerada (CTM-EG-01), versão 1. Execução **19**, registrada pela conta `hygor`, no Plano de Testes Manuais, build OpenAPI Generator v7.25.0.

- [Roteiro do caso](hygor/testes-manuais/CTM-EG-01.md)
- [Definição XML do caso com os identificadores do TestLink](hygor/testes-manuais/oag-3-exemplos-html.xml)
- [Registro da execução no TestLink](hygor/testes-manuais/evidencias/testlink-execucao-oag-3.png)
- [Arquivos de entrada](hygor/testes-manuais/entradas/)
- [Evidências de execução](hygor/testes-manuais/evidencias/)

Resultado: **FAIL: 7/7 passos executados; 4 PASS e 3 FAIL (passos 4, 5 e 6)**.

#### Issues

- [Texto das issues](hygor/issues-ExampleGenerator.md)
- [#25234](https://github.com/OpenAPITools/openapi-generator/issues/25234): valores numéricos gerados violam `maximum` negativo e `exclusiveMinimum` (CT-EG-24, CT-EG-25, passo 4 de CTM-EG-01).
- [#25235](https://github.com/OpenAPITools/openapi-generator/issues/25235): arrays gerados ignoram `minItems` (CT-EG-29, passo 5 de CTM-EG-01).
- [#25236](https://github.com/OpenAPITools/openapi-generator/issues/25236): chaves de um `example` objeto viram tipos de mídia em `generateFromResponseSchema` (CT-EG-45).
- [#25237](https://github.com/OpenAPITools/openapi-generator/issues/25237): `type: object` sem propriedades gera a string `"{}"` ou `null` (CT-EG-48, CT-EG-52, passo 6 de CTM-EG-01).

### Kauã

#### Testes unitários

- [MergedSpecBuilderJUnitTest.java](../modules/openapi-generator/src/test/java/org/openapitools/codegen/config/MergedSpecBuilderJUnitTest.java)
- [Oráculos e matriz de testes](kaua/planilha-oraculos-matriz-MergedSpecBuilder.xlsx)

Classe sob teste: `org.openapitools.codegen.config.MergedSpecBuilder` (complexidade ciclomática 119 na classe; 17 em `mergePathItem`, 11 em `resolveOperationIdConflicts`, 11 em `parseSpecFiles` e 10 em `resolveOutputVersion`).

Resultado da suíte: **28 casos executados: 28 PASS e 0 FAIL**.

Cobertura JaCoCo da classe: 91% das instruções (1565 / 1717) e 79% dos desvios (144 / 182).

#### Teste manual

Caso: mescla de especificações e geração de código via CLI (CTM-MSB-01, caso TestLink oag-3).

- [Roteiro do caso](kaua/testes-manuais/CTM-MSB-01.md)
- [Especificação exportada do TestLink](kaua/testes-manuais/oag-3-merged-spec-cli.xml)
- [Arquivos de entrada](kaua/testes-manuais/entradas/)
- [Evidências da execução](kaua/testes-manuais/evidencias/)

Resultado: **PASS: 7/7 passos executados com sucesso**.

### Carlos Bruno

#### Testes unitários

- [OpenAPINormalizerJUnitTest.java](../modules/openapi-generator/src/test/java/org/openapitools/codegen/OpenAPINormalizerJUnitTest.java)
- [Oráculos e matriz de testes](ludwig/planilha-oraculos-matriz-OpenAPINormalizer.xlsx)

Classe sob teste: `org.openapitools.codegen.OpenAPINormalizer` (complexidade ciclomática 509 na classe; 29 em `normalizeSchema`).

Resultado da suíte: **46 casos executados (47 invocações JUnit): 47/47 PASS e 0 FAIL**.

Cobertura JaCoCo da classe (só esta suíte, recorte R00–R06): 45% das instruções e 35% dos desvios; os métodos das regras testadas estão próximos de 100% (`getRule`, `skipNormalization`, `normalizeArraySchema`, `normalizeMapSchema`, `normalizeSimpleSchema`, `processRemoveXInternalFromOperation`, `processSetTagsForAllOperations`, `processSetTagsToOperationId` com 100% dos desvios; `normalizeSchema` com 39/56).

#### Teste manual

Caso: regras do normalizador OpenAPI via CLI (oag-4, CTM-NO-01), documentado em texto.

- [Roteiro do caso](ludwig/testes-manuais/CTM-NO-01.md)
- [Especificação de importação do TestLink](ludwig/testes-manuais/oag-4-normalizer.xml)
- [Arquivos de entrada](ludwig/testes-manuais/entradas/)
- [Evidências de execução](ludwig/testes-manuais/evidencias/)

Resultado: **PASS — 6/6 passos executados com sucesso**.

#### Issues

Nenhuma divergência observada; nenhuma issue aberta.

## TestLink

- [Resultados do plano de testes](testlink/resultados-testlink.xls) (relatório "Test Results Flat" exportado em 13/10/2026 com as linhas oag-1, oag-2, oag-3 e oag-4)
