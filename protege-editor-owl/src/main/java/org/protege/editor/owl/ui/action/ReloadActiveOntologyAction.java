package org.protege.editor.owl.ui.action;

import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.core.ProtegeProperties;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 13/03/15
 */
public class ReloadActiveOntologyAction extends ProtegeOWLAction {

    /**
     * Invoked when an action occurs.
     *
     * @param e
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        OWLModelManager modelManager = getOWLModelManager();
        OWLOntology activeOntology = modelManager.getActiveOntology();
        if (getOWLModelManager().isDirty(activeOntology)) {
            int ret = JOptionPane.showConfirmDialog(getOWLWorkspace(),
                    i18n("i18n.owl.dialog.reloadOntology.confirmMessage", "Are you sure that you want to reload the active ontology?  You will lose any unsaved changes."),
                    i18n("i18n.owl.dialog.reloadOntology.confirmTitle", "Reload ontology?"),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if(ret != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            modelManager.reload(activeOntology);
        } catch (OWLOntologyCreationException e1) {
            JOptionPane.showMessageDialog(getOWLWorkspace(),
                    i18n("i18n.owl.dialog.reloadOntology.errorMessage", "There was an error reloading the active ontology."),
                    i18n("i18n.owl.dialog.reloadOntology.errorTitle", "Error"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * The initialise method is called at the start of a
     * plugin instance life cycle.
     * This method is called to give the plugin a chance
     * to initialise itself.  All plugin initialisation
     * should be done in this method rather than the plugin
     * constructor, since the initialisation might need to
     * occur at a point after plugin instance creation, and
     * a each plugin must have a zero argument constructor.
     */
    @Override
    public void initialise() throws Exception {

    }

    @Override
    public void dispose() throws Exception {

    }

    private static String i18n(String key, String fallback) {
        String value = ProtegeProperties.getInstance().getProperty(key);
        return value != null ? value : fallback;
    }
}
