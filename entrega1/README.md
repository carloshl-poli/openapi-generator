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

## TestLink

- [Resultados do plano de testes](testlink/resultados-testlink.xls)
