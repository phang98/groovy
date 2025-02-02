/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package groovy.console.ui.text;

import groovy.lang.Tuple2;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.Token;
import org.apache.groovy.parser.antlr4.GroovyLangLexer;
import org.apache.groovy.parser.antlr4.GroovySyntaxError;
import org.apache.groovy.parser.antlr4.util.PositionConfigureUtils;
import org.apache.groovy.parser.antlr4.util.StringUtils;
import org.apache.groovy.util.ReversedList;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.DocumentFilter;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import java.awt.Color;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.groovy.parser.antlr4.GroovyLexer.ABSTRACT;
import static org.apache.groovy.parser.antlr4.GroovyLexer.AS;
import static org.apache.groovy.parser.antlr4.GroovyLexer.ASSERT;
import static org.apache.groovy.parser.antlr4.GroovyLexer.BREAK;
import static org.apache.groovy.parser.antlr4.GroovyLexer.BooleanLiteral;
import static org.apache.groovy.parser.antlr4.GroovyLexer.BuiltInPrimitiveType;
import static org.apache.groovy.parser.antlr4.GroovyLexer.CASE;
import static org.apache.groovy.parser.antlr4.GroovyLexer.CATCH;
import static org.apache.groovy.parser.antlr4.GroovyLexer.CLASS;
import static org.apache.groovy.parser.antlr4.GroovyLexer.COMMA;
import static org.apache.groovy.parser.antlr4.GroovyLexer.CONST;
import static org.apache.groovy.parser.antlr4.GroovyLexer.CONTINUE;
import static org.apache.groovy.parser.antlr4.GroovyLexer.DEF;
import static org.apache.groovy.parser.antlr4.GroovyLexer.DEFAULT;
import static org.apache.groovy.parser.antlr4.GroovyLexer.DO;
import static org.apache.groovy.parser.antlr4.GroovyLexer.ELSE;
import static org.apache.groovy.parser.antlr4.GroovyLexer.ENUM;
import static org.apache.groovy.parser.antlr4.GroovyLexer.EOF;
import static org.apache.groovy.parser.antlr4.GroovyLexer.EXTENDS;
import static org.apache.groovy.parser.antlr4.GroovyLexer.FINAL;
import static org.apache.groovy.parser.antlr4.GroovyLexer.FINALLY;
import static org.apache.groovy.parser.antlr4.GroovyLexer.FOR;
import static org.apache.groovy.parser.antlr4.GroovyLexer.FloatingPointLiteral;
import static org.apache.groovy.parser.antlr4.GroovyLexer.GOTO;
import static org.apache.groovy.parser.antlr4.GroovyLexer.GStringBegin;
import static org.apache.groovy.parser.antlr4.GroovyLexer.GStringEnd;
import static org.apache.groovy.parser.antlr4.GroovyLexer.GStringPart;
import static org.apache.groovy.parser.antlr4.GroovyLexer.IF;
import static org.apache.groovy.parser.antlr4.GroovyLexer.IMPLEMENTS;
import static org.apache.groovy.parser.antlr4.GroovyLexer.IMPORT;
import static org.apache.groovy.parser.antlr4.GroovyLexer.IN;
import static org.apache.groovy.parser.antlr4.GroovyLexer.INSTANCEOF;
import static org.apache.groovy.parser.antlr4.GroovyLexer.INTERFACE;
import static org.apache.groovy.parser.antlr4.GroovyLexer.IntegerLiteral;
import static org.apache.groovy.parser.antlr4.GroovyLexer.NATIVE;
import static org.apache.groovy.parser.antlr4.GroovyLexer.NEW;
import static org.apache.groovy.parser.antlr4.GroovyLexer.NL;
import static org.apache.groovy.parser.antlr4.GroovyLexer.NON_SEALED;
import static org.apache.groovy.parser.antlr4.GroovyLexer.NullLiteral;
import static org.apache.groovy.parser.antlr4.GroovyLexer.PACKAGE;
import static org.apache.groovy.parser.antlr4.GroovyLexer.PERMITS;
import static org.apache.groovy.parser.antlr4.GroovyLexer.PRIVATE;
import static org.apache.groovy.parser.antlr4.GroovyLexer.PROTECTED;
import static org.apache.groovy.parser.antlr4.GroovyLexer.PUBLIC;
import static org.apache.groovy.parser.antlr4.GroovyLexer.RETURN;
import static org.apache.groovy.parser.antlr4.GroovyLexer.SEALED;
import static org.apache.groovy.parser.antlr4.GroovyLexer.SEMI;
import static org.apache.groovy.parser.antlr4.GroovyLexer.STATIC;
import static org.apache.groovy.parser.antlr4.GroovyLexer.STRICTFP;
import static org.apache.groovy.parser.antlr4.GroovyLexer.SUPER;
import static org.apache.groovy.parser.antlr4.GroovyLexer.SWITCH;
import static org.apache.groovy.parser.antlr4.GroovyLexer.SYNCHRONIZED;
import static org.apache.groovy.parser.antlr4.GroovyLexer.StringLiteral;
import static org.apache.groovy.parser.antlr4.GroovyLexer.THIS;
import static org.apache.groovy.parser.antlr4.GroovyLexer.THREADSAFE;
import static org.apache.groovy.parser.antlr4.GroovyLexer.THROW;
import static org.apache.groovy.parser.antlr4.GroovyLexer.THROWS;
import static org.apache.groovy.parser.antlr4.GroovyLexer.TRAIT;
import static org.apache.groovy.parser.antlr4.GroovyLexer.TRANSIENT;
import static org.apache.groovy.parser.antlr4.GroovyLexer.TRY;
import static org.apache.groovy.parser.antlr4.GroovyLexer.UNEXPECTED_CHAR;
import static org.apache.groovy.parser.antlr4.GroovyLexer.VAR;
import static org.apache.groovy.parser.antlr4.GroovyLexer.VOID;
import static org.apache.groovy.parser.antlr4.GroovyLexer.VOLATILE;
import static org.apache.groovy.parser.antlr4.GroovyLexer.WHILE;
import static org.apache.groovy.parser.antlr4.GroovyLexer.YIELD;


