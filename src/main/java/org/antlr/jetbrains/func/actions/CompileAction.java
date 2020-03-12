package org.antlr.jetbrains.func;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class CompileAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        VirtualFile vfile = getFuncFileFromEvent(e);
        if (vfile == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        e.getPresentation().setEnabled(true);
        e.getPresentation().setVisible(true);
    }

    public static VirtualFile getFuncFileFromEvent(AnActionEvent e) {
        VirtualFile[] files = LangDataKeys.VIRTUAL_FILE_ARRAY.getData(e.getDataContext());
        if (files == null || files.length == 0) return null;
        VirtualFile vfile = files[0];
        if (vfile != null && vfile.getName().endsWith(".function")) {
            return vfile;
        }
        return null;
    }
}
