package org.openapitools.codegen;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link OpenAPINormalizer}, exercised through its public API
 * (constructor with {@code (OpenAPI, Map)} rules, {@link OpenAPINormalizer#normalizeSchema},
 * {@link OpenAPINormalizer#getRule} and {@code normalize()} in the same package).
 *
 * <p>Cases CT-NO-01..CT-NO-36 follow the oracle rules R01..R06 of the test plan
 * (spreadsheet {@code entrega1/ludwig/planilha-oraculos-matriz-OpenAPINormalizer.xlsx}).
 * The main oracle is {@code docs/customization.md} (openapi-normalizer section)
 * plus the rule comments in {@code OpenAPINormalizer} itself.
 */
public class OpenAPINormalizerJUnitTest {

    // ---------------------------------------------------------------- helpers

    private static OpenAPI emptyApi() {
        return new OpenAPI().components(new Components().schemas(new LinkedHashMap<>()));
    }

    private static OpenAPINormalizer normalizer(OpenAPI api, Map<String, String> rules) {
        return new OpenAPINormalizer(api, rules);
    }

    private static OpenAPI apiWithSchemas(Map<String, Schema> schemas) {
        return new OpenAPI().components(new Components().schemas(schemas));
    }

    private static Operation opWithTags(String... tags) {
        Operation op = new Operation().operationId("op");
        op.setTags(new ArrayList<>(Arrays.asList(tags)));
        return op;
    }

    private static OpenAPI apiWithOperation(Operation op) {
        OpenAPI api = emptyApi();
        api.paths(new Paths().addPathItem("/t", new PathItem().get(op)));
        return api;
    }

    private static List<String> tagsOf(OpenAPI api) {
        return api.getPaths().get("/t").getGet().getTags();
    }

    // ------------------------------------------------- R00: rule processing

    @Nested
    @DisplayName("R00 - processamento de regras")
    class RuleProcessing {

        @Test
        @DisplayName("CT-NO-01: regras de simplificacao sao true por padrao; as de tag/x-internal sao false")
        void defaults() {
            OpenAPINormalizer n = normalizer(emptyApi(), new HashMap<>());

            assertAll(
                    () -> assertTrue(n.getRule("SIMPLIFY_ONEOF_ANYOF")),
                    () -> assertTrue(n.getRule("SIMPLIFY_BOOLEAN_ENUM")),
                    () -> assertTrue(n.getRule("SIMPLIFY_ONEOF_ANYOF_ENUM")),
                    () -> assertTrue(n.getRule("REFACTOR_ALLOF_WITH_PROPERTIES_ONLY")),
                    () -> assertFalse(n.getRule("KEEP_ONLY_FIRST_TAG_IN_OPERATION")),
                    () -> assertFalse(n.getRule("SET_TAGS_TO_OPERATIONID")),
                    () -> assertFalse(n.getRule("REMOVE_X_INTERNAL")));
        }

        @Test
        @DisplayName("CT-NO-02: valor \"false\" desliga regra ligada por padrao; \"true\" liga regra desligada")
        void explicitValues() {
            OpenAPINormalizer n = normalizer(emptyApi(), Map.of(
                    "SIMPLIFY_BOOLEAN_ENUM", "false",
                    "KEEP_ONLY_FIRST_TAG_IN_OPERATION", "true"));

            assertAll(
                    () -> assertFalse(n.getRule("SIMPLIFY_BOOLEAN_ENUM")),
                    () -> assertTrue(n.getRule("KEEP_ONLY_FIRST_TAG_IN_OPERATION")));
        }

        @Test
        @DisplayName("CT-NO-03: nome de regra invalido nao lanca excecao e nao afeta as demais regras")
        void invalidRuleNameIsIgnored() {
            OpenAPI api = TestUtils.parseSpec("src/test/resources/3_0/simplifyBooleanEnum_test.yaml");
            assertDoesNotThrow(() -> normalizer(api, Map.of("NO_SUCH_RULE", "true")).normalize());

            Schema s = api.getComponents().getSchemas().get("BooleanEnumTest");
            assertNull(((BooleanSchema) s.getProperties().get("boolean_enum")).getEnum(),
                    "default SIMPLIFY_BOOLEAN_ENUM must still apply");
        }

        @Test
        @DisplayName("CT-NO-04: DISABLE_ALL pula a normalizacao mesmo com regras default true")
        void disableAllSkipsNormalization() {
            OpenAPI api = TestUtils.parseSpec("src/test/resources/3_0/simplifyBooleanEnum_test.yaml");

            normalizer(api, Map.of("DISABLE_ALL", "true")).normalize();

            Schema s = api.getComponents().getSchemas().get("BooleanEnumTest");
            assertEquals(2, ((BooleanSchema) s.getProperties().get("boolean_enum")).getEnum().size());
        }

        @Test
        @DisplayName("CT-NO-05: createNormalizer sem NORMALIZER_CLASS devolve OpenAPINormalizer")
        void factoryDefault() {
            assertTrue(OpenAPINormalizer.createNormalizer(emptyApi(), Map.of()) instanceof OpenAPINormalizer);
        }

        @Test
        @DisplayName("CT-NO-33: regra desconhecida em getRule devolve false")
        void unknownRuleIsFalse() {
            assertFalse(normalizer(emptyApi(), new HashMap<>()).getRule("NO_SUCH_RULE"));
        }
    }

    // ------------------------------------------------- R01: SIMPLIFY_BOOLEAN_ENUM

    @Nested
    @DisplayName("R01 - SIMPLIFY_BOOLEAN_ENUM")
    class SimplifyBooleanEnum {

        @Test
        @DisplayName("CT-NO-06: enum [true,false] e apagado com a regra ligada")
        void fullBooleanEnumIsErased() {
            BooleanSchema property = new BooleanSchema();
            property.setEnum(new ArrayList<>(Arrays.asList(true, false)));

            normalizer(emptyApi(), Map.of("SIMPLIFY_BOOLEAN_ENUM", "true"))
                    .normalizeSchema(property, new java.util.HashSet<>());

            assertNull(property.getEnum());
        }

        @Test
        @DisplayName("CT-NO-36: enum de valor unico [true] tambem e apagado")
        void singleValueBooleanEnumIsErased() {
            BooleanSchema property = new BooleanSchema();
            property.setEnum(new ArrayList<>(List.of(true)));

            normalizer(emptyApi(), Map.of("SIMPLIFY_BOOLEAN_ENUM", "true"))
                    .normalizeSchema(property, new java.util.HashSet<>());

            assertNull(property.getEnum());
        }

        @Test
        @DisplayName("CT-NO-07: fixture yaml - BooleanEnumTest perde o enum apos normalize()")
        void yamlFixture() {
            OpenAPI api = TestUtils.parseSpec("src/test/resources/3_0/simplifyBooleanEnum_test.yaml");
            Schema before = api.getComponents().getSchemas().get("BooleanEnumTest");
            assertEquals(2, ((BooleanSchema) before.getProperties().get("boolean_enum")).getEnum().size());

            normalizer(api, Map.of("SIMPLIFY_BOOLEAN_ENUM", "true")).normalize();

            Schema after = api.getComponents().getSchemas().get("BooleanEnumTest");
            assertNull(((BooleanSchema) after.getProperties().get("boolean_enum")).getEnum());
        }

        @Test
        @DisplayName("CT-NO-08: regra desligada mantem o enum")
        void disabledKeepsEnum() {
            OpenAPI api = TestUtils.parseSpec("src/test/resources/3_0/simplifyBooleanEnum_test.yaml");

            normalizer(api, Map.of("SIMPLIFY_BOOLEAN_ENUM", "false")).normalize();

            Schema s = api.getComponents().getSchemas().get("BooleanEnumTest");
            assertEquals(2, ((BooleanSchema) s.getProperties().get("boolean_enum")).getEnum().size());
        }

        @Test
        @DisplayName("CT-NO-09: boolean sem enum nao quebra e continua sem enum")
        void noEnumNoCrash() {
            BooleanSchema property = new BooleanSchema();

            assertDoesNotThrow(() -> normalizer(emptyApi(), Map.of("SIMPLIFY_BOOLEAN_ENUM", "true"))
                    .normalizeSchema(property, new java.util.HashSet<>()));
            assertNull(property.getEnum());
        }

        @Test
        @DisplayName("CT-NO-10: enum de string nao e tocado por esta regra")
        void stringEnumUntouched() {
            StringSchema property = new StringSchema();
            property.setEnum(new ArrayList<>(Arrays.asList("a", "b")));

            normalizer(emptyApi(), Map.of("SIMPLIFY_BOOLEAN_ENUM", "true"))
                    .normalizeSchema(property, new java.util.HashSet<>());

            assertEquals(Arrays.asList("a", "b"), property.getEnum());
        }
    }

    // ------------------------------------------------- R02: SIMPLIFY_ONEOF_ANYOF

    @Nested
    @DisplayName("R02 - SIMPLIFY_ONEOF_ANYOF")
    class SimplifyOneOfAnyOf {

        @Test
        @DisplayName("CT-NO-11: SingleAnyOfTest de 1 sub-schema vira string com o enum")
        void singleAnyOfBecomesString() {
            OpenAPI api = TestUtils.parseSpec("src/test/resources/3_0/simplifyOneOfAnyOf_test.yaml");
            assertEquals(1, api.getComponents().getSchemas().get("SingleAnyOfTest").getAnyOf().size());

            normalizer(api, Map.of("SIMPLIFY_ONEOF_ANYOF", "true")).normalize();

            Schema s = api.getComponents().getSchemas().get("SingleAnyOfTest");
            assertAll(
                    () -> assertNull(s.getAnyOf()),
                    () -> assertEquals("string", s.getType()),
                    () -> assertEquals(2, s.getEnum().size()));
        }

        @Test
        @DisplayName("CT-NO-12: AnyOfTest com null vira string nullable")
        void anyOfWithNullBecomesNullable() {
            OpenAPI api = TestUtils.parseSpec("src/test/resources/3_0/simplifyOneOfAnyOf_test.yaml");

            normalizer(api, Map.of("SIMPLIFY_ONEOF_ANYOF", "true")).normalize();

            Schema s = api.getComponents().getSchemas().get("AnyOfTest");
            assertAll(
                    () -> assertNull(s.getAnyOf()),
                    () -> assertTrue(s instanceof StringSchema),
                    () -> assertTrue(s.getNullable()));
        }

        @Test
        @DisplayName("CT-NO-13: com SIMPLIFY_ONEOF_ANYOF e SIMPLIFY_ONEOF_ANYOF_ENUM off, anyOf unitario e mantido")
        void bothRulesOffKeepsAnyOf() {
            OpenAPI api = TestUtils.parseSpec("src/test/resources/3_0/simplifyOneOfAnyOf_test.yaml");

            normalizer(api, Map.of("SIMPLIFY_ONEOF_ANYOF", "false",
                    "SIMPLIFY_ONEOF_ANYOF_ENUM", "false")).normalize();

            assertEquals(1, api.getComponents().getSchemas().get("SingleAnyOfTest").getAnyOf().size());
        }

        @Test
        @DisplayName("CT-NO-14: DISABLE_ALL mantem o anyOf original")
        void disableAllKeepsAnyOf() {
            OpenAPI api = TestUtils.parseSpec("src/test/resources/3_0/simplifyOneOfAnyOf_test.yaml");

            normalizer(api, Map.of("DISABLE_ALL", "true")).normalize();

            assertEquals(1, api.getComponents().getSchemas().get("SingleAnyOfTest").getAnyOf().size());
        }
    }

    // ------------------------------------------------- R03: SIMPLIFY_ONEOF_ANYOF_ENUM

    @Nested
    @DisplayName("R03 - SIMPLIFY_ONEOF_ANYOF_ENUM")
    class SimplifyOneOfAnyOfEnum {

        @Test
        @DisplayName("CT-NO-15: ColorEnum (oneOf de enums unitarios) vira um unico enum string")
        void colorEnumMerged() {
            OpenAPI api = TestUtils.parseSpec("src/test/resources/3_0/simplifyOneOfWithEnums_test.yaml");

            normalizer(api, Map.of("SIMPLIFY_ONEOF_ANYOF_ENUM", "true")).normalize();

            Schema s = api.getComponents().getSchemas().get("ColorEnum");
            assertAll(
                    () -> assertNull(s.getOneOf()),
                    () -> assertEquals("string", s.getType()),
                    () -> assertEquals(Arrays.asList("red", "green", "blue", "yellow", "purple"), s.getEnum()));
        }

        @Test
        @DisplayName("CT-NO-16: oneOf de $refs para enums (combinedRefsEnum) vira enum [A,B,C,D]")
        void refsToEnumsMerged() {
            OpenAPI api = TestUtils.parseSpec("src/test/resources/3_0/simplifyOneOfWithEnums_test.yaml");

            normalizer(api, Map.of("SIMPLIFY_ONEOF_ANYOF_ENUM", "true")).normalize();

            Schema s = api.getComponents().getSchemas().get("combinedRefsEnum");
            assertAll(
                    () -> assertNull(s.getOneOf()),
                    () -> assertEquals(Arrays.asList("A", "B", "C", "D"), s.getEnum()));
        }

        @Test
        @DisplayName("CT-NO-17: regra desligada mantem o oneOf do parametro")
        void disabledKeepsOneOf() {
            OpenAPI api = TestUtils.parseSpec("src/test/resources/3_0/simplifyOneOfWithEnums_test.yaml");

            normalizer(api, Map.of("SIMPLIFY_ONEOF_ANYOF_ENUM", "false")).normalize();

            Schema param = api.getPaths().get("/test").getGet().getParameters().get(0).getSchema();
            assertAll(
                    () -> assertNotNull(param.getOneOf()),
                    () -> assertNull(param.getEnum()));
        }

        @Test
        @DisplayName("CT-NO-18: oneOf misto (enum + nao-enum) nao e fundido")
        void mixedOneOfKept() {
            ComposedSchema mixed = new ComposedSchema();
            StringSchema en = new StringSchema();
            en.setEnum(new ArrayList<>(List.of("a")));
            mixed.addOneOfItem(en);
            mixed.addOneOfItem(new IntegerSchema());

            Schema out = normalizer(emptyApi(), Map.of("SIMPLIFY_ONEOF_ANYOF_ENUM", "true"))
                    .normalizeSchema(mixed, new java.util.HashSet<>());

            assertNotNull(out.getOneOf(), "mixed oneOf must be kept");
            assertEquals(2, out.getOneOf().size());
        }
    }

    // ------------------------------------------------- R04/R05: tags

    @Nested
    @DisplayName("R04/R05 - regras de tag")
    class Tags {

        @Test
        @DisplayName("CT-NO-19: KEEP_ONLY_FIRST_TAG reduz [person,basic] a [person]")
        void keepFirstTag() {
            OpenAPI api = apiWithOperation(opWithTags("person", "basic"));

            normalizer(api, Map.of("KEEP_ONLY_FIRST_TAG_IN_OPERATION", "true")).normalize();

            assertEquals(List.of("person"), tagsOf(api));
        }

        @ParameterizedTest(name = "CT-NO-31: KEEP_ONLY_FIRST_TAG com {0} tags mantem a primeira")
        @CsvSource({"alpha,beta,gamma,alpha", "a,b,a"})
        void keepFirstTagParametrized(String csv) {
            String[] tags = csv.split(",");
            OpenAPI api = apiWithOperation(opWithTags(tags));

            normalizer(api, Map.of("KEEP_ONLY_FIRST_TAG_IN_OPERATION", "true")).normalize();

            assertEquals(List.of(tags[0]), tagsOf(api));
        }

        @Test
        @DisplayName("CT-NO-20: operacao com 1 tag nao e alterada")
        void singleTagUntouched() {
            OpenAPI api = apiWithOperation(opWithTags("person"));

            normalizer(api, Map.of("KEEP_ONLY_FIRST_TAG_IN_OPERATION", "true")).normalize();

            assertEquals(List.of("person"), tagsOf(api));
        }

        @Test
        @DisplayName("CT-NO-21: operacao sem tags nao quebra e continua sem tags")
        void noTagsNoCrash() {
            OpenAPI api = apiWithOperation(new Operation().operationId("op"));

            assertDoesNotThrow(() -> normalizer(api,
                    Map.of("KEEP_ONLY_FIRST_TAG_IN_OPERATION", "true")).normalize());
            assertNull(tagsOf(api));
        }

        @Test
        @DisplayName("CT-NO-22: regra desligada mantem [person,basic]")
        void disabledKeepsTags() {
            OpenAPI api = apiWithOperation(opWithTags("person", "basic"));

            normalizer(api, new HashMap<>()).normalize();

            assertEquals(Arrays.asList("person", "basic"), tagsOf(api));
        }

        @Test
        @DisplayName("CT-NO-23: SET_TAGS_FOR_ALL_OPERATIONS=core redefine as tags")
        void setTagsForAll() {
            OpenAPI api = apiWithOperation(opWithTags("person", "basic"));

            normalizer(api, Map.of("SET_TAGS_FOR_ALL_OPERATIONS", "core")).normalize();

            assertEquals(List.of("core"), tagsOf(api));
        }

        @Test
        @DisplayName("CT-NO-32: SET_TAGS_FOR_ALL_OPERATIONS vazio nao altera as tags")
        void setTagsForAllEmpty() {
            OpenAPI api = apiWithOperation(opWithTags("person", "basic"));

            normalizer(api, Map.of("SET_TAGS_FOR_ALL_OPERATIONS", "")).normalize();

            assertEquals(Arrays.asList("person", "basic"), tagsOf(api));
        }

        @Test
        @DisplayName("CT-NO-24: SET_TAGS_TO_OPERATIONID usa o operationId; sem ele usa \"default\"")
        void setTagsToOperationId() {
            OpenAPI withId = apiWithOperation(opWithTags("person"));
            normalizer(withId, Map.of("SET_TAGS_TO_OPERATIONID", "true")).normalize();
            assertEquals(List.of("op"), tagsOf(withId));

            OpenAPI noId = apiWithOperation(new Operation());
            normalizer(noId, Map.of("SET_TAGS_TO_OPERATIONID", "true")).normalize();
            assertEquals(List.of("default"), tagsOf(noId));
        }

        @Test
        @DisplayName("CT-NO-25: fixture yaml - GET fica com [person], DELETE inalterado")
        void yamlFixture() {
            OpenAPI api = TestUtils.parseSpec(
                    "src/test/resources/3_0/enableKeepOnlyFirstTagInOperation_test.yaml");

            normalizer(api, Map.of("KEEP_ONLY_FIRST_TAG_IN_OPERATION", "true")).normalize();

            assertAll(
                    () -> assertEquals(List.of("person"),
                            api.getPaths().get("/person/display/{personId}").getGet().getTags()),
                    () -> assertEquals(1, api.getPaths().get("/person/display/{personId}")
                            .getDelete().getTags().size()));
        }
    }

    // ------------------------------------------------- R06: REMOVE_X_INTERNAL

    @Nested
    @DisplayName("R06 - REMOVE_X_INTERNAL")
    class RemoveXInternal {

        @Test
        @DisplayName("CT-NO-26: x-internal true e removido da operacao")
        void removedFromOperation() {
            Operation op = opWithTags("person");
            op.addExtension("x-internal", true);
            OpenAPI api = apiWithOperation(op);

            normalizer(api, Map.of("REMOVE_X_INTERNAL", "true")).normalize();

            assertNull(api.getPaths().get("/t").getGet().getExtensions().get("x-internal"));
        }

        @Test
        @DisplayName("CT-NO-27: operacao sem extensions nao quebra")
        void noExtensionsNoCrash() {
            OpenAPI api = apiWithOperation(opWithTags("person"));

            assertDoesNotThrow(() -> normalizer(api, Map.of("REMOVE_X_INTERNAL", "true")).normalize());
            assertNull(api.getPaths().get("/t").getGet().getExtensions());
        }

        @Test
        @DisplayName("CT-NO-28: regra desligada mantem x-internal")
        void disabledKeepsXInternal() {
            Operation op = opWithTags("person");
            op.addExtension("x-internal", true);
            OpenAPI api = apiWithOperation(op);

            normalizer(api, new HashMap<>()).normalize();

            assertEquals(true, api.getPaths().get("/t").getGet().getExtensions().get("x-internal"));
        }

        @Test
        @DisplayName("CT-NO-29: x-internal true e removido do schema em components")
        void removedFromSchema() {
            ObjectSchema dummy = new ObjectSchema();
            dummy.addExtension("x-internal", true);
            Map<String, Schema> schemas = new LinkedHashMap<>();
            schemas.put("Dummy", dummy);

            normalizer(apiWithSchemas(schemas), Map.of("REMOVE_X_INTERNAL", "true")).normalize();

            assertNull(schemas.get("Dummy").getExtensions().get("x-internal"));
        }

        @Test
        @DisplayName("CT-NO-30: fixture yaml - Dummy e DELETE perdem x-internal; GET sem extensions intacto")
        void yamlFixture() {
            OpenAPI api = TestUtils.parseSpec(
                    "src/test/resources/3_0/enableKeepOnlyFirstTagInOperation_test.yaml");

            normalizer(api, Map.of("REMOVE_X_INTERNAL", "true")).normalize();

            assertAll(
                    () -> assertNull(api.getComponents().getSchemas().get("Dummy")
                            .getExtensions().get("x-internal")),
                    () -> assertNull(api.getPaths().get("/person/display/{personId}")
                            .getDelete().getExtensions().get("x-internal")),
                    () -> assertNull(api.getPaths().get("/person/display/{personId}")
                            .getGet().getExtensions()));
        }
    }

    // ------------------------------------------------- R00 extra: normalize() base

    @Nested
    @DisplayName("R00 extra - comportamento base de normalize()")
    class NormalizeBase {

        @Test
        @DisplayName("CT-NO-34: normalize() preenche info ausente")
        void fillsMissingInfo() {
            OpenAPI api = emptyApi();

            normalizer(api, new HashMap<>()).normalize();

            assertAll(
                    () -> assertNotNull(api.getInfo()),
                    () -> assertEquals("OpenAPI", api.getInfo().getTitle()),
                    () -> assertEquals("0.0.1", api.getInfo().getVersion()));
        }

        @Test
        @DisplayName("CT-NO-35: normalize() sem paths nao quebra")
        void noPathsNoCrash() {
            assertDoesNotThrow(() -> normalizer(emptyApi(), new HashMap<>()).normalize());
        }

        @Test
        @DisplayName("CT-NO-45: normalize() cria components/schemas quando ausentes")
        void createsMissingComponents() {
            OpenAPI api = new OpenAPI();

            normalizer(api, new HashMap<>()).normalize();

            assertAll(
                    () -> assertNotNull(api.getComponents()),
                    () -> assertNotNull(api.getComponents().getSchemas()));
        }
    }

    // ------------------------------------------------- Rodada 2: ramos estruturais (R00-R06)

    @Nested
    @DisplayName("Rodada 2 - dispatch estrutural de normalizeSchema e interacoes")
    class StructuralDispatch {

        @Test
        @DisplayName("CT-NO-37: ArraySchema tem os items normalizados (boolean-enum some nos items)")
        void arrayItemsNormalized() {
            BooleanSchema items = new BooleanSchema();
            items.setEnum(new ArrayList<>(Arrays.asList(true, false)));
            ArraySchema property = new ArraySchema().items(items);

            Schema out = normalizer(emptyApi(), Map.of("SIMPLIFY_BOOLEAN_ENUM", "true"))
                    .normalizeSchema(property, new java.util.HashSet<>());

            assertAll(
                    () -> assertTrue(out instanceof ArraySchema),
                    () -> assertNull(((BooleanSchema) ((ArraySchema) out).getItems()).getEnum()));
        }

        @Test
        @DisplayName("CT-NO-38: MapSchema (additionalProperties) passa ileso sem NORMALIZE_31SPEC")
        void mapSchemaIntact() {
            MapSchema property = new MapSchema();
            property.setAdditionalProperties(new StringSchema());

            Schema out = normalizer(emptyApi(), new HashMap<>())
                    .normalizeSchema(property, new java.util.HashSet<>());

            assertAll(
                    () -> assertTrue(out instanceof MapSchema),
                    () -> assertEquals("string",
                            ((Schema) out.getAdditionalProperties()).getType()));
        }

        @Test
        @DisplayName("CT-NO-39: $ref puro sem siblings nao e normalizado (skip)")
        void plainRefSkipped() {
            Schema<?> ref = new Schema<>().$ref("#/components/schemas/Pet");

            Schema out = normalizer(emptyApi(), new HashMap<>())
                    .normalizeSchema(ref, new java.util.HashSet<>());

            assertEquals("#/components/schemas/Pet", out.get$ref());
        }

        @Test
        @DisplayName("CT-NO-40: schema nulo devolve nulo sem excecao")
        void nullSchemaNoCrash() {
            assertNull(normalizer(emptyApi(), new HashMap<>())
                    .normalizeSchema(null, new java.util.HashSet<>()));
        }

        @Test
        @DisplayName("CT-NO-41: oneOf com os 6 tipos vira any-type (sem tipo, sem oneOf)")
        void sixTypeOneOfBecomesAnyType() {
            ComposedSchema any = new ComposedSchema();
            any.addOneOfItem(new StringSchema());
            any.addOneOfItem(new IntegerSchema());
            any.addOneOfItem(new NumberSchema());
            any.addOneOfItem(new BooleanSchema());
            any.addOneOfItem(new ArraySchema().items(new StringSchema()));
            any.addOneOfItem(new ObjectSchema());

            Schema out = normalizer(emptyApi(), Map.of("SIMPLIFY_ONEOF_ANYOF", "true"))
                    .normalizeSchema(any, new java.util.HashSet<>());

            assertAll(
                    () -> assertNull(out.getOneOf()),
                    () -> assertNull(out.getType()));
        }

        @Test
        @DisplayName("CT-NO-42: x-internal em string ('true') tambem e removido da propriedade")
        void stringXInternalRemoved() {
            ObjectSchema holder = new ObjectSchema();
            StringSchema prop = new StringSchema();
            prop.addExtension("x-internal", "true");
            holder.addProperty("nick", prop);
            Map<String, Schema> schemas = new LinkedHashMap<>();
            schemas.put("Holder", holder);

            normalizer(apiWithSchemas(schemas), Map.of("REMOVE_X_INTERNAL", "true")).normalize();

            assertNull(((Schema) schemas.get("Holder").getProperties().get("nick"))
                    .getExtensions().get("x-internal"));
        }

        @Test
        @DisplayName("CT-NO-43: $ref com sibling (description) vira allOf e perde o $ref")
        void refWithSiblingBecomesAllOf() {
            Schema<?> ref = new Schema<>().$ref("#/components/schemas/Pet");
            ref.setDescription("um pet");

            Schema out = normalizer(emptyApi(), new HashMap<>())
                    .normalizeSchema(ref, new java.util.HashSet<>());

            assertAll(
                    () -> assertNull(out.get$ref()),
                    () -> assertNotNull(out.getAllOf()),
                    () -> assertEquals(1, out.getAllOf().size()));
        }

        @Test
        @DisplayName("CT-NO-44: getRule reflete regra nao-booleana informada (SET_TAGS_FOR_ALL_OPERATIONS)")
        void nonBooleanRuleFlag() {
            assertTrue(normalizer(emptyApi(), Map.of("SET_TAGS_FOR_ALL_OPERATIONS", "core"))
                    .getRule("SET_TAGS_FOR_ALL_OPERATIONS"));
        }

        @Test
        @DisplayName("CT-NO-46: KEEP_ONLY_FIRST_TAG + SET_TAGS_FOR_ALL_OPERATIONS -> vale o valor fixo")
        void setAllWinsOverKeepFirst() {
            OpenAPI api = apiWithOperation(opWithTags("person", "basic"));

            normalizer(api, Map.of("KEEP_ONLY_FIRST_TAG_IN_OPERATION", "true",
                    "SET_TAGS_FOR_ALL_OPERATIONS", "core")).normalize();

            assertEquals(List.of("core"), tagsOf(api));
        }
    }
}