/**
 * The document filter based on Parrot's lexer is for highlighting the content of text editor
 *
 * @since 3.0.0
 */
public class SmartDocumentFilter extends DocumentFilter {
    private static final String MONOSPACED = "Monospaced";
    private final DefaultStyledDocument styledDocument;
    private final StyleContext styleContext;
    private final Style defaultStyle;

    public SmartDocumentFilter(DefaultStyledDocument styledDocument) {
        this.styledDocument = styledDocument;

        this.styleContext = StyleContext.getDefaultStyleContext();
        this.defaultStyle = this.styleContext.getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontFamily(this.defaultStyle, MONOSPACED);

        initStyles();
    }

    @Override
    public void insertString(DocumentFilter.FilterBypass fb, int offset,
                             String text, AttributeSet attrs) throws BadLocationException {
        // remove problem meta characters returns
        text = replaceMetaCharacters(text);

        fb.insertString(offset, text, attrs);
        parseDocument();
    }

    @Override
    public void remove(DocumentFilter.FilterBypass fb, int offset, int length)
            throws BadLocationException {

        fb.remove(offset, length);
        parseDocument();
    }

    @Override
    public void replace(DocumentFilter.FilterBypass fb, int offset,
                        int length, String text, AttributeSet attrs)
            throws BadLocationException {

        // text might be null and indicates no replacement text
        if (text == null) text = "";

        // remove problem meta characters returns
        text = replaceMetaCharacters(text);

        fb.replace(offset, length, text, attrs);

        parseDocument();
    }

    private String replaceMetaCharacters(String string) {
        // just in case remove carriage returns
        string = string.replace("\r\n", "\n");
        return string;
    }

