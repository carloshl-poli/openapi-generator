# Caso de teste manual CTM-MSB-01

**Sistema:** OpenAPI Generator v7.25.0 (fork). Commit `ef964b04480889ef86b56cfae84ade8ad4c91c41`.  
**Funcionalidade:** consolidação de múltiplas especificações OpenAPI e geração de código via CLI:  
`openapi-generator-cli generate -g <gerador> --input-spec-files <arquivos> --merged-file-output-dir <pasta> --merged-file-name <nome> --merge-mode <DEEP|REF> --merge-conflict-strategy <WARN|FAIL> -o <saída>`.  
**Unidade relacionada:** `org.openapitools.codegen.config.MergedSpecBuilder`, instanciada e configurada por `org.openapitools.codegen.cmd.Generate` na CLI para consolidar os arquivos de entrada antes de repassar a especificação unificada para o pipeline de geração.

**Objetivo:** verificar, no nível do sistema, se a mescla de especificações via CLI:
- consolida corretamente múltiplos arquivos válidos em um único documento (`_merged_spec.yaml`), combinando rotas e esquemas sem perda de dados;
- combina operações com métodos HTTP distintos que compartilham a mesma URL (`GET /users` da primeira spec e `POST /users` da segunda);
- deduplica componentes idênticos (`User`);
- respeita a estratégia de conflito `WARN` (mantendo a primeira definição e emitindo alerta) e `FAIL` (abortando o processo) diante de métodos HTTP duplicados;
- rejeita a mistura de especificações com versões maiores/menores incompatíveis (OpenAPI 3.0 vs 3.1);
- gera com sucesso o código-fonte final a partir da especificação unificada resultante.

Os resultados esperados derivam das regras R01 a R14 da planilha de oráculos e da documentação do comando `generate` da CLI.

---

## Informações sobre a execução do teste

| Campo | Valor |
|---|---|
| Nome do testador | Kauã Gomes |
| Data(s) do teste | 12/10/2026 |
| Local / máquina em uso | Máquina local, Windows 11 / Terminal PowerShell |
| Pré-requisitos | JDK instalado (OpenJDK 17 ou 21); fork clonado na baseline v7.25.0; arquivos de teste da pasta `entradas/` |
| Versões de software | OpenAPI Generator CLI 7.25.0; Java OpenJDK; terminal |
| Configuração necessária | Variável de ambiente `J` apontando para o JAR da CLI gerado pelo Maven |

### Preparação (executar uma vez antes dos passos)

1. Gerar o JAR executável da CLI a partir da raiz do fork:
   ```bash
   ./mvnw.cmd -pl modules/openapi-generator-cli -am package -DskipTests
   ```
2. Definir a variável para o JAR da CLI:
   ```bash
   $J = "modules/openapi-generator-cli/target/openapi-generator-cli.jar"
   ```

### Arquivos de entrada (pasta `entradas/`)

| Arquivo | Conteúdo relevante |
|---|---|
| `spec1-users.yaml` | OpenAPI 3.0.3; rota `GET /users` (`operationId: getUsers`); modelo `User` |
| `spec2-orders.yaml` | OpenAPI 3.0.3; rota `POST /users` (`createUser`), rota `GET /orders` (`getOrders`); modelos `User` (idêntico) e `Order` |
| `spec-conflito-metodo.yaml` | OpenAPI 3.0.3; rota `GET /users` (`duplicateGetUsers`), colidindo com `spec1-users.yaml` |
| `spec-incompativel-31.yaml` | OpenAPI 3.1.0; versão incompatível com OpenAPI 3.0.3 |

---

## Etapas / resultados do script de teste

