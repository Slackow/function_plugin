package org.antlr.jetbrains.func.structview;

import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class FuncStructureViewRootElement extends FuncStructureViewElement {
	public FuncStructureViewRootElement(PsiFile element) {
		super(element);
	}

	@NotNull
	@Override
	public ItemPresentation getPresentation() {
		return new FuncRootPresentation((PsiFile)element);
	}
}
