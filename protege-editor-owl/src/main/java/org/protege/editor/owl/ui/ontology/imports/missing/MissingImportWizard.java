package org.protege.editor.owl.ui.ontology.imports.missing;

import org.protege.editor.core.ui.wizard.Wizard;
import org.protege.editor.core.ProtegeProperties;
import org.protege.editor.owl.OWLEditorKit;

import java.awt.*;


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Medical Informatics Group<br>
 * Date: 17-Oct-2006<br><br>

 * matthew.horridge@cs.man.ac.uk<br>
 * www.cs.man.ac.uk/~horridgm<br><br>
 */
public class MissingImportWizard extends Wizard {


    public MissingImportWizard(Frame frame, OWLEditorKit owlEditorKit) {
        super(frame);
        setTitle(i18n("i18n.owl.dialog.resolveMissingImportWizard.title", "Resolve missing import wizard"));
        registerWizardPanel(ResolutionTypePanel.ID, new ResolutionTypePanel(owlEditorKit));
        registerWizardPanel(SpecifyFilePathPanel.ID, new SpecifyFilePathPanel(owlEditorKit));
        registerWizardPanel(CopyOptionPanel.ID, new CopyOptionPanel(owlEditorKit));
        setCurrentPanel(ResolutionTypePanel.ID);
    }


    public static void main(String[] args) {
        MissingImportWizard w = new MissingImportWizard(null, null);
        w.showModalDialog();
    }

    private static String i18n(String key, String fallback) {
        String value = ProtegeProperties.getInstance().getProperty(key);
        return value != null ? value : fallback;
    }
}
