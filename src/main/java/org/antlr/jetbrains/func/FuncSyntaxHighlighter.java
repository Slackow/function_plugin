package org.antlr.jetbrains.func;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.adaptor.lexer.ANTLRLexerAdaptor;
import org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;
import org.antlr.jetbrains.func.parser.FuncLexer;
import org.antlr.jetbrains.func.parser.FuncParser;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

/** A highlighter is really just a mapping from token type to
 *  some text attributes using {@link #getTokenHighlights(IElementType)}.
 *  The reason that it returns an array, TextAttributesKey[], is
 *  that you might want to mix the attributes of a few known highlighters.
 *  A {@link TextAttributesKey} is just a name for that a theme
 *  or IDE skin can set. For example, {@link com.intellij.openapi.editor.DefaultLanguageHighlighterColors#KEYWORD}
 *  is the key that maps to what identifiers look like in the editor.
 *  To change it, see dialog: Editor > Colors & Fonts > Language Defaults.
 *
 *  From <a href="http://www.jetbrains.org/intellij/sdk/docs/reference_guide/custom_language_support/syntax_highlighting_and_error_highlighting.html">doc</a>:
 *  "The mapping of the TextAttributesKey to specific attributes used
 *  in an editor is defined by the EditorColorsScheme class, and can
 *  be configured by the user if the plugin provides an appropriate
 *  configuration interface.
 *  ...
 *  The syntax highlighter returns the {@link TextAttributesKey}
 * instances for each token type which needs special highlighting.
 * For highlighting lexer errors, the standard TextAttributesKey
 * for bad characters HighlighterColors.BAD_CHARACTER can be used."
 */
public class FuncSyntaxHighlighter extends SyntaxHighlighterBase {
	private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];
	public static final TextAttributesKey ID =
		createTextAttributesKey("FUNC_ID", DefaultLanguageHighlighterColors.IDENTIFIER);
	public static final TextAttributesKey KEYWORD =
		createTextAttributesKey("FUNC_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
	public static final TextAttributesKey STRING =
		createTextAttributesKey("FUNC_STRING", DefaultLanguageHighlighterColors.STRING);
	public static final TextAttributesKey LINE_COMMENT =
		createTextAttributesKey("FUNC_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
	public static final TextAttributesKey ESCAPE =
			createTextAttributesKey("FUNC_ESCAPE", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);
	public static final TextAttributesKey SEMI =
			createTextAttributesKey("FUNC_SEMI", DefaultLanguageHighlighterColors.SEMICOLON);
	public static final TextAttributesKey PAREN = createTextAttributesKey("FUNC_PAREN", DefaultLanguageHighlighterColors.PARENTHESES);
	static {
		//noinspection deprecation

		PSIElementTypeFactory.defineLanguageIElementTypes(FuncLanguage.INSTANCE,
		                                                  FuncParser.tokenNames,
		                                                  FuncParser.ruleNames);
	}

	@NotNull
	@Override
	public Lexer getHighlightingLexer() {
		FuncLexer lexer = new FuncLexer(null);
		return new ANTLRLexerAdaptor(FuncLanguage.INSTANCE, lexer);
	}

	@NotNull
	@Override
	public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
		if ( !(tokenType instanceof TokenIElementType) ) return EMPTY_KEYS;
		TokenIElementType myType = (TokenIElementType)tokenType;
		int ttype = myType.getANTLRTokenType();
		TextAttributesKey attrKey;
		switch ( ttype ) {
			case FuncLexer.IDEN :
			case FuncLexer.ID_INTERP:
			case FuncLexer.ID_INTERP_COMMAND:
				attrKey = ID;
				break;
			case FuncLexer.VAR :
			case FuncLexer.WHILE :
			case FuncLexer.IF :
			case FuncLexer.ELSE :
			case FuncLexer.RETURN :
			case FuncLexer.LOG :
			case FuncLexer.FUNCTION :
			case FuncLexer.BOOL:
			case FuncLexer.CONSTRUCTOR :
			case FuncLexer.CONTINUE :
			case FuncLexer.BREAK :
			case FuncLexer.NULL :
			case FuncLexer.THIS :
			case FuncLexer.ASSERT:
			case FuncLexer.CASE:
			case FuncLexer.CLASS:
			case FuncLexer.DO:
			case FuncLexer.FOR:
			case FuncLexer.GEN:
			case FuncLexer.IN:
			case FuncLexer.IS:
			case FuncLexer.LOAD:
			case FuncLexer.INSTANCE:
			case FuncLexer.SWITCH:
			case FuncLexer.TICK:
			case FuncLexer.THROW:
			case FuncLexer.OPEN_FUNCTION:
			case FuncLexer.THIS_FUNCTION:
				attrKey = KEYWORD;
				break;
			case FuncLexer.OPEN_STRING:
			case FuncLexer.CLOSE_STRING:
			case FuncLexer.TEXT:
			case FuncLexer.OPEN_FUNCTION_NAME:
				attrKey = STRING;
				break;
			case FuncLexer.COMMENT :
				attrKey = LINE_COMMENT;
				break;
			case FuncLexer.ESCAPE:
				attrKey = ESCAPE;
				break;
			case FuncLexer.SEMI:
				attrKey = SEMI;
				break;
			case FuncLexer.LPAREN:
			case FuncLexer.RPAREN:
				attrKey = PAREN;
				break;
			default:
				return EMPTY_KEYS;
		}
		return new TextAttributesKey[] {attrKey};
	}

}