    private void parseDocument() throws BadLocationException {
        GroovyLangLexer lexer;
        try {
            lexer = createLexer(styledDocument.getText(0, styledDocument.getLength()));
        } catch (IOException e) {
            e.printStackTrace();
            this.latest = false;
            return;
        }

        CommonTokenStream tokenStream = new CommonTokenStream(lexer);

        try {
            tokenStream.fill();
        } catch (LexerNoViableAltException | GroovySyntaxError e) {
            // ignore
            this.latest = false;
            return;
        } catch (Exception e) {
            e.printStackTrace();
            this.latest = false;
            return;
        }

        List<Token> tokenList = tokenStream.getTokens();
        List<Token> tokenListToRender;
        try {
            tokenListToRender = findTokensToRender(tokenList);
        } finally {
            this.setRenderRange(null);
        }

        for (Token token : tokenListToRender) {
            int tokenType = token.getType();

//                if (token instanceof CommonToken) {
//                    System.out.println(((CommonToken) token).toString(lexer));
//                }

            if (EOF == tokenType) {
                continue;
            }

            int tokenStartIndex = token.getStartIndex();
            int tokenStopIndex = token.getStopIndex();
            int tokenLength = tokenStopIndex - tokenStartIndex + 1;

            styledDocument.setCharacterAttributes(tokenStartIndex,
                    tokenLength,
                    findStyleByTokenType(tokenType),
                    true);

            if (GStringBegin == tokenType || GStringPart == tokenType) {
                styledDocument.setCharacterAttributes(
                        tokenStartIndex + tokenLength - 1,
                        1,
                        defaultStyle,
                        true);
            }
        }

        this.latestTokenList = tokenList;
        this.latest = true;
    }

    private List<Token> findTokensToRender(List<Token> tokenList) {
        final Tuple2<Integer, Integer> renderRange = getRenderRange();
        if (null != renderRange) {
            long startLine = lineNumber(renderRange.getV1());
            long stopLine = lineNumber(renderRange.getV2());

            if (startLine < 0 || stopLine < 0) return tokenList; // should never happen

            return tokenList.stream()
                    .filter(e -> e.getLine() >= startLine && PositionConfigureUtils.endPosition(e).getV1() <= stopLine)
                    .collect(Collectors.toList());
        }

        List<Token> tmpLatestTokenList = filterNewlines(this.latestTokenList);
        int latestTokenListSize = tmpLatestTokenList.size();
        if (0 == latestTokenListSize) {
            return tokenList;
        }

        List<Token> tmpTokenList = filterNewlines(tokenList);
        int tmpTokenListSize = tmpTokenList.size();
        if (0 == tmpTokenListSize) {
            return tokenList;
        }

        int startTokenIndex = 0;
        int minSize = Math.min(tmpTokenListSize, latestTokenListSize);
        for (int i = 0; i < minSize; i++) {
            Token token = tmpTokenList.get(i);
            Token latestToken = tmpLatestTokenList.get(i);

            if (token.getType() == latestToken.getType()
                    && token.getStartIndex() == latestToken.getStartIndex()
                    && token.getStopIndex() == latestToken.getStopIndex()) {
                continue;
            }

            startTokenIndex = i;
            break;
        }

        List<Token> newTokenList = new ReversedList<>(tmpTokenList);
        List<Token> newLatestTokenList = new ReversedList<>(tmpLatestTokenList);

        int stopTokenIndex = tmpTokenListSize;

        Token lastToken = newTokenList.get(0);
        Token lastLatestToken = newLatestTokenList.get(0);

        for (int i = 0; i < minSize; i++) {
            Token token = newTokenList.get(i);
            Token latestToken = newLatestTokenList.get(i);

            if ((token.getType() == latestToken.getType())
                    && (token.getStartIndex() - lastToken.getStartIndex()) == (latestToken.getStartIndex() - lastLatestToken.getStartIndex())
                    && ((token.getStopIndex() - lastToken.getStopIndex()) == (latestToken.getStopIndex() - lastLatestToken.getStopIndex()))) {
                continue;
            }

            stopTokenIndex = tmpTokenListSize - i;
            break;
        }

        if (startTokenIndex == stopTokenIndex) {
            return tmpTokenListSize != latestTokenListSize ? tokenList : Collections.emptyList();
        } else if (startTokenIndex < stopTokenIndex) {
            return tmpTokenList.subList(startTokenIndex, stopTokenIndex);
        }

        return tokenList; // should never reach here. If unexpected error occurred, it's better to render all tokens
    }

