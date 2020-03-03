package org.antlr.jetbrains.func.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class VariableRef extends FuncElementRef {
	public VariableRef(@NotNull IdentifierPSINode element) {
		super(element);
	}

	@Override
	public boolean isDefSubtree(PsiElement def) {
		return def instanceof VardefSubtree;
	}
}
