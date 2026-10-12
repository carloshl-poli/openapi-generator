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

Resultado da suíte: **48 casos executados (68 invocações JUnit): 43 PASS e 5 FAIL**.

Cobertura JaCoCo da classe: 97% das instruções e 84% dos desvios.

#### Teste manual

Caso: exemplos de resposta na documentação HTML gerada (oag-3, CTM-EG-01).

- [Roteiro do caso](hygor/testes-manuais/CTM-EG-01.md)
- [Caso para o TestLink (XML)](hygor/testes-manuais/oag-3-exemplos-html.xml)
- [Arquivos de entrada](hygor/testes-manuais/entradas/)
- [Evidências de execução](hygor/testes-manuais/evidencias/)

Resultado: **FAIL: 7/7 passos executados; 4 PASS e 3 FAIL (passos 4, 5 e 6)**.

#### Issues

- [Texto das issues](hygor/issues-ExampleGenerator.md)
- F1 #NNNN: valores numéricos gerados violam `maximum` negativo e `exclusiveMinimum` (CT-EG-24, CT-EG-25, passo 4 de oag-3).
- F2 #NNNN: arrays gerados ignoram `minItems` (CT-EG-29, passo 5 de oag-3).
- F3 #NNNN: chaves de um `example` objeto viram tipos de mídia em `generateFromResponseSchema` (CT-EG-45).
- F4 #NNNN: `type: object` sem propriedades gera a string `"{}"` (CT-EG-48, passo 6 de oag-3).

## TestLink

- [Resultados do plano de testes](testlink/resultados-testlink.xls)
