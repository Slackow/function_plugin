package org.antlr.jetbrains.func.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.antlr.intellij.adaptor.SymtabUtils;
import org.antlr.intellij.adaptor.psi.ANTLRPsiNode;
import org.antlr.intellij.adaptor.psi.ScopeNode;
import org.antlr.jetbrains.func.FuncLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockSubtree extends ANTLRPsiNode implements ScopeNode {
	public BlockSubtree(@NotNull ASTNode node) {
		super(node);
	}

	@Nullable
	@Override
	public PsiElement resolve(PsiNamedElement element) {
//		System.out.println(getClass().getSimpleName()+
//		                   ".resolve("+element.getName()+
//		                   " at "+Integer.toHexString(element.hashCode())+")");

		return SymtabUtils.resolve(this, FuncLanguage.INSTANCE,
		                           element, "/block/vardef/ID");
	}
}
