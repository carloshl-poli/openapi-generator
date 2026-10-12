package org.openapitools.codegen.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openapitools.codegen.config.MergedSpecBuilder.MergeConflictStrategy;
import org.openapitools.codegen.config.MergedSpecBuilder.MergeMode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para {@link MergedSpecBuilder}.
 * <p>
 * Os casos CT-MSB-01 a CT-MSB-28 cobrem as regras de oráculo R01 a R14 do plano de testes
 * da disciplina COS482 - Qualidade de Software.
 */
public class MergedSpecBuilderJUnitTest {

    // ---------------------------------------------------------------- helpers

    private static OpenAPI emptySpec(String version) {
        OpenAPI spec = new OpenAPI();
        spec.openapi(version);
        spec.info(new Info().title("Test Spec").version("1.0.0").description("Spec for unit testing"));
        spec.paths(new Paths());
        spec.components(new Components());
        return spec;
    }

    private static Operation op(String operationId) {
        return new Operation()
                .operationId(operationId)
                .responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("OK")));
    }

    private static PathItem pathItem(PathItem.HttpMethod method, Operation operation) {
        PathItem item = new PathItem();
        item.operation(method, operation);
        return item;
    }

    private static MergedSpecBuilder builder(MergeMode mode, MergeConflictStrategy strategy) {
        return new MergedSpecBuilder(Collections.emptyList(), "target/test-output", "_merged_spec")
                .withMergeMode(mode)
                .withConflictStrategy(strategy);
    }

    private static Path writeSpecFile(Path dir, String fileName, String content) throws IOException {
        Path file = dir.resolve(fileName);
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    // ------------------------------------------------- R01: validação de entradas

    @Nested
    @DisplayName("R01 - Validação de entradas e diretórios")
    class InputValidation {

        @Test
        @DisplayName("CT-MSB-01: lista vazia de arquivos de especificação lança RuntimeException")
        void emptyInputFileListThrowsException(@TempDir Path tempDir) {
            MergedSpecBuilder builder = new MergedSpecBuilder(Collections.emptyList(), tempDir.toString(), "_merged");
            RuntimeException ex = assertThrows(RuntimeException.class, builder::buildMergedSpec);
            assertTrue(ex.getMessage().contains("inputSpecFiles list is empty"),
                    "Mensagem esperada deve informar lista vazia: " + ex.getMessage());
        }

        @Test
        @DisplayName("CT-MSB-02: diretório de especificações vazio lança RuntimeException")
        void emptyDirectoryThrowsException(@TempDir Path tempDir) {
            MergedSpecBuilder builder = new MergedSpecBuilder(tempDir.toString(), "_merged");
            RuntimeException ex = assertThrows(RuntimeException.class, builder::buildMergedSpec);
            assertTrue(ex.getMessage().contains("Spec directory doesn't contain any specification"),
                    "Mensagem esperada deve indicar ausência de especificações: " + ex.getMessage());
        }

        @Test
        @DisplayName("CT-MSB-03: diretório contendo apenas extensões não suportadas lança RuntimeException")
        void directoryWithUnsupportedExtensionsIgnored(@TempDir Path tempDir) throws IOException {
            writeSpecFile(tempDir, "notes.txt", "Texto irrelevante");
            writeSpecFile(tempDir, "schema.xml", "<xml>schema</xml>");

            MergedSpecBuilder builder = new MergedSpecBuilder(tempDir.toString(), "_merged");
            RuntimeException ex = assertThrows(RuntimeException.class, builder::buildMergedSpec);
            assertTrue(ex.getMessage().contains("Spec directory doesn't contain any specification"),
                    "Extensões não suportadas devem ser ignoradas: " + ex.getMessage());
        }
    }

    // ------------------------------------------------- R02, R03: Modos REF e DEEP

    @Nested
    @DisplayName("R02/R03 - Modos de fusão REF e DEEP")
    class MergeModes {

        @Test
        @DisplayName("CT-MSB-04: MergeMode.REF gera especificação leve com $ref relativo aos arquivos originais")
        void refModeGeneratesRelativeRefs(@TempDir Path tempDir) throws IOException {
            String yaml1 = "openapi: 3.0.3\n"
                    + "info:\n  title: Spec 1\n  version: 1.0.0\n"
                    + "paths:\n  /users:\n    get:\n      responses:\n        '200':\n          description: ok\n";
            String yaml2 = "openapi: 3.0.3\n"
                    + "info:\n  title: Spec 2\n  version: 1.0.0\n"
                    + "paths:\n  /orders:\n    get:\n      responses:\n        '200':\n          description: ok\n";

            writeSpecFile(tempDir, "spec1.yaml", yaml1);
            writeSpecFile(tempDir, "spec2.yaml", yaml2);

            MergedSpecBuilder builder = new MergedSpecBuilder(tempDir.toString(), "_merged_ref")
                    .withMergeMode(MergeMode.REF);

            String mergedPath = builder.buildMergedSpec();
            File mergedFile = new File(mergedPath);
            assertTrue(mergedFile.exists(), "Arquivo mesclado deve existir");

            String content = Files.readString(mergedFile.toPath());
            assertTrue(content.contains("$ref"), "Modo REF deve conter referências $ref");
            assertTrue(content.contains("spec1.yaml") && content.contains("spec2.yaml"),
                    "Modo REF deve referenciar os arquivos originais");
        }

        @Test
        @DisplayName("CT-MSB-05: MergeMode.REF inclui cabeçalho info e agrega servidores declarados")
        void refModeIncludesInfoAndServers(@TempDir Path tempDir) throws IOException {
            String yaml = "openapi: 3.0.3\n"
                    + "info:\n  title: Service A\n  version: 1.0.0\n"
                    + "servers:\n  - url: https://api.service-a.com\n"
                    + "paths:\n  /ping:\n    get:\n      responses:\n        '200':\n          description: ok\n";
            writeSpecFile(tempDir, "api.yaml", yaml);

            MergedSpecBuilder builder = new MergedSpecBuilder(tempDir.toString(), "_merged_info",
                    "Custom Title", "Custom Description", "2.5.0", null)
                    .withMergeMode(MergeMode.REF);

            String mergedPath = builder.buildMergedSpec();
            String content = Files.readString(Path.of(mergedPath));

            assertTrue(content.contains("Custom Title"), "Título customizado deve estar presente");
            assertTrue(content.contains("2.5.0"), "Versão customizada deve estar presente");
            assertTrue(content.contains("https://api.service-a.com"), "Servidor declarado deve ser preservado");
        }

        @Test
        @DisplayName("CT-MSB-06: MergeMode.DEEP inlines caminhos e componentes sem referências externas")
        void deepModeInlinesPathsAndComponents(@TempDir Path tempDir) throws IOException {
            String yaml1 = "openapi: 3.0.3\n"
                    + "info:\n  title: Users API\n  version: 1.0.0\n"
                    + "paths:\n  /users:\n    get:\n      responses:\n        '200':\n          description: ok\n"
                    + "components:\n  schemas:\n    User:\n      type: object\n      properties:\n        id:\n          type: integer\n";
            String yaml2 = "openapi: 3.0.3\n"
                    + "info:\n  title: Products API\n  version: 1.0.0\n"
                    + "paths:\n  /products:\n    get:\n      responses:\n        '200':\n          description: ok\n"
                    + "components:\n  schemas:\n    Product:\n      type: object\n      properties:\n        sku:\n          type: string\n";

            writeSpecFile(tempDir, "users.yaml", yaml1);
            writeSpecFile(tempDir, "products.yaml", yaml2);

            MergedSpecBuilder builder = new MergedSpecBuilder(tempDir.toString(), "_merged_deep")
                    .withMergeMode(MergeMode.DEEP);

            String mergedPath = builder.buildMergedSpec();
            File mergedFile = new File(mergedPath);
            assertTrue(mergedFile.exists());

            String content = Files.readString(mergedFile.toPath());
            assertFalse(content.contains("./users.yaml"), "Modo DEEP não deve conter caminhos externos relativos");
            assertTrue(content.contains("/users") && content.contains("/products"), "Ambos os paths devem estar inlined");
            assertTrue(content.contains("User") && content.contains("Product"), "Ambos os schemas devem estar inlined");
        }

        @Test
        @DisplayName("CT-MSB-07: MergeMode.DEEP detecta formato JSON quando primeiro arquivo é .json")
        void deepModeGeneratesJsonWhenFirstFileIsJson(@TempDir Path tempDir) throws IOException {
            String json1 = "{\n"
                    + "  \"openapi\": \"3.0.3\",\n"
                    + "  \"info\": { \"title\": \"API JSON\", \"version\": \"1.0.0\" },\n"
                    + "  \"paths\": { \"/test\": { \"get\": { \"responses\": { \"200\": { \"description\": \"ok\" } } } } }\n"
                    + "}";
            Path file1 = writeSpecFile(tempDir, "first.json", json1);

            MergedSpecBuilder builder = new MergedSpecBuilder(List.of(file1.toString()), tempDir.toString(), "_merged_json")
                    .withMergeMode(MergeMode.DEEP);

            String mergedPath = builder.buildMergedSpec();
            assertTrue(mergedPath.endsWith(".json"), "Nome do arquivo gerado deve ter extensão .json");

            // Verifica se é JSON sintaticamente válido
            ObjectMapper mapper = new ObjectMapper();
            assertDoesNotThrow(() -> mapper.readTree(new File(mergedPath)));
        }
    }

    // ------------------------------------------------- R04, R05, R06: Versões OpenAPI

    @Nested
    @DisplayName("R04/R05/R06 - Resolução e validação de versão OpenAPI")
    class VersionResolution {

        @Test
        @DisplayName("CT-MSB-08: Versão malformada com caracteres não numéricos é rejeitada")
        void malformedVersionWithLettersRejected() {
            OpenAPI spec1 = emptySpec("3.0.3");
            OpenAPI spec2 = emptySpec("3.x.3");

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList()));
            assertTrue(ex.getMessage().contains("Malformed OpenAPI version '3.x.3'"),
                    "Deve acusar versão malformada: " + ex.getMessage());
        }

        @Test
        @DisplayName("CT-MSB-09: Versão malformada com dois segmentos ou zeros à esquerda é rejeitada")
        void malformedVersionMissingPatchOrLeadingZeroRejected() {
            OpenAPI spec = emptySpec("3.01.0");

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> builder.mergeSpecs(List.of(spec), Collections.emptyList()));
            assertTrue(ex.getMessage().contains("Malformed OpenAPI version"),
                    "Deve rejeitar versão com zero à esquerda: " + ex.getMessage());
        }

        @Test
        @DisplayName("CT-MSB-10: Mistura de versões maior/menor incompatíveis (3.0.x e 3.1.x) lança RuntimeException")
        void incompatibleMajorMinorVersionsRejected() {
            OpenAPI spec1 = emptySpec("3.0.3");
            OpenAPI spec2 = emptySpec("3.1.0");

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList()));
            assertTrue(ex.getMessage().contains("incompatible OpenAPI versions"),
                    "Deve acusar versões incompatíveis: " + ex.getMessage());
        }

        @Test
        @DisplayName("CT-MSB-11: Mistura de especificação versionada com especificação sem versão lança RuntimeException")
        void mixedVersionedAndUnversionedRejected() {
            OpenAPI spec1 = emptySpec("3.0.3");
            OpenAPI spec2 = emptySpec(null);

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList()));
            assertTrue(ex.getMessage().contains("The 'openapi' field is required"),
                    "Deve exigir que todas declarem versão: " + ex.getMessage());
        }

        @Test
        @DisplayName("CT-MSB-12: Especificações compatíveis adotam a maior versão de patch encontrada")
        void highestPatchVersionAdopted() {
            OpenAPI spec1 = emptySpec("3.0.1");
            OpenAPI spec2 = emptySpec("3.0.3");
            OpenAPI spec3 = emptySpec("3.0.2");

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            OpenAPI merged = builder.mergeSpecs(List.of(spec1, spec2, spec3), Collections.emptyList());

            assertEquals("3.0.3", merged.getOpenapi(), "Deve adotar o maior patch disponível (3.0.3)");
        }

        @Test
        @DisplayName("CT-MSB-13: Quando todas as especificações possuem versão nula, assume o padrão 3.0.3")
        void allUnversionedDefaultsTo303() {
            OpenAPI spec1 = emptySpec(null);
            OpenAPI spec2 = emptySpec(null);

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            OpenAPI merged = builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList());

            assertEquals("3.0.3", merged.getOpenapi(), "Padrão deve ser 3.0.3 quando todas forem nulas");
        }
    }

    // ------------------------------------------------- R07, R08: Fusão de Rotas e Métodos HTTP

    @Nested
    @DisplayName("R07/R08 - Fusão de rotas e métodos HTTP")
    class PathAndMethodMerging {

        @Test
        @DisplayName("CT-MSB-14: Métodos HTTP distintos na mesma URL são combinados no mesmo PathItem")
        void distinctHttpMethodsOnSamePathCombined() {
            OpenAPI spec1 = emptySpec("3.0.3");
            spec1.getPaths().addPathItem("/users", pathItem(PathItem.HttpMethod.GET, op("getUsers")));

            OpenAPI spec2 = emptySpec("3.0.3");
            spec2.getPaths().addPathItem("/users", pathItem(PathItem.HttpMethod.POST, op("createUser")));

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            OpenAPI merged = builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList());

            PathItem mergedPath = merged.getPaths().get("/users");
            assertNotNull(mergedPath);
            assertNotNull(mergedPath.getGet(), "Método GET deve estar presente");
            assertNotNull(mergedPath.getPost(), "Método POST deve estar presente");
            assertEquals("getUsers", mergedPath.getGet().getOperationId());
            assertEquals("createUser", mergedPath.getPost().getOperationId());
        }

        @Test
        @DisplayName("CT-MSB-15: Rotas completamente distintas são todas preservadas no documento mesclado")
        void distinctPathsPreserved() {
            OpenAPI spec1 = emptySpec("3.0.3");
            spec1.getPaths().addPathItem("/users", pathItem(PathItem.HttpMethod.GET, op("getUsers")));

            OpenAPI spec2 = emptySpec("3.0.3");
            spec2.getPaths().addPathItem("/orders", pathItem(PathItem.HttpMethod.GET, op("getOrders")));

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            OpenAPI merged = builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList());

            assertEquals(2, merged.getPaths().size());
            assertTrue(merged.getPaths().containsKey("/users"));
            assertTrue(merged.getPaths().containsKey("/orders"));
        }

        @Test
        @DisplayName("CT-MSB-16: Mesmo método HTTP na mesma rota com WARN mantém a primeira operação")
        void duplicateHttpMethodWithWarnKeepsFirst() {
            OpenAPI spec1 = emptySpec("3.0.3");
            spec1.getPaths().addPathItem("/users", pathItem(PathItem.HttpMethod.GET, op("getUsersV1")));

            OpenAPI spec2 = emptySpec("3.0.3");
            spec2.getPaths().addPathItem("/users", pathItem(PathItem.HttpMethod.GET, op("getUsersV2")));

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            OpenAPI merged = builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList());

            PathItem mergedPath = merged.getPaths().get("/users");
            assertEquals("getUsersV1", mergedPath.getGet().getOperationId(),
                    "Em caso de conflito sob WARN, a primeira definição deve prevalecer");
        }

        @Test
        @DisplayName("CT-MSB-17: Mesmo método HTTP na mesma rota com FAIL lança RuntimeException")
        void duplicateHttpMethodWithFailThrowsException() {
            OpenAPI spec1 = emptySpec("3.0.3");
            spec1.getPaths().addPathItem("/users", pathItem(PathItem.HttpMethod.GET, op("getUsersV1")));

            OpenAPI spec2 = emptySpec("3.0.3");
            spec2.getPaths().addPathItem("/users", pathItem(PathItem.HttpMethod.GET, op("getUsersV2")));

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.FAIL);
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList()));
            assertTrue(ex.getMessage().contains("Path+method conflict during spec merge"),
                    "Sob FAIL deve abortar informando conflito de método e path: " + ex.getMessage());
        }
    }

    // ------------------------------------------------- R09: Conflito de operationId

    @Nested
    @DisplayName("R09 - Resolução de conflitos de operationId")
    class OperationIdConflict {

        @Test
        @DisplayName("CT-MSB-18: operationId duplicado sob WARN é renomeado com sufixo _2")
        void duplicateOperationIdRenamedWithSuffix2UnderWarn() {
            OpenAPI spec1 = emptySpec("3.0.3");
            spec1.getPaths().addPathItem("/users", pathItem(PathItem.HttpMethod.GET, op("findItem")));

            OpenAPI spec2 = emptySpec("3.0.3");
            spec2.getPaths().addPathItem("/orders", pathItem(PathItem.HttpMethod.GET, op("findItem")));

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            OpenAPI merged = builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList());

            Operation op1 = merged.getPaths().get("/users").getGet();
            Operation op2 = merged.getPaths().get("/orders").getGet();

            assertEquals("findItem", op1.getOperationId(), "Primeira operação deve manter seu ID original");
            assertEquals("findItem_2", op2.getOperationId(), "Segunda operação deve receber o sufixo _2");
        }

        @Test
        @DisplayName("CT-MSB-19: Três operações com mesmo operationId sob WARN são renomeadas para _2 e _3")
        void multipleOperationIdCollisionsIncrementSuffix() {
            OpenAPI spec1 = emptySpec("3.0.3");
            spec1.getPaths().addPathItem("/p1", pathItem(PathItem.HttpMethod.GET, op("search")));

            OpenAPI spec2 = emptySpec("3.0.3");
            spec2.getPaths().addPathItem("/p2", pathItem(PathItem.HttpMethod.GET, op("search")));

            OpenAPI spec3 = emptySpec("3.0.3");
            spec3.getPaths().addPathItem("/p3", pathItem(PathItem.HttpMethod.GET, op("search")));

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            OpenAPI merged = builder.mergeSpecs(List.of(spec1, spec2, spec3), Collections.emptyList());

            assertEquals("search", merged.getPaths().get("/p1").getGet().getOperationId());
            assertEquals("search_2", merged.getPaths().get("/p2").getGet().getOperationId());
            assertEquals("search_3", merged.getPaths().get("/p3").getGet().getOperationId());
        }

        @Test
        @DisplayName("CT-MSB-20: operationId duplicado sob FAIL lança RuntimeException")
        void duplicateOperationIdThrowsUnderFail() {
            OpenAPI spec1 = emptySpec("3.0.3");
            spec1.getPaths().addPathItem("/users", pathItem(PathItem.HttpMethod.GET, op("getItem")));

            OpenAPI spec2 = emptySpec("3.0.3");
            spec2.getPaths().addPathItem("/products", pathItem(PathItem.HttpMethod.GET, op("getItem")));

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.FAIL);
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList()));
            assertTrue(ex.getMessage().contains("operationId conflict during spec merge"),
                    "Deve abortar acusando colisão de operationId: " + ex.getMessage());
        }
    }

    // ------------------------------------------------- R10, R11: Fusão de Componentes

    @Nested
    @DisplayName("R10/R11 - Fusão e conflitos de componentes")
    class ComponentMerging {

        @Test
        @DisplayName("CT-MSB-21: Componentes com mesmo nome e estrutura idêntica são silenciosamente deduplicados")
        void identicalComponentsSilentlyDeduplicated() {
            Schema<?> schema1 = new ObjectSchema().addProperty("id", new StringSchema());
            Schema<?> schema2 = new ObjectSchema().addProperty("id", new StringSchema());

            OpenAPI spec1 = emptySpec("3.0.3");
            spec1.getComponents().addSchemas("Model", schema1);

            OpenAPI spec2 = emptySpec("3.0.3");
            spec2.getComponents().addSchemas("Model", schema2);

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.FAIL);
            OpenAPI merged = builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList());

            assertEquals(1, merged.getComponents().getSchemas().size());
            assertNotNull(merged.getComponents().getSchemas().get("Model"));
        }

        @Test
        @DisplayName("CT-MSB-22: Componentes com mesmo nome e definições distintas sob WARN mantêm o primeiro")
        void conflictingComponentsUnderWarnKeepsFirst() {
            Schema<?> schemaA = new ObjectSchema().addProperty("name", new StringSchema());
            Schema<?> schemaB = new ObjectSchema().addProperty("price", new StringSchema());

            OpenAPI spec1 = emptySpec("3.0.3");
            spec1.getComponents().addSchemas("Item", schemaA);

            OpenAPI spec2 = emptySpec("3.0.3");
            spec2.getComponents().addSchemas("Item", schemaB);

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            OpenAPI merged = builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList());

            Schema<?> mergedSchema = merged.getComponents().getSchemas().get("Item");
            assertTrue(mergedSchema.getProperties().containsKey("name"), "Primeira definição deve ser mantida");
            assertFalse(mergedSchema.getProperties().containsKey("price"), "Segunda definição deve ser descartada");
        }

        @Test
        @DisplayName("CT-MSB-23: Componentes com mesmo nome e definições distintas sob FAIL lançam RuntimeException")
        void conflictingComponentsUnderFailThrowsException() {
            Schema<?> schemaA = new ObjectSchema().addProperty("code", new StringSchema());
            Schema<?> schemaB = new ObjectSchema().addProperty("amount", new StringSchema());

            OpenAPI spec1 = emptySpec("3.0.3");
            spec1.getComponents().addSchemas("Account", schemaA);

            OpenAPI spec2 = emptySpec("3.0.3");
            spec2.getComponents().addSchemas("Account", schemaB);

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.FAIL);
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList()));
            assertTrue(ex.getMessage().contains("Component schema name conflict"),
                    "Deve abortar acusando conflito de schema: " + ex.getMessage());
        }

        @Test
        @DisplayName("CT-MSB-24: Componentes de tipos distintos (securitySchemes e parameters) são preservados")
        void distinctComponentTypesMerged() {
            OpenAPI spec1 = emptySpec("3.0.3");
            spec1.getComponents().addSecuritySchemes("BearerAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer"));

            OpenAPI spec2 = emptySpec("3.0.3");
            spec2.getComponents().addParameters("PageParam", new QueryParameter().name("page"));

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            OpenAPI merged = builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList());

            assertNotNull(merged.getComponents().getSecuritySchemes().get("BearerAuth"));
            assertNotNull(merged.getComponents().getParameters().get("PageParam"));
        }
    }

    // ------------------------------------------------- R12, R13, R14: Segurança, Parâmetros e Metadados

    @Nested
    @DisplayName("R12/R13/R14 - Segurança, parâmetros e metadados")
    class SecurityAndMetadata {

        @Test
        @DisplayName("CT-MSB-25: Requisito de segurança global é propagado para operações filhas sem autenticação")
        void rootSecurityPropagatedToOperationsWithoutSecurity() {
            OpenAPI spec = emptySpec("3.0.3");
            spec.addSecurityItem(new SecurityRequirement().addList("OAuth2", List.of("read", "write")));

            Operation operation = op("secureOp");
            assertNull(operation.getSecurity(), "Operação inicialmente sem security");
            spec.getPaths().addPathItem("/protected", pathItem(PathItem.HttpMethod.GET, operation));

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            OpenAPI merged = builder.mergeSpecs(List.of(spec), Collections.emptyList());

            Operation resultOp = merged.getPaths().get("/protected").getGet();
            assertNotNull(resultOp.getSecurity(), "Operação filha deve ter recebido a segurança propagada");
            assertEquals(1, resultOp.getSecurity().size());
            assertTrue(resultOp.getSecurity().get(0).containsKey("OAuth2"));
        }

        @Test
        @DisplayName("CT-MSB-26: Operação com segurança explícita vazia não é sobrescrita pela segurança global")
        void explicitEmptySecurityNotOverwrittenByRootSecurity() {
            OpenAPI spec = emptySpec("3.0.3");
            spec.addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"));

            Operation publicOp = op("publicOp");
            publicOp.setSecurity(new ArrayList<>()); // explicitamente desabilitada
            spec.getPaths().addPathItem("/public", pathItem(PathItem.HttpMethod.GET, publicOp));

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            OpenAPI merged = builder.mergeSpecs(List.of(spec), Collections.emptyList());

            Operation resultOp = merged.getPaths().get("/public").getGet();
            assertNotNull(resultOp.getSecurity());
            assertTrue(resultOp.getSecurity().isEmpty(), "Segurança explícita vazia deve ser preservada");
        }

        @Test
        @DisplayName("CT-MSB-27: Parâmetros de rota são deduplicados por $ref e por chave name:in")
        void pathParametersDeduplicatedByRefAndNameIn() {
            PathItem item1 = new PathItem();
            item1.setParameters(new ArrayList<>(List.of(
                    new Parameter().$ref("#/components/parameters/UserId"),
                    new PathParameter().name("subId")
            )));
            item1.operation(PathItem.HttpMethod.GET, op("getWithParams"));

            PathItem item2 = new PathItem();
            item2.setParameters(new ArrayList<>(List.of(
                    new Parameter().$ref("#/components/parameters/UserId"), // duplicado por ref
                    new PathParameter().name("subId"),                     // duplicado por name:in
                    new QueryParameter().name("filter")                    // novo
            )));
            item2.operation(PathItem.HttpMethod.POST, op("postWithParams"));

            OpenAPI spec1 = emptySpec("3.0.3");
            spec1.getPaths().addPathItem("/users/{id}", item1);

            OpenAPI spec2 = emptySpec("3.0.3");
            spec2.getPaths().addPathItem("/users/{id}", item2);

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            OpenAPI merged = builder.mergeSpecs(List.of(spec1, spec2), Collections.emptyList());

            List<Parameter> mergedParams = merged.getPaths().get("/users/{id}").getParameters();
            assertEquals(3, mergedParams.size(), "Deve conter exatamente 3 parâmetros únicos");
        }

        @Test
        @DisplayName("CT-MSB-28: Servidores com URLs distintas são agregados e com mesma URL são deduplicados")
        void serversDeduplicatedByUrlAndExtensionsMerged() {
            OpenAPI spec1 = emptySpec("3.0.3");
            spec1.addTagsItem(new Tag().name("Accounts").description("First"));
            spec1.addExtension("x-api-owner", "Team-A");

            OpenAPI spec2 = emptySpec("3.0.3");
            spec2.addTagsItem(new Tag().name("Accounts").description("Second"));
            spec2.addExtension("x-api-owner", "Team-B"); // conflito de chave
            spec2.addExtension("x-release-stage", "beta");

            List<Server> servers = List.of(
                    new Server().url("https://api.example.com"),
                    new Server().url("https://api.example.com"), // duplicado
                    new Server().url("https://beta.example.com")
            );

            MergedSpecBuilder builder = builder(MergeMode.DEEP, MergeConflictStrategy.WARN);
            OpenAPI merged = builder.mergeSpecs(List.of(spec1, spec2), servers);

            assertEquals(2, merged.getServers().size(), "Servidores duplicados devem ser desduplicados por URL");
            assertEquals(1, merged.getTags().size(), "Tags com mesmo nome devem ser desduplicadas");
            assertEquals("First", merged.getTags().get(0).getDescription(), "Primeira definição de tag deve prevalecer");
            assertEquals("Team-A", merged.getExtensions().get("x-api-owner"), "Primeira extensão x- deve prevalecer");
            assertEquals("beta", merged.getExtensions().get("x-release-stage"), "Nova extensão x- deve ser incorporada");
        }
    }
}

