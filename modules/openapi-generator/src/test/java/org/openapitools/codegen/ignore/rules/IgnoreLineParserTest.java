package org.openapitools.codegen.ignore.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.openapitools.codegen.ignore.rules.IgnoreLineParser.Token;

/**
 * Testes derivados exclusivamente da aba "Matriz de Testes" do oráculo.
 * Nenhum comportamento esperado adicional foi inferido da implementação.
 */
class IgnoreLineParserTest {

    private static ExpectedPart part(Token token, String value) {
        return new ExpectedPart(token, value);
    }

    private static String describeParts(List<Part> parts) {
        return parts.stream()
                .map(part -> part.getToken() + "(\"" + part.getValue() + "\")")
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static void assertParts(List<Part> actual, ExpectedPart... expected) {
        assertNotNull(actual);
        assertEquals(expected.length, actual.size(),
                "Quantidade de Part inesperada. Obtido: " + describeParts(actual));

        for (int i = 0; i < expected.length; i++) {
            Part actualPart = actual.get(i);
            ExpectedPart expectedPart = expected[i];

            assertNotNull(actualPart, "Part nulo na posição " + i);
            assertNotNull(actualPart.getToken(), "Token nulo na posição " + i);
            assertEquals(expectedPart.token, actualPart.getToken(),
                    "Token inesperado na posição " + i + ". Obtido: " + describeParts(actual));
            assertEquals(expectedPart.value, actualPart.getValue(),
                    "value inesperado na posição " + i + ". Obtido: " + describeParts(actual));
        }
    }

    private static final class ExpectedPart {
        private final Token token;
        private final String value;

        private ExpectedPart(Token token, String value) {
            this.token = token;
            this.value = value;
        }
    }

    @Test
    void t01_FronteiraInferiorDaEntrada() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("");
        assertParts(result);
    }

