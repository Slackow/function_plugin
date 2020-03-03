package org.antlr.jetbrains.func.structview;

import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiFile;
import org.antlr.jetbrains.func.Icons;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class FuncRootPresentation implements ItemPresentation {
	protected final PsiFile element;

	protected FuncRootPresentation(PsiFile element) {
		this.element = element;
	}

	@Nullable
	@Override
	public Icon getIcon(boolean unused) {
		return Icons.FUNC_ICON;
	}

	@Nullable
	@Override
	public String getPresentableText() {
		return element.getVirtualFile().getNameWithoutExtension();
	}

	@Nullable
	@Override
	public String getLocationString() {
		return null;
	}
}
