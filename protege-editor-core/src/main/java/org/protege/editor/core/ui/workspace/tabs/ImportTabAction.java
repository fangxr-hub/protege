package org.protege.editor.core.ui.workspace.tabs;

import org.protege.editor.core.ui.action.ProtegeAction;
import org.protege.editor.core.ui.util.UIUtil;
import org.protege.editor.core.ui.workspace.TabbedWorkspace;
import org.protege.editor.core.ui.workspace.WorkspaceViewsTab;
import org.protege.editor.core.ProtegeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ImportTabAction extends ProtegeAction {

	private static final long serialVersionUID = 7952432018266375998L;

	private final Logger logger = LoggerFactory.getLogger(ImportTabAction.class);

	public ImportTabAction() {
	}

	public void initialise() throws Exception {
	}


	public void dispose() throws Exception {
	}

	public void actionPerformed(ActionEvent event) {
		TabbedWorkspace workspace = (TabbedWorkspace) getWorkspace();
        Set<String> extensions = new HashSet<>();
        extensions.add("xml");
        File f = UIUtil.openFile((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, workspace),
                ProtegeProperties.getInstance().getProperty("i18n.core.tabLayout.openDialogTitle"),
                ProtegeProperties.getInstance().getProperty("i18n.core.tabLayout.openDialogDescription"),
                extensions);
        if (f == null) {
            return;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            WorkspaceViewsTab tab = (WorkspaceViewsTab) CreateTabAction.handleCreateNewTab(workspace);
            if (tab == null) {
                return;
            }
            tab.reset(sb.toString());
        }
        catch (IOException e) {
            logger.error("An error occurred when attempting to import a tab configuration.  File: {}, Details: {}",
                    f.getAbsolutePath(), e);
            JOptionPane.showMessageDialog(workspace,
                    ProtegeProperties.getInstance().getProperty("i18n.core.tabLayout.importErrorMessage"),
                    ProtegeProperties.getInstance().getProperty("i18n.dialog.errorTitle"),
                    JOptionPane.ERROR_MESSAGE);
        }
	}

}