    @Test
    void t02_TokenizacaoDeTextoComum() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("file");
        assertParts(
                result,
                part(Token.TEXT, "file")
        );
    }

    @Test
    void t03_PreservarAOrdemRelativaEntreTextoMetacaractereETexto() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("abc*def");
        assertParts(
                result,
                part(Token.TEXT, "abc"),
                part(Token.MATCH_ANY, "*"),
                part(Token.TEXT, "def")
        );
    }

    @Test
    void t04_ReconhecerComentarioIniciadoNaPosicaoZero() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("#comentario");
        assertParts(
                result,
                part(Token.COMMENT, "#comentario")
        );
    }

    @Test
    void t05_VerificarPrecedenciaDeCOMMENTSobreExclamacaoAsteriscoasteriscoasterisc() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("# !*** /");
        assertParts(
                result,
                part(Token.COMMENT, "# !*** /")
        );
    }

    @Test
    void t06_IsolarOTratamentoDoHashInicialEscapado() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("\\#foo");
        assertParts(
                result,
                part(Token.TEXT, "#foo")
        );
    }

    @Test
    void t07_AplicarOHashInicialEscapadoAUmNomeQueContemOutros() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("\\##emacs_bak_file##");
        assertParts(
                result,
                part(Token.TEXT, "##emacs_bak_file##")
        );
    }

    @Test
    void t08_DistinguirInicialDeInterno() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("a#b");
        assertParts(
                result,
                part(Token.TEXT, "a#b")
        );
    }

    @Test
    void t09_RejeitarNegacaoSemPadrao() throws ParserException {
        assertThrows(ParserException.class, () -> IgnoreLineParser.parse("!"));
    }

    @Test
    void t10_ReconhecerNegacaoNaPosicaoInicial() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("!file");
        assertParts(
                result,
                part(Token.NEGATE, "!"),
                part(Token.TEXT, "file")
        );
    }

    @Test
    void t11_DistinguirExclamacaoInicialDeExclamacaoInterno() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("a!b");
        assertParts(
                result,
                part(Token.TEXT, "a!b")
        );
    }

    @Test
    void t12_ReconhecerExclamacaoEscapadaIsolada() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("\\!");
        assertParts(
                result,
                part(Token.ESCAPED_EXCLAMATION, "\\!")
        );
    }

    @Test
    void t13_ContinuarAAnaliseAposAExclamacaoEscapada() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("\\!foo");
        assertParts(
                result,
                part(Token.ESCAPED_EXCLAMATION, "\\!"),
                part(Token.TEXT, "foo")
        );
    }

    @Test
    void t14_ReconhecerEspacoEscapadoIsolado() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("\\ ");
        assertParts(
                result,
                part(Token.ESCAPED_SPACE, "\\ ")
        );
    }

    @Test
    void t15_ContinuarAAnaliseAposEspacoEscapado() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("\\ foo");
        assertParts(
                result,
                part(Token.ESCAPED_SPACE, "\\ "),
                part(Token.TEXT, "foo")
        );
    }

    @Test
    void t16_RejeitarAsteriscoasteriscoasteriscoQuandoHaSufixo() throws ParserException {
        assertThrows(ParserException.class, () -> IgnoreLineParser.parse("***a"));
    }

    @Test
    void t17_RejeitarQuatroAsteriscosConsecutivos() throws ParserException {
        assertThrows(ParserException.class, () -> IgnoreLineParser.parse("****"));
    }

    @Test
    void t18_RejeitarAsteriscoasteriscoasteriscoNoLimiteExatoDaString() throws ParserException {
        assertThrows(ParserException.class, () -> IgnoreLineParser.parse("***"));
    }

    @Test
    void t19_RejeitarAsteriscoasteriscoasteriscoQuandoOcorreNoMeioDaLinha() throws ParserException {
        assertThrows(ParserException.class, () -> IgnoreLineParser.parse("a***b"));
    }

    @Test
    void t20_IsolarMATCHALLEmContextoInequivoco() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("**");
        assertParts(
                result,
                part(Token.MATCH_ALL, "**")
        );
    }

    @Test
    void t21_ReconhecerAsteriscoasteriscoAposComponenteEDelimitador() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("dir/**");
        assertParts(
                result,
                part(Token.TEXT, "dir"),
                part(Token.PATH_DELIM, "/"),
                part(Token.MATCH_ALL, "**")
        );
    }

    @Test
    void t22_IsolarMATCHANY() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("*");
        assertParts(
                result,
                part(Token.MATCH_ANY, "*")
        );
    }

    @Test
    void t23_ReconhecerAsteriscoAposComponenteEDelimitador() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("dir/*");
        assertParts(
                result,
                part(Token.TEXT, "dir"),
                part(Token.PATH_DELIM, "/"),
                part(Token.MATCH_ANY, "*")
        );
    }

    @Test
    void t24_ReconhecerBarraInicial() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("/dir");
        assertParts(
                result,
                part(Token.ROOTED_MARKER, "/"),
                part(Token.TEXT, "dir")
        );
    }

    @Test
    void t25_ReconhecerBarraFinal() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("dir/");
        assertParts(
                result,
                part(Token.TEXT, "dir"),
                part(Token.DIRECTORY_MARKER, "/")
        );
    }

    @Test
    void t26_ReconhecerBarraInterna() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("dir/file");
        assertParts(
                result,
                part(Token.TEXT, "dir"),
                part(Token.PATH_DELIM, "/"),
                part(Token.TEXT, "file")
        );
    }

    @Test
    void t27_IgnorarEspacosFinaisNaoEscapados() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("file  ");
        assertParts(
                result,
                part(Token.TEXT, "file")
        );
    }

    @Test
    void t28_PreservarEspacoComumQuandoEInterno() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("foo bar");
        assertParts(
                result,
                part(Token.TEXT, "foo bar")
        );
    }

    @Test
    void t29_PreservarOrdemQuandoHaTextoAntesEDepoisDeEscapeexclamacao() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("a\\!b");
        assertParts(
                result,
                part(Token.TEXT, "a"),
                part(Token.ESCAPED_EXCLAMATION, "\\!"),
                part(Token.TEXT, "b")
        );
    }

    @Test
    void t30_DistinguirEspacoFinalEscapadoDeEspacoFinalComumEPreservarOrdem() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("foo\\ ");
        assertParts(
                result,
                part(Token.TEXT, "foo"),
                part(Token.ESCAPED_SPACE, "\\ ")
        );
    }

    @Test
    void t31_PreservarPosicaoDeEspacoEscapadoEntreDoisTextos() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("a\\ b");
        assertParts(
                result,
                part(Token.TEXT, "a"),
                part(Token.ESCAPED_SPACE, "\\ "),
                part(Token.TEXT, "b")
        );
    }

    @Test
    void t32_RejeitarAsteriscoasteriscoasteriscoTerminandoExatamenteNoFimAposUmPref() throws ParserException {
        assertThrows(ParserException.class, () -> IgnoreLineParser.parse("a***"));
    }

    @Test
    void t33_ExercitarMATCHALLNoInicioSeguidoDeDelimitadorETexto() throws ParserException {
        List<Part> result = IgnoreLineParser.parse("**/foo");
        assertParts(
                result,
                part(Token.MATCH_ALL, "**"),
                part(Token.PATH_DELIM, "/"),
                part(Token.TEXT, "foo")
        );
    }

}