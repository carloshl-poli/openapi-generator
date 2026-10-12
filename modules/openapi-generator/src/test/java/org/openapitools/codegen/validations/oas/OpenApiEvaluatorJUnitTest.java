package org.openapitools.codegen.validations.oas;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.validation.Invalid;
import org.openapitools.codegen.validation.ValidationResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OpenApiEvaluator#validate(OpenAPI)}.
 * <p>
 * Cases CT-OE-01..CT-OE-28 follow the oracle rules R01..R12 of the test plan.
 * {@link RuleConfiguration} is replaced by a Mockito mock, so each test enables only the rules it exercises.
 */
public class OpenApiEvaluatorJUnitTest {

    private static final String UNDERSCORE_HEADER = "X_Token";

    // ---------------------------------------------------------------- helpers

    /**
     * Mocked configuration: every flag not listed here returns false (Mockito default).
     */
    private static RuleConfiguration config(boolean recommendations, boolean unusedSchemas, boolean defaultNotInEnum,
                                            boolean apacheNginxUnderscore, boolean getWithBody) {
        RuleConfiguration config = mock(RuleConfiguration.class);
        when(config.isEnableRecommendations()).thenReturn(recommendations);
        when(config.isEnableUnusedSchemasRecommendation()).thenReturn(unusedSchemas);
        when(config.isEnableDefaultNotInEnumRecommendation()).thenReturn(defaultNotInEnum);
        when(config.isEnableApacheNginxUnderscoreRecommendation()).thenReturn(apacheNginxUnderscore);
        when(config.isEnableApiRequestUriWithBodyRecommendation()).thenReturn(getWithBody);
        return config;
    }

    private static RuleConfiguration onlyUnusedSchemas() {
        return config(true, true, false, false, false);
    }

    private static RuleConfiguration onlyDefaultNotInEnum() {
        return config(true, false, true, false, false);
    }

    private static RuleConfiguration onlyUnderscore() {
        return config(true, false, false, true, false);
    }

    private static RuleConfiguration onlyGetWithBody() {
        return config(true, false, false, false, true);
    }

    private static RuleConfiguration noRules() {
        return config(true, false, false, false, false);
    }

    private static List<Invalid> warningsWith(ValidationResult result, String text) {
        return result.getWarnings().stream()
                .filter(i -> i.getMessage().contains(text)
                        || (i.getDetails() != null && i.getDetails().contains(text)))
                .collect(Collectors.toList());
    }

    private static List<String> messages(ValidationResult result) {
        return result.getWarnings().stream().map(Invalid::getMessage).collect(Collectors.toList());
    }