| Passo | Etapa / entrada do teste | Resultados esperados | Resultados reais | Requisitos validados | Passou / Falhou |
|---|---|---|---|---|---|
| 1 | `java -jar $J version` | Código de saída 0; imprime `7.25.0` | Código de saída 0; imprime `7.25.0` | Ambiente funcional | PASSOU |
| 2 | `java -jar $J generate -g java --input-spec-files entrega1/kaua/testes-manuais/entradas/spec1-users.yaml --input-spec-files entrega1/kaua/testes-manuais/entradas/spec2-orders.yaml --merged-file-output-dir target/manual-test/merged --merged-spec-filename api_unificada.yaml --merge-mode DEEP -o target/manual-test/client` | Código de saída 0; arquivo `api_unificada.yaml` criado contendo `/users` com `get` e `post`, `/orders` com `get`, e schemas `User` e `Order` inlined; código Java gerado | Código de saída 0; arquivo `api_unificada.yaml` gerado com todas as rotas e componentes; cliente Java gerado | R03, R07, R10 | PASSOU |
| 3 | Inspecionar classes geradas em `target/manual-test/client/src/main/java/org/openapitools/client/model/` | Arquivos `User.java` e `Order.java` gerados; modelo `User` não duplicado | `User.java` e `Order.java` presentes e válidos | R03, R10 | PASSOU |
| 4 | `java -jar $J generate -g java --input-spec-files entrega1/kaua/testes-manuais/entradas/spec1-users.yaml --input-spec-files entrega1/kaua/testes-manuais/entradas/spec2-orders.yaml --merged-file-output-dir target/manual-test/merged-ref --merged-spec-filename api_ref.yaml --merge-mode REF -o target/manual-test/client-ref` | Código de saída 0; arquivo `api_ref.yaml` criado contendo `$ref` apontando relativamente para `spec1-users.yaml` e `spec2-orders.yaml` | Código de saída 0; arquivo `api_ref.yaml` gerado com referências `$ref` relativas | R02 | PASSOU |
| 5 | `java -jar $J generate -g java --input-spec-files entrega1/kaua/testes-manuais/entradas/spec1-users.yaml --input-spec-files entrega1/kaua/testes-manuais/entradas/spec-conflito-metodo.yaml --merged-file-output-dir target/manual-test/merged-warn --merge-mode DEEP --merge-conflict-strategy WARN -o target/manual-test/client-warn` | Código de saída 0; aviso no log informando `Path+method conflict during spec merge: GET /users is defined in multiple specs. Keeping the first definition.`; primeira operação mantida | Código de saída 0; warning emitido no console e primeira operação preservada | R08 | PASSOU |
| 6 | `java -jar $J generate -g java --input-spec-files entrega1/kaua/testes-manuais/entradas/spec1-users.yaml --input-spec-files entrega1/kaua/testes-manuais/entradas/spec-conflito-metodo.yaml --merged-file-output-dir target/manual-test/merged-fail --merge-mode DEEP --merge-conflict-strategy FAIL -o target/manual-test/client-fail` | Código de saída diferente de 0; aborta com exceção informando `Path+method conflict during spec merge` | Execução abortada com erro e código diferente de 0 | R08 | PASSOU |
| 7 | `java -jar $J generate -g java --input-spec-files entrega1/kaua/testes-manuais/entradas/spec1-users.yaml --input-spec-files entrega1/kaua/testes-manuais/entradas/spec-incompativel-31.yaml --merged-file-output-dir target/manual-test/merged-incompatible --merge-mode DEEP -o target/manual-test/client-incompatible` | Código de saída diferente de 0; aborta com erro acusando versões incompatíveis (`Cannot merge specs that declare incompatible OpenAPI versions`) | Aborta acusando incompatibilidade de versões entre 3.0 e 3.1 | R05 | PASSOU |

**Resultado geral do caso:** PASSOU. Todos os 7 passos foram executados com sucesso de ponta a ponta, comprovando a conformidade de `MergedSpecBuilder` e da interface CLI com as regras especificadas.

---

## Registro

Caso documentado em texto, conforme padrão adotado pelo grupo. O arquivo [`oag-3-merged-spec-cli.xml`](oag-3-merged-spec-cli.xml) traz o mesmo caso no formato de importação do TestLink (suíte "Kauã - Mescla de especificações via CLI").

Execução: 12/10/2026, build OpenAPI Generator v7.25.0, resultado **PASSOU** (todos os 7 passos em conformidade).

Evidências geradas:
- [`evidencias/api_unificada.yaml`](evidencias/api_unificada.yaml): especificação consolidada gerada no passo 2 com deduplicação de schemas e mescla de rotas;
- [`evidencias/api_ref.yaml`](evidencias/api_ref.yaml): especificação consolidada gerada no passo 4 utilizando apontadores `$ref` relativos;
- [`evidencias/log-passo2-merge-deep.txt`](evidencias/log-passo2-merge-deep.txt): log da CLI registrando a mescla profunda e geração do cliente Java;
- [`evidencias/log-passo4-merge-ref.txt`](evidencias/log-passo4-merge-ref.txt): log da CLI registrando a mescla por referência;
- [`evidencias/log-passo5-merge-warn.txt`](evidencias/log-passo5-merge-warn.txt): log da CLI comprovando o alerta de colisão de rota/método e retenção da primeira definição;
- [`evidencias/log-passo6-merge-fail.txt`](evidencias/log-passo6-merge-fail.txt): log da CLI comprovando o lançamento de exceção e interrupção do processo no modo FAIL;
- [`evidencias/log-passo7-merge-incompativel.txt`](evidencias/log-passo7-merge-incompativel.txt): log da CLI comprovando a rejeição de mescla entre OpenAPI 3.0 e 3.1.

