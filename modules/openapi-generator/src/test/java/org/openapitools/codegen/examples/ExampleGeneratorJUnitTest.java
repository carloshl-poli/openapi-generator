package org.openapitools.codegen.examples;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BinarySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.UUIDSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ExampleGenerator}, exercised through its public API
 * ({@code generate(Map, List, Schema)}, {@code generate(Map, List, String)} and
 * {@code generateFromResponseSchema(String, Schema, Set)}).
 * <p>
 * Cases CT-EG-01..CT-EG-48 follow the oracle rules R01..R12 of the test plan
 * (spreadsheet {@code entrega1/hygor/planilha-oraculos-matriz-ExampleGenerator.xlsx}).
 * The main oracle for generated values is the OpenAPI 3.0.3 Schema Object definition
 * of {@code example} ("an example of an instance for this schema"): a generated example
 * must be a valid instance of the schema it was generated from.
 */
public class ExampleGeneratorJUnitTest {

    private static final String JSON = "application/json";
    private static final String XML = "application/xml";

    // ---------------------------------------------------------------- helpers

    /** Spec whose components/schemas are both the generator's "examples" map and the $ref target. */
    private static OpenAPI spec(Map<String, Schema> schemas) {
        return new OpenAPI().components(new Components().schemas(schemas));
    }

    private static ExampleGenerator generator(Map<String, Schema> schemas) {
        return new ExampleGenerator(schemas, spec(schemas));
    }

    private static ExampleGenerator emptyGenerator() {
        return generator(new LinkedHashMap<>());
    }

    @SafeVarargs
    private static Map<String, Schema> schemas(Map.Entry<String, Schema>... entries) {
        Map<String, Schema> map = new LinkedHashMap<>();
        for (Map.Entry<String, Schema> e : entries) {
            map.put(e.getKey(), e.getValue());
        }
        return map;
    }

    private static Schema<?> ref(String name) {
        return new Schema<>().$ref("#/components/schemas/" + name);
    }

    private static JsonNode parse(String json) {
        try {
            return Json.mapper().readTree(json);
        } catch (Exception e) {
            throw new AssertionError("generated example is not valid JSON: " + json, e);
        }
    }

    /** JSON example generated for a standalone (property) schema. */
    private static JsonNode jsonExample(Schema<?> property) {
        List<Map<String, String>> out = emptyGenerator().generate(null, List.of(JSON), property);
        assertEquals(1, out.size(), "one JSON example expected: " + out);
        return parse(out.get(0).get("example"));
    }

    /** JSON example generated for a named model, using the given components. */
    private static JsonNode modelExample(Map<String, Schema> schemas, String model) {
        List<Map<String, String>> out = generator(schemas).generate(null, List.of(JSON), model);
        assertEquals(1, out.size(), "one JSON example expected: " + out);
        return parse(out.get(0).get("example"));
    }

    /** Model with a single property, so that the string fallback (property name) can be observed. */
    private static JsonNode propertyInModel(String propertyName, Schema<?> property) {
        Schema<?> model = new ObjectSchema().addProperty(propertyName, property);
        return modelExample(schemas(Map.entry("Holder", model)), "Holder").get(propertyName);
    }

    // ------------------------------------------------- R01, R02, R05: output structure and media types

    @Nested
    @DisplayName("R01/R02/R05 - estrutura da saída e tipos de mídia")
    class OutputStructure {

        @Test
        @DisplayName("CT-EG-01: exemplos explícitos têm precedência e são devolvidos um por tipo de mídia")
        void explicitExamplesWin() {
            Map<String, Object> explicit = new LinkedHashMap<>();
            explicit.put(JSON, Map.of("id", 1));
            explicit.put(XML, "<id>1</id>");

            List<Map<String, String>> out = emptyGenerator().generate(explicit, List.of(JSON), new IntegerSchema());

            assertEquals(2, out.size());
            assertAll(
                    () -> assertEquals(JSON, out.get(0).get("contentType")),
                    () -> assertEquals(1, parse(out.get(0).get("example")).get("id").asInt()),
                    () -> assertEquals(XML, out.get(1).get("contentType")),
                    () -> assertEquals("\"<id>1</id>\"", out.get(1).get("example")),
                    () -> assertFalse(out.get(0).containsKey("generatedContentType"),
                            "explicit examples are not generated"));
        }

