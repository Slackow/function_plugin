package org.antlr.jetbrains.func.structview;

import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.Sorter;
import org.antlr.jetbrains.func.psi.FuncPSIFileRoot;
import org.jetbrains.annotations.NotNull;

public class FuncStructureViewModel
	extends StructureViewModelBase
	implements StructureViewModel.ElementInfoProvider
{
	public FuncStructureViewModel(FuncPSIFileRoot root) {
		super(root, new FuncStructureViewRootElement(root));
	}

	@NotNull
	public Sorter[] getSorters() {
		return new Sorter[] {Sorter.ALPHA_SORTER};
	}

	@Override
	public boolean isAlwaysLeaf(StructureViewTreeElement element) {
  		return !isAlwaysShowsPlus(element);
	}

	@Override
	public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
		Object value = element.getValue();
  		return value instanceof FuncPSIFileRoot;
	}
}
