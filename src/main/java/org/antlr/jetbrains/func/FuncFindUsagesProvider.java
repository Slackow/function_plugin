package org.antlr.jetbrains.func;

import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import org.antlr.intellij.adaptor.lexer.RuleIElementType;
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode;
import org.antlr.jetbrains.func.psi.FunctionSubtree;
import org.antlr.jetbrains.func.psi.IdentifierPSINode;
import org.antlr.jetbrains.func.psi.VardefSubtree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.antlr.jetbrains.func.parser.FuncParser.*;

public class FuncFindUsagesProvider implements FindUsagesProvider {
	/** Is "find usages" meaningful for a kind of definition subtree? */
	@Override
	public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
		return psiElement instanceof IdentifierPSINode || // the case where we highlight the ID in def subtree itself
			   psiElement instanceof FunctionSubtree ||   // remaining cases are for resolve() results
			   psiElement instanceof VardefSubtree;
	}

	@Nullable
	@Override
	public WordsScanner getWordsScanner() {
		return null; // null implies use SimpleWordScanner default
	}

	@Nullable
	@Override
	public String getHelpId(@NotNull PsiElement psiElement) {
		return null;
	}

	/** What kind of thing is the ID node? Can group by in "Find Usages" dialog */
	@NotNull
	@Override
	public String getType(PsiElement element) {
		// The parent of an ID node will be a RuleIElementType:
		// function, vardef, formal_arg, statement, expr, call_expr, primary
		ANTLRPsiNode parent = (ANTLRPsiNode)element.getParent();
		RuleIElementType elType = (RuleIElementType)parent.getNode().getElementType();
		switch ( elType.getRuleIndex() ) {
			case RULE_function_line :
			case RULE_function_call_line :
				return "function";
			case RULE_var_definition :
			case RULE_var_modification:
			case RULE_statement :
			case RULE_expr :
				return "variable";
		}
		return "";
	}

	@NotNull
	@Override
	public String getDescriptiveName(PsiElement element) {
		return element.getText();
	}

	@NotNull
	@Override
	public String getNodeText(PsiElement element, boolean useFullName) {
		return element.getText();
	}
}