        @Test
        @DisplayName("CT-EG-02: lista de tipos de mídia nula assume application/json")
        void nullMediaTypesDefaultToJson() {
            List<Map<String, String>> out = emptyGenerator().generate(null, null, new BooleanSchema());

            assertEquals(1, out.size());
            assertAll(
                    () -> assertEquals(JSON, out.get(0).get("contentType")),
                    () -> assertEquals(JSON, out.get(0).get("generatedContentType")),
                    () -> assertEquals("true", out.get(0).get("example")));
        }

        @ParameterizedTest(name = "CT-EG-03: \"{0}\" é tratado como JSON")
        @CsvSource({"application/json", "application/json; charset=utf-8", "*/*"})
        void jsonCompatibleMediaTypes(String mediaType) {
            List<Map<String, String>> out = emptyGenerator().generate(null, List.of(mediaType), new BooleanSchema());

            assertEquals(1, out.size());
            assertAll(
                    () -> assertEquals(mediaType, out.get(0).get("contentType")),
                    () -> assertEquals(JSON, out.get(0).get("generatedContentType")));
        }

        @Test
        @DisplayName("CT-EG-04: modelo com application/xml gera exemplo XML")
        void modelAsXml() {
            Map<String, Schema> components = schemas(Map.entry("Pet",
                    new ObjectSchema().addProperty("name", new StringSchema().example("Rex"))));

            List<Map<String, String>> out = generator(components).generate(null, List.of(XML), "Pet");

            assertEquals(1, out.size());
            assertAll(
                    () -> assertEquals(XML, out.get(0).get("generatedContentType")),
                    () -> assertTrue(out.get(0).get("example").contains("<name>Rex</name>"), out.get(0).get("example")));
        }

        @Test
        @DisplayName("CT-EG-05: modelo com tipo de mídia não suportado devolve mensagem explicativa")
        void modelWithCustomMediaType() {
            Map<String, Schema> components = schemas(Map.entry("Pet", new ObjectSchema().addProperty("id", new IntegerSchema())));

            List<Map<String, String>> out = generator(components).generate(null, List.of("text/plain"), "Pet");

            assertEquals(1, out.size());
            assertEquals("Custom MIME type example not yet supported: text/plain", out.get(0).get("example"));
        }

        @Test
        @DisplayName("CT-EG-06: nenhum exemplo produzível resulta em {output: none}")
        void nothingProducible() {
            List<Map<String, String>> out = emptyGenerator().generate(null, List.of("text/plain"), new StringSchema());

            assertEquals(List.of(Map.of("output", "none")), out);
        }

        @Test
        @DisplayName("CT-EG-07: modelo inexistente em JSON resulta em {output: none}")
        void unknownModel() {
            List<Map<String, String>> out = emptyGenerator().generate(null, List.of(JSON), "DoesNotExist");

            assertEquals(List.of(Map.of("output", "none")), out);
        }

        @Test
        @DisplayName("CT-EG-41: propriedade com application/xml gera exemplo XML")
        void propertyAsXml() {
            List<Map<String, String>> out = emptyGenerator().generate(null, List.of(XML), new StringSchema().example("abc"));

            assertEquals(1, out.size());
            assertAll(
                    () -> assertEquals(XML, out.get(0).get("contentType")),
                    () -> assertEquals(XML, out.get(0).get("generatedContentType")),
                    () -> assertTrue(out.get(0).get("example").contains("abc"), out.get(0).get("example")));
        }

        @Test
        @DisplayName("CT-EG-42: exemplos explícitos também prevalecem na sobrecarga por nome de modelo")
        void explicitExamplesWinForModel() {
            Map<String, Schema> components = schemas(Map.entry("Pet", new ObjectSchema().addProperty("id", new IntegerSchema())));

            List<Map<String, String>> out = generator(components)
                    .generate(Map.of(JSON, Map.of("id", 99)), List.of(JSON), "Pet");

            assertEquals(1, out.size());
            assertEquals(99, parse(out.get(0).get("example")).get("id").asInt());
        }