    /**
     * Returns line number starting with 1
     */
    private long lineNumber(int position) {
        long result = -1;

        if (position < 0) return result;

        try {
            String text = this.styledDocument.getText(0, position + 1);
            result = StringUtils.countChar(text, '\n') + 1;
        } catch (BadLocationException e) {
            e.printStackTrace(System.err);
        }

        return result;
    }

    private List<Token> filterNewlines(List<Token> tokenList) {
        return tokenList.stream()
                .filter(e -> !(NL == e.getType() && e.getText().trim().isEmpty()))
                .collect(Collectors.toList());
    }

    private Style findStyleByTokenType(int tokenType) {
        Style style = styleContext.getStyle(String.valueOf(tokenType));

        return null == style ? defaultStyle : style;
    }

    private Style createDefaultStyleByTokenType(int tokenType) {
        return styleContext.addStyle(String.valueOf(tokenType), defaultStyle);
    }

    private GroovyLangLexer createLexer(String text) throws IOException {
        CharStream charStream = CharStreams.fromReader(new StringReader(text));
        GroovyLangLexer lexer = new GroovyLangLexer(charStream);

        lexer.setErrorIgnored(true);
        lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);

        return lexer;
    }

    private void initStyles() {
        Style comment = createDefaultStyleByTokenType(NL);
        StyleConstants.setForeground(comment, Color.LIGHT_GRAY.darker().darker());
        StyleConstants.setItalic(comment, true);

        // gstrings, e.g. "${xxx}", /xxx/
        for (int t : Arrays.asList(GStringBegin, GStringPart, GStringEnd)) {
            Style style = createDefaultStyleByTokenType(t);
            StyleConstants.setForeground(style, Color.MAGENTA.darker().darker());
        }

        // strings, e.g. 'xxx'
        Style stringLiteral = createDefaultStyleByTokenType(StringLiteral);
        StyleConstants.setForeground(stringLiteral, Color.GREEN.darker().darker());

        // numbers, e.g. 123, 1.23
        for (int t : Arrays.asList(IntegerLiteral, FloatingPointLiteral)) {
            Style style = createDefaultStyleByTokenType(t);
            StyleConstants.setForeground(style, Color.RED.darker());
        }

        // reserved keywords, null literals, boolean literals
        for (int t : Arrays.asList(AS, DEF, IN, TRAIT, THREADSAFE,
                VAR, BuiltInPrimitiveType, ABSTRACT, ASSERT, BREAK, CASE, CATCH, CLASS, CONST, CONTINUE, DEFAULT, DO,
                ELSE, ENUM, EXTENDS, FINAL, FINALLY, FOR, IF, GOTO, IMPLEMENTS, IMPORT, INSTANCEOF, INTERFACE,
                NATIVE, NEW, NON_SEALED, PACKAGE, PERMITS, PRIVATE, PROTECTED, PUBLIC, RETURN, SEALED, STATIC, STRICTFP, SUPER, SWITCH, SYNCHRONIZED,
                THIS, THROW, THROWS, TRANSIENT, TRY, VOID, VOLATILE, WHILE, YIELD, NullLiteral, BooleanLiteral)) {
            Style style = createDefaultStyleByTokenType(t);
            StyleConstants.setBold(style, true);
            StyleConstants.setForeground(style, Color.BLUE.darker().darker());
        }

        // commas, semicolons
        for (int t : Arrays.asList(COMMA, SEMI)) {
            Style style = createDefaultStyleByTokenType(t);
            StyleConstants.setForeground(style, Color.BLUE.darker());
        }

        // unexpected char, e.g. `
        Style unexpectedChar = createDefaultStyleByTokenType(UNEXPECTED_CHAR);
        StyleConstants.setForeground(unexpectedChar, Color.CYAN.darker());
    }

    private volatile boolean latest = false;
    private volatile List<Token> latestTokenList = Collections.emptyList();
    private volatile Tuple2<Integer, Integer> renderRange;

    public boolean isLatest() {
        return latest;
    }

    public List<Token> getLatestTokenList() {
        return latestTokenList;
    }

    public void setRenderRange(Tuple2<Integer, Integer> renderRange) {
        this.renderRange = renderRange;
    }
    public Tuple2<Integer, Integer> getRenderRange() {
        return renderRange;
    }
}
