package org.antlr.jetbrains.func.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.antlr.intellij.adaptor.SymtabUtils;
import org.antlr.intellij.adaptor.psi.ScopeNode;
import org.antlr.jetbrains.func.Icons;
import org.antlr.jetbrains.func.FuncFileType;
import org.antlr.jetbrains.func.FuncLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class FuncPSIFileRoot extends PsiFileBase implements ScopeNode {
    public FuncPSIFileRoot(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, FuncLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return FuncFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "Func Language file";
    }

    @Override
    public Icon getIcon(int flags) {
        return Icons.FUNC_ICON;
    }

	/** Return null since a file scope has no enclosing scope. It is
	 *  not itself in a scope.
	 */
	@Override
	public ScopeNode getContext() {
		return null;
	}

	@Nullable
	@Override
	public PsiElement resolve(PsiNamedElement element) {
//		System.out.println(getClass().getSimpleName()+
//		                   ".resolve("+element.getName()+
//		                   " at "+Integer.toHexString(element.hashCode())+")");
		if ( element.getParent() instanceof CallSubtree ) {
			return SymtabUtils.resolve(this, FuncLanguage.INSTANCE,
			                           element, "/script/function/ID");
		}
		return SymtabUtils.resolve(this, FuncLanguage.INSTANCE,
		                           element, "/script/vardef/ID");
	}
}