        @Test
        @DisplayName("CT-EG-43: modelo com lista de tipos de mídia nula assume application/json")
        void modelWithNullMediaTypes() {
            Map<String, Schema> components = schemas(Map.entry("Pet", new ObjectSchema().addProperty("id", new IntegerSchema().example(5))));

            List<Map<String, String>> out = generator(components).generate(null, null, "Pet");

            assertEquals(1, out.size());
            assertEquals(JSON, out.get(0).get("contentType"));
            assertEquals(5, parse(out.get(0).get("example")).get("id").asInt());
        }
    }

    // ------------------------------------------------- R03: generateFromResponseSchema

    @Nested
    @DisplayName("R03 - generateFromResponseSchema")
    class FromResponseSchema {

        @Test
        @DisplayName("CT-EG-08: statusCode é copiado para cada exemplo, um por tipo de mídia produzido")
        void statusCodeOnEveryExample() {
            Map<String, Schema> components = schemas(Map.entry("Pet", new ObjectSchema().addProperty("id", new IntegerSchema().example(7))));
            LinkedHashSet<String> produces = new LinkedHashSet<>(List.of(JSON, XML));

            List<Map<String, String>> out = generator(components).generateFromResponseSchema("201", ref("Pet"), produces);

            assertEquals(2, out.size());
            out.forEach(example -> assertEquals("201", example.get("statusCode")));
            assertEquals(7, parse(out.get(0).get("example")).get("id").asInt());
        }

        @Test
        @DisplayName("CT-EG-09: schema sem example, sem $ref e que não é array devolve null")
        void noExampleNoRefNoArray() {
            assertNull(emptyGenerator().generateFromResponseSchema("200", new StringSchema(), produces(JSON)));
        }

        @Test
        @DisplayName("CT-EG-10: example primitivo do schema de resposta é serializado como JSON")
        void primitiveExample() {
            List<Map<String, String>> out = emptyGenerator()
                    .generateFromResponseSchema("200", new StringSchema().example("pong"), produces(JSON));

            assertEquals(1, out.size());
            assertEquals("\"pong\"", out.get(0).get("example"));
            assertEquals("200", out.get(0).get("statusCode"));
        }

        @Test
        @DisplayName("CT-EG-11: array de primitivos na resposta gera um array JSON")
        void arrayResponse() {
            ArraySchema array = new ArraySchema().items(new BooleanSchema());

            List<Map<String, String>> out = emptyGenerator().generateFromResponseSchema("200", array, produces(JSON));

            JsonNode example = parse(out.get(0).get("example"));
            assertTrue(example.isArray());
            assertTrue(example.get(0).asBoolean());
        }

        @Test
        @DisplayName("CT-EG-44: example primitivo pedido só em XML resulta em {output: none}")
        void primitiveExampleAsXmlOnly() {
            List<Map<String, String>> out = emptyGenerator()
                    .generateFromResponseSchema("200", new StringSchema().example("pong"), produces(XML));

            assertEquals(1, out.size());
            assertEquals("none", out.get(0).get("output"));
        }

        @Test
        @DisplayName("CT-EG-45: schema inline de objeto com example objeto mantém o tipo de mídia e o example declarado")
        void inlineObjectExample() {
            Schema<Object> response = new ObjectSchema()
                    .addProperty("id", new IntegerSchema())
                    .addProperty("name", new StringSchema());
            response.setExample(Map.of("id", 1, "name", "Rex"));

            List<Map<String, String>> out = emptyGenerator().generateFromResponseSchema("200", response, produces(JSON));

            assertEquals(1, out.size(), "one example per produced media type: " + out);
            assertEquals(JSON, out.get(0).get("contentType"), "contentType must be a produced media type: " + out);
            JsonNode example = parse(out.get(0).get("example"));
            assertEquals(1, example.get("id").asInt());
            assertEquals("Rex", example.get("name").asText());
        }

        private LinkedHashSet<String> produces(String... mediaTypes) {
            return new LinkedHashSet<>(List.of(mediaTypes));
        }
    }

    // ------------------------------------------------- R04, R08: value precedence

    @Nested
    @DisplayName("R04/R08 - precedência de valores")
    class ValuePrecedence {

