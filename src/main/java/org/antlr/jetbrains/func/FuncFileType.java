package org.antlr.jetbrains.func;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class FuncFileType extends LanguageFileType {
	public static final String FILE_EXTENSION = "function";
		public static final FuncFileType INSTANCE = new FuncFileType();

	protected FuncFileType() {
		super(FuncLanguage.INSTANCE);
	}

	@NotNull
	@Override
	public String getName() {
		return "StringTemplate v4 template group file";
	}

	@NotNull
	@Override
	public String getDescription() {
		return "StringTemplate v4 template group file";
	}

	@NotNull
	@Override
	public String getDefaultExtension() {
		return FILE_EXTENSION;
	}

	@Nullable
	@Override
	public Icon getIcon() {
		return Icons.FUNC_ICON;
	}
}