    private static OpenAPI emptySpec() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.openapi("3.0.1");
        return openAPI;
    }

    private static OpenAPI specWithSchema(String name, Schema<?> schema) {
        OpenAPI openAPI = emptySpec();
        openAPI.setComponents(new Components().addSchemas(name, schema));
        return openAPI;
    }

    private static OpenAPI specWithEnumProperty(List<String> enumValues, String defaultValue) {
        StringSchema property = new StringSchema();
        property.setEnum(enumValues);
        property.setDefault(defaultValue);
        return specWithSchema("Config", new ObjectSchema().addProperty("protocol", property));
    }

    private static Content jsonContent(Schema<?> schema) {
        return new Content().addMediaType("application/json", new MediaType().schema(schema));
    }

    private static Operation operation(String operationId) {
        return new Operation().operationId(operationId).responses(new ApiResponses());
    }

    private static OpenAPI specWithPath(String path, PathItem pathItem) {
        OpenAPI openAPI = emptySpec();
        openAPI.setPaths(new Paths().addPathItem(path, pathItem));
        return openAPI;
    }

    private static OpenAPI specWithTags(String... names) {
        OpenAPI openAPI = emptySpec();
        List<Tag> tags = new ArrayList<>();
        for (String name : names) {
            tags.add(new Tag().name(name));
        }
        openAPI.setTags(tags);
        return openAPI;
    }

    private static Parameter underscoreHeader() {
        return new HeaderParameter().name(UNDERSCORE_HEADER).schema(new StringSchema());
    }

    // ---------------------------------------------------------------- R01 / R12: input and configuration

    @Test
    @DisplayName("CT-OE-01 especificação nula retorna resultado vazio sem consultar a configuração")
    void ct01NullSpecificationReturnsEmptyResult() {
        RuleConfiguration config = mock(RuleConfiguration.class);

        ValidationResult result = new OpenApiEvaluator(config).validate(null);

        assertNotNull(result);
        assertTrue(result.getAll().isEmpty());
        verifyNoInteractions(config);
    }

    @Test
    @DisplayName("CT-OE-02 especificação vazia com todas as regras ativas não gera erro nem aviso")
    void ct02EmptySpecificationWithAllRulesEnabled() {
        RuleConfiguration config = new RuleConfiguration();
        config.setEnableRecommendations(true);

        ValidationResult result = assertDoesNotThrow(() -> new OpenApiEvaluator(config).validate(emptySpec()));

        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
    }

    @Test
    @DisplayName("CT-OE-03 OpenAPI mockado sem paths, components e tags não lança exceção")
    void ct03MockedSpecificationWithoutSectionsIsHandled() {
        OpenAPI openAPI = mock(OpenAPI.class);
        RuleConfiguration config = config(true, true, true, true, true);

        ValidationResult result = assertDoesNotThrow(() -> new OpenApiEvaluator(config).validate(openAPI));

        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
        verify(openAPI, atLeastOnce()).getPaths();
        verify(openAPI, atLeastOnce()).getComponents();
        verify(openAPI, atLeastOnce()).getTags();
    }

    // ---------------------------------------------------------------- R02 / R03: unused schemas

    @Test
    @DisplayName("CT-OE-04 esquema não referenciado gera um aviso 'Unused model'")
    void ct04UnusedSchemaIsReported() {
        OpenAPI openAPI = specWithSchema("Orphan", new ObjectSchema());

        ValidationResult result = new OpenApiEvaluator(onlyUnusedSchemas()).validate(openAPI);

        List<Invalid> warnings = warningsWith(result, "Unused model");
        assertEquals(1, warnings.size());
        assertEquals("Unused model: Orphan", warnings.get(0).getMessage());
    }

    @Test
    @DisplayName("CT-OE-05 esquema referenciado por uma operação não gera aviso")
    void ct05ReferencedSchemaIsNotReported() {
        OpenAPI openAPI = specWithSchema("Pet", new ObjectSchema());
        ApiResponses responses = new ApiResponses().addApiResponse("200",
                new ApiResponse().description("ok").content(jsonContent(new Schema<>().$ref("#/components/schemas/Pet"))));
        openAPI.setPaths(new Paths().addPathItem("/pets",
                new PathItem().get(new Operation().operationId("getPet").responses(responses))));

        ValidationResult result = new OpenApiEvaluator(onlyUnusedSchemas()).validate(openAPI);

        assertTrue(warningsWith(result, "Unused model").isEmpty());
    }

    @Test
    @DisplayName("CT-OE-06 regra de esquema não utilizado desativada não gera aviso")
    void ct06UnusedSchemaRuleDisabled() {
        RuleConfiguration config = config(true, false, false, false, false);

        ValidationResult result = new OpenApiEvaluator(config).validate(specWithSchema("Orphan", new ObjectSchema()));

        assertTrue(warningsWith(result, "Unused model").isEmpty());
        verify(config).isEnableUnusedSchemasRecommendation();
    }

    @Test
    @DisplayName("CT-OE-07 recomendações desligadas globalmente não geram aviso de esquema não utilizado")
    void ct07GlobalRecommendationsOffSuppressUnusedSchema() {
        RuleConfiguration config = config(false, true, false, false, false);

        ValidationResult result = new OpenApiEvaluator(config).validate(specWithSchema("Orphan", new ObjectSchema()));

        assertTrue(warningsWith(result, "Unused model").isEmpty(),
                "unused-schema recommendation emitted while recommendations are disabled: " + messages(result));
    }

    // ---------------------------------------------------------------- R04 / R05 / R06: default not in enum

    @Test
    @DisplayName("CT-OE-08 default fora do enum gera um aviso com o valor e o enum")
    void ct08DefaultNotInEnumIsReported() {
        ValidationResult result = new OpenApiEvaluator(onlyDefaultNotInEnum())
                .validate(specWithEnumProperty(Arrays.asList("udp", "tcp"), "http"));

        List<Invalid> warnings = warningsWith(result, "not in enum");
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).getMessage().contains("'http'"));
        assertTrue(warnings.get(0).getMessage().contains("[udp, tcp]"));
    }

    @Test
    @DisplayName("CT-OE-09 default pertencente ao enum não gera aviso")
    void ct09DefaultInEnum() {
        ValidationResult result = new OpenApiEvaluator(onlyDefaultNotInEnum())
                .validate(specWithEnumProperty(Arrays.asList("udp", "tcp"), "tcp"));

        assertTrue(warningsWith(result, "not in enum").isEmpty());
    }

    @Test
    @DisplayName("CT-OE-10 enum vazio (limite) não gera aviso")
    void ct10EmptyEnum() {
        ValidationResult result = new OpenApiEvaluator(onlyDefaultNotInEnum())
                .validate(specWithEnumProperty(Collections.emptyList(), "http"));

        assertTrue(warningsWith(result, "not in enum").isEmpty());
    }

    @Test
    @DisplayName("CT-OE-11 default nulo (limite) não gera aviso")
    void ct11NullDefault() {
        ValidationResult result = new OpenApiEvaluator(onlyDefaultNotInEnum())
                .validate(specWithEnumProperty(Collections.singletonList("udp"), null));

        assertTrue(warningsWith(result, "not in enum").isEmpty());
    }

    @Test
    @DisplayName("CT-OE-12 recomendações desligadas: regra de default não é consultada nem aplicada")
    void ct12DefaultNotInEnumWithRecommendationsOff() {
        RuleConfiguration config = config(false, false, true, false, false);

        ValidationResult result = new OpenApiEvaluator(config)
                .validate(specWithEnumProperty(Collections.singletonList("udp"), "http"));

        assertTrue(warningsWith(result, "not in enum").isEmpty());
        verify(config, never()).isEnableDefaultNotInEnumRecommendation();
    }

    @Test
    @DisplayName("CT-OE-13 default fora do enum em esquema inline do request body gera aviso")
    void ct13DefaultNotInEnumInlineRequestBody() {
        StringSchema property = new StringSchema();
        property.setEnum(Arrays.asList("udp", "tcp"));
        property.setDefault("http");
        Operation post = operation("create")
                .requestBody(new RequestBody().content(jsonContent(new ObjectSchema().addProperty("protocol", property))));

        ValidationResult result = new OpenApiEvaluator(onlyDefaultNotInEnum())
                .validate(specWithPath("/items", new PathItem().post(post)));

        List<Invalid> warnings = warningsWith(result, "not in enum");
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).getMessage().contains("'http'"));
    }

    @Test
    @DisplayName("CT-OE-14 default numérico igual em valor a um item do enum (1.5 e 1.50) não gera aviso")
    void ct14NumericDefaultComparedByValue() {
        NumberSchema property = new NumberSchema();
        property.setEnum(Collections.singletonList(new BigDecimal("1.5")));
        property.setDefault(new BigDecimal("1.50"));
        OpenAPI openAPI = specWithSchema("Rate", new ObjectSchema().addProperty("factor", property));

        ValidationResult result = new OpenApiEvaluator(onlyDefaultNotInEnum()).validate(openAPI);

        assertTrue(warningsWith(result, "not in enum").isEmpty(),
                "1.50 reported as outside enum [1.5]: " + messages(result));
    }

    // ---------------------------------------------------------------- R07 / R08: parameters

    @Test
    @DisplayName("CT-OE-15 header com '_' declarado na operação gera aviso")
    void ct15UnderscoreHeaderOnOperation() {
        Operation get = operation("list").addParametersItem(underscoreHeader());

        ValidationResult result = new OpenApiEvaluator(onlyUnderscore())
                .validate(specWithPath("/items", new PathItem().get(get)));

        List<Invalid> warnings = warningsWith(result, UNDERSCORE_HEADER + " contains an underscore.");
        assertEquals(1, warnings.size());
    }

    @Test
    @DisplayName("CT-OE-16 header com '_' declarado no path item gera aviso")
    void ct16UnderscoreHeaderOnPathItem() {
        PathItem pathItem = new PathItem().get(operation("list")).parameters(Collections.singletonList(underscoreHeader()));

        ValidationResult result = new OpenApiEvaluator(onlyUnderscore()).validate(specWithPath("/items", pathItem));

        assertEquals(1, warningsWith(result, UNDERSCORE_HEADER + " contains an underscore.").size());
    }

    @Test
    @DisplayName("CT-OE-17 header com '_' em components/parameters gera aviso")
    void ct17UnderscoreHeaderInComponents() {
        OpenAPI openAPI = emptySpec();
        openAPI.setComponents(new Components().addParameters("TokenHeader", underscoreHeader()));

        ValidationResult result = new OpenApiEvaluator(onlyUnderscore()).validate(openAPI);

        assertEquals(1, warningsWith(result, UNDERSCORE_HEADER + " contains an underscore.").size());
    }

    @Test
    @DisplayName("CT-OE-18 parâmetro com $ref é resolvido antes da validação")
    void ct18ReferencedParameterIsResolved() {
        Operation get = operation("list").addParametersItem(new Parameter().$ref("#/components/parameters/TokenHeader"));
        OpenAPI openAPI = specWithPath("/items", new PathItem().get(get));
        openAPI.setComponents(new Components().addParameters("TokenHeader", underscoreHeader()));

        ValidationResult result = new OpenApiEvaluator(onlyUnderscore()).validate(openAPI);

        // one occurrence from the resolved $ref on the operation, one from components/parameters
        assertEquals(2, warningsWith(result, UNDERSCORE_HEADER + " contains an underscore.").size());
    }

    @Test
    @DisplayName("CT-OE-19 parâmetro de query com '_' não gera aviso")
    void ct19UnderscoreQueryParameterIgnored() {
        Operation get = operation("list").addParametersItem(new QueryParameter().name("page_size").schema(new StringSchema()));

        ValidationResult result = new OpenApiEvaluator(onlyUnderscore())
                .validate(specWithPath("/items", new PathItem().get(get)));

        assertTrue(result.getWarnings().isEmpty());
    }

    // ---------------------------------------------------------------- R09: operations

    @Test
    @DisplayName("CT-OE-20 GET com request body gera aviso de anti-pattern")
    void ct20GetWithBodyIsReported() {
        Operation get = operation("search").requestBody(new RequestBody().content(jsonContent(new StringSchema())));

        ValidationResult result = new OpenApiEvaluator(onlyGetWithBody())
                .validate(specWithPath("/search", new PathItem().get(get)));

        List<Invalid> warnings = warningsWith(result, "anti-pattern");
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).getDetails().startsWith("GET search"));
    }

    @Test
    @DisplayName("CT-OE-21 POST com request body não gera aviso")
    void ct21PostWithBodyIsAccepted() {
        Operation post = operation("create").requestBody(new RequestBody().content(jsonContent(new StringSchema())));

        ValidationResult result = new OpenApiEvaluator(onlyGetWithBody())
                .validate(specWithPath("/items", new PathItem().post(post)));

        assertTrue(result.getWarnings().isEmpty());
    }

    // ---------------------------------------------------------------- R10: security schemes

    @Test
    @DisplayName("CT-OE-22 apiKey em header com '_' gera aviso")
    void ct22UnderscoreApiKeyHeader() {
        OpenAPI openAPI = emptySpec();
        openAPI.setComponents(new Components().addSecuritySchemes("key",
                new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name("api_key")));

        ValidationResult result = new OpenApiEvaluator(onlyUnderscore()).validate(openAPI);

        assertEquals(1, warningsWith(result, "api_key contains an underscore.").size());
    }

    @Test
    @DisplayName("CT-OE-23 mapa de security schemes vazio não gera aviso")
    void ct23EmptySecuritySchemes() {
        OpenAPI openAPI = emptySpec();
        Map<String, SecurityScheme> empty = new HashMap<>();
        openAPI.setComponents(new Components().securitySchemes(empty));

        ValidationResult result = new OpenApiEvaluator(onlyUnderscore()).validate(openAPI);

        assertTrue(result.getAll().isEmpty());
    }

    // ---------------------------------------------------------------- R11: tags

    @Test
    @DisplayName("CT-OE-24 lista de tags nula ou vazia não gera aviso")
    void ct24NullOrEmptyTags() {
        OpenApiEvaluator evaluator = new OpenApiEvaluator(noRules());

        assertTrue(warningsWith(evaluator.validate(emptySpec()), "Duplicated tag").isEmpty());
        assertTrue(warningsWith(evaluator.validate(specWithTags()), "Duplicated tag").isEmpty());
    }

    @Test
    @DisplayName("CT-OE-25 uma única tag (limite) não gera aviso")
    void ct25SingleTag() {
        ValidationResult result = new OpenApiEvaluator(noRules()).validate(specWithTags("pet"));

        assertTrue(warningsWith(result, "Duplicated tag").isEmpty());
    }

    @Test
    @DisplayName("CT-OE-26 tags distintas não geram aviso")
    void ct26DistinctTags() {
        ValidationResult result = new OpenApiEvaluator(noRules()).validate(specWithTags("pet", "store"));

        assertTrue(warningsWith(result, "Duplicated tag").isEmpty());
    }

    @Test
    @DisplayName("CT-OE-27 tags repetidas geram um único aviso citando cada nome uma vez")
    void ct27DuplicatedTags() {
        ValidationResult result = new OpenApiEvaluator(noRules())
                .validate(specWithTags("pet", "pet", "pet", "store", "store"));

        List<Invalid> warnings = warningsWith(result, "Duplicated tag");
        assertEquals(1, warnings.size());
        String names = warnings.get(0).getMessage().substring("Duplicated tag(s): ".length());
        List<String> listed = Arrays.asList(names.split(","));
        assertEquals(2, listed.size());
        assertTrue(listed.containsAll(Arrays.asList("pet", "store")));
    }

    // ---------------------------------------------------------------- R12: aggregation

    @Test
    @DisplayName("CT-OE-28 resultado agrega avisos de esquema, operação e tags")
    void ct28ResultsAreAggregated() {
        Operation get = operation("search").requestBody(new RequestBody().content(jsonContent(new StringSchema())));
        OpenAPI openAPI = specWithPath("/search", new PathItem().get(get));
        openAPI.setComponents(new Components().addSchemas("Orphan", new ObjectSchema()));
        openAPI.setTags(Arrays.asList(new Tag().name("pet"), new Tag().name("pet")));

        ValidationResult result = new OpenApiEvaluator(config(true, true, false, false, true)).validate(openAPI);

        assertEquals(0, result.getErrors().size());
        assertEquals(3, result.getWarnings().size());
        assertEquals(1, warningsWith(result, "Unused model: Orphan").size());
        assertEquals(1, warningsWith(result, "anti-pattern").size());
        assertEquals(1, warningsWith(result, "Duplicated tag(s): pet").size());
    }
}