        @Test
        @DisplayName("CT-EG-12: example declarado prevalece sobre as restrições do schema")
        void declaredExampleWins() {
            Schema<?> property = new IntegerSchema().minimum(BigDecimal.valueOf(100)).example(42);

            assertEquals(42, jsonExample(property).asInt());
        }

        @Test
        @DisplayName("CT-EG-13: example do tipo Date é formatado como yyyy-MM-dd em UTC")
        void dateExampleFormattedInUtc() {
            // 01:30 UTC on 2024-03-05 is still 2024-03-04 in Brazil (UTC-3): the result must not depend on the JVM time zone
            Date date = Date.from(Instant.parse("2024-03-05T01:30:00Z"));
            Schema<?> property = new DateSchema();
            property.setExample(date);

            assertEquals("2024-03-05", jsonExample(property).asText());
        }

        @Test
        @DisplayName("CT-EG-14: em string, default prevalece sobre enum")
        void defaultBeatsEnum() {
            StringSchema property = new StringSchema();
            property.setEnum(List.of("a", "b"));
            property.setDefault("b");

            assertEquals("b", jsonExample(property).asText());
        }

        @Test
        @DisplayName("CT-EG-15: em string sem default, usa o primeiro valor do enum")
        void firstEnumValue() {
            StringSchema property = new StringSchema();
            property.setEnum(List.of("available", "sold"));

            assertEquals("available", jsonExample(property).asText());
        }

        @Test
        @DisplayName("CT-EG-16: string sem default, enum ou formato usa o nome da propriedade")
        void propertyNameFallback() {
            assertEquals("nickname", propertyInModel("nickname", new StringSchema()).asText());
        }

        @ParameterizedTest(name = "CT-EG-17: boolean com default={0} gera {1}")
        @CsvSource(value = {"false, false", "true, true", "null, true"}, nullValues = "null")
        void booleanDefault(Boolean defaultValue, boolean expected) {
            BooleanSchema property = new BooleanSchema();
            property.setDefault(defaultValue);

            assertEquals(expected, jsonExample(property).asBoolean());
        }
    }

    // ------------------------------------------------- R11: formats and numeric types

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS) // allows non-static @MethodSource factories (Java 11)
    @DisplayName("R11 - formatos e tipos")
    class Formats {

        Stream<Arguments> fixedFormats() {
            return Stream.of(
                    Arguments.of("date", new DateSchema(), "2000-01-23"),
                    Arguments.of("date-time", new DateTimeSchema(), "2000-01-23T04:56:07.000+00:00"),
                    Arguments.of("uuid", new UUIDSchema(), "046b6c7f-0b8a-43b9-b35d-6489e6daee91"),
                    Arguments.of("uri", new StringSchema().format("uri"), "https://openapi-generator.tech"),
                    Arguments.of("url", new StringSchema().format("url"), "http://example.com/aeiou"),
                    Arguments.of("binary", new BinarySchema(), ""));
        }

        @ParameterizedTest(name = "CT-EG-18: formato {0} gera valor fixo")
        @MethodSource("fixedFormats")
        void fixedFormatValues(String format, Schema<?> property, String expected) {
            assertEquals(expected, jsonExample(property).asText());
        }

        Stream<Arguments> numericTypes() {
            return Stream.of(
                    Arguments.of("integer/int32", new IntegerSchema(), true),
                    Arguments.of("integer/int64", new IntegerSchema().format("int64"), true),
                    Arguments.of("number/float", new NumberSchema().format("float"), false),
                    Arguments.of("number/double", new NumberSchema().format("double"), false),
                    Arguments.of("number", new NumberSchema(), false));
        }

        @ParameterizedTest(name = "CT-EG-19: {0} gera número (inteiro esperado: {2})")
        @MethodSource("numericTypes")
        void numericTypeIsPreserved(String type, Schema<?> property, boolean integral) {
            JsonNode value = jsonExample(property);

            assertTrue(value.isNumber(), type + " -> " + value);
            assertEquals(integral, value.isIntegralNumber(), type + " -> " + value);
        }
    }

    // ------------------------------------------------- R07: numeric constraints

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("R07 - restrições numéricas (minimum/maximum/exclusive*)")
    class NumericConstraints {

        @Test
        @DisplayName("CT-EG-20: integer com minimum e maximum fica no intervalo")
        void integerWithinRange() {
            int value = jsonExample(new IntegerSchema().minimum(BigDecimal.valueOf(10)).maximum(BigDecimal.valueOf(20))).asInt();

            assertTrue(value >= 10 && value <= 20, "value " + value);
        }

        @Test
        @DisplayName("CT-EG-21: integer só com minimum respeita o limite inferior")
        void integerMinimumOnly() {
            int value = jsonExample(new IntegerSchema().minimum(BigDecimal.valueOf(100))).asInt();

            assertTrue(value >= 100, "value " + value);
        }

        @Test
        @DisplayName("CT-EG-22: integer só com maximum positivo respeita o limite superior")
        void integerPositiveMaximumOnly() {
            int value = jsonExample(new IntegerSchema().maximum(BigDecimal.valueOf(50))).asInt();

            assertTrue(value <= 50, "value " + value);
        }

        @Test
        @DisplayName("CT-EG-23: minimum = maximum gera exatamente esse valor")
        void degenerateRange() {
            assertEquals(7, jsonExample(new IntegerSchema().minimum(BigDecimal.valueOf(7)).maximum(BigDecimal.valueOf(7))).asInt());
        }

        Stream<Arguments> negativeMaximumOnly() {
            return Stream.of(
                    Arguments.of("integer", (Supplier<Schema<?>>) () -> new IntegerSchema().maximum(BigDecimal.valueOf(-5))),
                    Arguments.of("number", (Supplier<Schema<?>>) () -> new NumberSchema().maximum(BigDecimal.valueOf(-5))));
        }

        @ParameterizedTest(name = "CT-EG-24: {0} só com maximum negativo (-5) respeita o limite superior")
        @MethodSource("negativeMaximumOnly")
        void negativeMaximumOnly(String type, Supplier<Schema<?>> property) {
            double value = jsonExample(property.get()).asDouble();

            assertTrue(value <= -5, type + " example " + value + " violates maximum -5");
        }

        @Test
        @DisplayName("CT-EG-25: integer com minimum 0 exclusivo gera valor > 0")
        void integerExclusiveMinimum() {
            IntegerSchema property = new IntegerSchema();
            property.setMinimum(BigDecimal.ZERO);
            property.setExclusiveMinimum(true);

            int value = jsonExample(property).asInt();

            assertTrue(value > 0, "example " + value + " violates exclusiveMinimum 0");
        }

        @Test
        @DisplayName("CT-EG-26: number com maximum 1 exclusivo gera valor < 1")
        void numberExclusiveMaximum() {
            NumberSchema property = new NumberSchema();
            property.setMinimum(BigDecimal.ZERO);
            property.setMaximum(BigDecimal.ONE);
            property.setExclusiveMaximum(true);

            double value = jsonExample(property).asDouble();

            assertTrue(value >= 0 && value < 1, "value " + value);
        }
    }

    // ------------------------------------------------- R09: arrays

    @Nested
    @DisplayName("R09 - arrays (maxItems/minItems)")
    class ArrayExamples {

        @ParameterizedTest(name = "CT-EG-27: maxItems={0} gera array com {1} itens")
        @CsvSource(value = {"null, 2", "0, 0", "1, 1", "3, 3", "5, 5", "6, 5", "100, 5"}, nullValues = "null")
        void lengthFollowsMaxItemsWithCapOfFive(Integer maxItems, int expectedLength) {
            ArraySchema property = new ArraySchema().items(new BooleanSchema());
            property.setMaxItems(maxItems);

            assertEquals(expectedLength, jsonExample(property).size());
        }

        @Test
        @DisplayName("CT-EG-28: itens do array seguem o schema de items")
        void itemsFollowItemSchema() {
            JsonNode value = jsonExample(new ArraySchema().items(new UUIDSchema()));

            value.forEach(item -> assertEquals("046b6c7f-0b8a-43b9-b35d-6489e6daee91", item.asText()));
        }

        @Test
        @DisplayName("CT-EG-29: minItems=3 sem maxItems gera ao menos 3 itens")
        void minItemsIsRespected() {
            ArraySchema property = new ArraySchema().items(new BooleanSchema());
            property.setMinItems(3);

            int size = jsonExample(property).size();

            assertTrue(size >= 3, "array with " + size + " items violates minItems 3");
        }
    }

    // ------------------------------------------------- R10: maps

    @Nested
    @DisplayName("R10 - mapas (additionalProperties)")
    class Maps {

        @Test
        @DisplayName("CT-EG-30: mapa gera objeto com a chave \"key\" e valor do tipo de additionalProperties")
        void mapUsesKeyPlaceholder() {
            MapSchema property = new MapSchema();
            property.setAdditionalProperties(new DateSchema());

            JsonNode value = jsonExample(property);

            assertEquals(1, value.size());
            assertEquals("2000-01-23", value.get("key").asText());
        }
    }

    // ------------------------------------------------- R12, R13: models, references and composition

    @Nested
    @DisplayName("R12/R13 - modelos, referências e composição")
    class Models {

        @Test
        @DisplayName("CT-EG-31: propriedades do modelo aparecem na ordem de declaração")
        void propertyOrder() {
            Schema<?> pet = new ObjectSchema()
                    .addProperty("id", new IntegerSchema().example(1))
                    .addProperty("name", new StringSchema())
                    .addProperty("born", new DateSchema());

            JsonNode value = modelExample(schemas(Map.entry("Pet", pet)), "Pet");

            List<String> names = new ArrayList<>();
            value.fieldNames().forEachRemaining(names::add);
            assertEquals(List.of("id", "name", "born"), names);
            assertEquals("name", value.get("name").asText());
        }

        @Test
        @DisplayName("CT-EG-32: propriedade $ref é resolvida para o exemplo do modelo referenciado")
        void nestedReference() {
            Map<String, Schema> components = schemas(
                    Map.entry("Category", new ObjectSchema().addProperty("label", new StringSchema().example("dogs"))),
                    Map.entry("Pet", new ObjectSchema().addProperty("category", ref("Category"))));

            assertEquals("dogs", modelExample(components, "Pet").at("/category/label").asText());
        }

        @Test
        @DisplayName("CT-EG-33: $ref inexistente não interrompe a geração do modelo")
        void danglingReference() {
            Map<String, Schema> components = schemas(Map.entry("Pet", new ObjectSchema()
                    .addProperty("owner", ref("Missing"))
                    .addProperty("id", new IntegerSchema().example(3))));

            JsonNode value = modelExample(components, "Pet");

            assertTrue(value.has("owner"));
            assertEquals(3, value.get("id").asInt());
        }

        @Test
        @DisplayName("CT-EG-34: autorreferência (Node.next -> Node) termina")
        void selfReferenceTerminates() {
            Map<String, Schema> components = schemas(Map.entry("Node", new ObjectSchema()
                    .addProperty("value", new IntegerSchema().example(1))
                    .addProperty("next", ref("Node"))));

            JsonNode value = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> modelExample(components, "Node"));

            assertEquals(1, value.get("value").asInt());
            // the recursion is cut with null, and Json.pretty omits null fields
            assertFalse(value.hasNonNull("next"), "recursion must be cut: " + value);
        }

        @Test
        @DisplayName("CT-EG-35: ciclo indireto (A -> B -> A) termina")
        void indirectCycleTerminates() {
            Map<String, Schema> components = schemas(
                    Map.entry("A", new ObjectSchema().addProperty("b", ref("B"))),
                    Map.entry("B", new ObjectSchema().addProperty("a", ref("A"))));

            JsonNode value = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> modelExample(components, "A"));

            assertTrue(value.at("/b").isObject(), value.toString());
        }

        @Test
        @DisplayName("CT-EG-36: allOf combina as propriedades do $ref e do schema inline")
        void allOfMergesProperties() {
            ComposedSchema dog = new ComposedSchema();
            dog.addAllOfItem(ref("Animal"));
            dog.addAllOfItem(new ObjectSchema().addProperty("barks", new BooleanSchema()));
            Map<String, Schema> components = schemas(
                    Map.entry("Animal", new ObjectSchema().addProperty("name", new StringSchema().example("Rex"))),
                    Map.entry("Dog", dog));

            JsonNode value = modelExample(components, "Dog");

            assertEquals("Rex", value.get("name").asText());
            assertTrue(value.get("barks").asBoolean());
        }

        @Test
        @DisplayName("CT-EG-37: oneOf usa a primeira alternativa com $ref resolvível")
        void oneOfUsesFirstResolvableAlternative() {
            ComposedSchema pet = new ComposedSchema();
            pet.addOneOfItem(ref("Missing"));
            pet.addOneOfItem(ref("Cat"));
            pet.addOneOfItem(ref("Dog"));
            Map<String, Schema> components = schemas(
                    Map.entry("Cat", new ObjectSchema().addProperty("kind", new StringSchema().example("cat"))),
                    Map.entry("Dog", new ObjectSchema().addProperty("kind", new StringSchema().example("dog"))),
                    Map.entry("Pet", pet));

            assertEquals("cat", modelExample(components, "Pet").get("kind").asText());
        }

        @Test
        @DisplayName("CT-EG-38: modelo enum de nível superior usa o primeiro valor")
        void topLevelEnumModel() {
            StringSchema status = new StringSchema();
            status.setEnum(List.of("placed", "approved"));

            assertEquals("placed", modelExample(schemas(Map.entry("Status", status)), "Status").asText());
        }

        @Test
        @DisplayName("CT-EG-46: allOf com referência circular (A allOf B, B allOf A) termina")
        void allOfCycleTerminates() {
            ComposedSchema a = new ComposedSchema();
            a.addAllOfItem(ref("B"));
            a.addAllOfItem(new ObjectSchema().addProperty("fromA", new IntegerSchema().example(1)));
            ComposedSchema b = new ComposedSchema();
            b.addAllOfItem(ref("A"));
            b.addAllOfItem(new ObjectSchema().addProperty("fromB", new IntegerSchema().example(2)));
            Map<String, Schema> components = schemas(Map.entry("A", a), Map.entry("B", b));

            JsonNode value = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> modelExample(components, "A"));

            assertEquals(1, value.get("fromA").asInt());
            assertEquals(2, value.get("fromB").asInt());
        }

        @Test
        @DisplayName("CT-EG-47: modelo array de nível superior gera array dos itens")
        void topLevelArrayModel() {
            Map<String, Schema> components = schemas(Map.entry("Tags", new ArraySchema().items(new DateSchema())));

            JsonNode value = modelExample(components, "Tags");

            assertTrue(value.isArray(), value.toString());
            assertEquals("2000-01-23", value.get(0).asText());
        }

        @Test
        @DisplayName("CT-EG-48: propriedade object sem propriedades gera um objeto JSON")
        void emptyInlineObjectIsJsonObject() {
            JsonNode value = propertyInModel("metadata", new ObjectSchema());

            assertTrue(value.isObject(), "type: object property generated as " + value.getNodeType() + " " + value);
        }
    }

    // ------------------------------------------------- R06: determinism

    @Nested
    @DisplayName("R06 - reprodutibilidade")
    class Determinism {

        private Map<String, Schema> numericModel() {
            return schemas(Map.entry("Measure", new ObjectSchema()
                    .addProperty("count", new IntegerSchema())
                    .addProperty("ratio", new NumberSchema().format("double"))
                    .addProperty("weight", new NumberSchema().format("float"))
                    .addProperty("big", new IntegerSchema().format("int64").minimum(BigDecimal.valueOf(1000)))));
        }

        @Test
        @DisplayName("CT-EG-39: duas instâncias com a mesma entrada geram o mesmo exemplo")
        void sameInputSameOutput() {
            List<Map<String, String>> first = generator(numericModel()).generate(null, List.of(JSON), "Measure");
            List<Map<String, String>> second = generator(numericModel()).generate(null, List.of(JSON), "Measure");

            assertEquals(first, second);
        }

        @Test
        @DisplayName("CT-EG-40: a mesma instância chamada duas vezes devolve o mesmo exemplo do modelo")
        void repeatedCallsAreStable() {
            ExampleGenerator generator = generator(numericModel());

            String first = generator.generate(null, List.of(JSON), "Measure").get(0).get("example");
            String second = generator.generate(null, List.of(JSON), "Measure").get(0).get("example");

            assertEquals(first, second);
        }
    }
}
