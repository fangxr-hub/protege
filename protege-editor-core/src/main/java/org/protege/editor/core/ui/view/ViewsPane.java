package org.protege.editor.core.ui.view;

import org.coode.mdock.*;
import org.protege.editor.core.ProtegeProperties;
import org.protege.editor.core.prefs.Preferences;
import org.protege.editor.core.prefs.PreferencesManager;
import org.protege.editor.core.ui.workspace.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import java.awt.*;
import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Author: Matthew Horridge<br> The University Of Manchester<br> Medical Informatics Group<br> Date:
 * 15-May-2006<br><br>

 * matthew.horridge@cs.man.ac.uk<br> www.cs.man.ac.uk/~horridgm<br><br>

 * A panel that contains views.  Each panel has an id.
 */
public class ViewsPane extends JPanel {

    private static final Pattern COMPONENT_LABEL_PATTERN = Pattern.compile(
            "(<Component\\s+label\\s*=\\s*\")([^\"]*)(\"\\s*>\\s*<Property\\s+id\\s*=\\s*\"pluginId\"\\s+value\\s*=\\s*\")([^\"]*)(\"\\s*/>)",
            Pattern.DOTALL
    );

    private final Logger logger = LoggerFactory.getLogger(ViewsPane.class);

    private ViewsPaneMemento memento;

    private DynamicConfigPanel dynamicConfigPanel;

    private NodePanel nodePanel;


    @SuppressWarnings("unchecked")
    public ViewsPane(Workspace workspace, ViewsPaneMemento memento) {
        this.memento = memento;
        setLayout(new BorderLayout());

        // We need to read in a views configuration file.  Either we find
        // a customised one, or we read in the default one.

        // See if there is a customised file
        String serialisedViews = readViewLayout();
        Reader reader = null;
        if (serialisedViews.length() != 0 && !memento.isForceReset()) {
            // Got a previous config and not trying to reset
            reader = new StringReader(localizeViewConfig(serialisedViews));
        }
        else {
            // Try and restore
            if(memento.getInitialCongigFileURL() != null) {
                // No file, so default to default one :)
                try {
                    String config = readFully(new InputStreamReader(new BufferedInputStream(memento.getInitialCongigFileURL().openStream())));
                    reader = new StringReader(localizeViewConfig(config));
                }
                catch (IOException e) {
                    logger.error("An error occurred whilst loading a views configuration file: {}", e);
                }
            }
        }
        
        if (reader != null) {
            // Got our config file.  Attempt to reannimate the views.
            NodeReanimator nodeReanimator = new NodeReanimator(reader, new ViewComponentFactory(workspace));
            SplitterNode node = nodeReanimator.getRootNode();
            nodePanel = new NodePanel(node);
            add(nodePanel);
            dynamicConfigPanel = new DynamicConfigPanel(nodePanel);
        }
        else {
            // There isn't even a default xml config file.  We don't want the system
            // to keel over, so just create a blank panel (the user can drag views on
            // to it as they wish).
            VerticalSplitterNode node = new VerticalSplitterNode(Collections.emptyList(), Collections.emptyList());
            nodePanel = new NodePanel(node);
            add(nodePanel);
            dynamicConfigPanel = new DynamicConfigPanel(nodePanel);
        }
    }


    public void dispose() {
        // Dispose of the views
        logger.debug("Disposing of views");
        for (View view : getViews()) {
            view.dispose();
        }
    }


    public Set<View> getViews() {
        Set<View> views = new HashSet<>();
        getViews(this, views);
        return views;
    }


    private static void getViews(Component c, Set<View> result) {
        if (c instanceof View) {
            result.add((View) c);
        }
        if (c instanceof Container) {
            Component[] components = ((Container) c).getComponents();
            for (Component comp : components) {
                getViews(comp, result);
            }
        }
    }


    public void saveViews(Writer writer) {
        try {
            NodeSerialiser nodeSerialiser = new NodeSerialiser(nodePanel.getRootNode(),
                                                               new ViewComponentPropertiesFactory(),
                                                               writer);
            nodeSerialiser.serialise();
            writer.flush();
        }
        catch (ParserConfigurationException | IOException | TransformerFactoryConfigurationError | TransformerException e) {
            logger.error("An error occurred whilst saving a views configuration file: {}", e);
        }
    }


    public void saveViews() {
        StringWriter writer = new StringWriter();
        saveViews(writer);
        storeViewLayout(writer.getBuffer().toString());
    }


    public boolean containsView(String id) {
        for (View view : getViews()) {
            if (id.equals(view.getId())) {
                return true;
            }
        }
        return false;
    }


    public void addView(View view, String label) {
        view.createUI();
        dynamicConfigPanel.setCurrentComponent(view, label);
        dynamicConfigPanel.activate();
    }


    public void bringViewToFront(String id) {
        for (View view : getViews()) {
            if (view.getId() != null && view.getId().equals(id)) {
                Util.bringToFront(view);
                // Carry on until all views with the specified
                // view id are in front
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////

    // We now save view layout information in the preferences.  This means that view layout
    // don't disappear between installations

    public static final String VIEW_LAYOUT_PREFERENCES_ID = "ViewLayoutPreferences";


    /**
     * Gets the layout preferences set.  This is common amongst all views
     * @return The preferences.
     */
    public static Preferences getViewLayoutPreferences() {
        PreferencesManager prefsMan = PreferencesManager.getInstance();
        return prefsMan.getApplicationPreferences(VIEW_LAYOUT_PREFERENCES_ID);
    }


    /**
     * Gets the layout preferences key for this view panel
     * @return The key
     */
    public String getLayoutPreferencesKey() {
        return "protege-4.1." + memento.getViewPaneId();
    }


    /**
     * Stores a serialisation of a view layout in the preferences system
     * @param serialisation The serialisation to be stored.
     */
    public void storeViewLayout(String serialisation) {
        getViewLayoutPreferences().putString(getLayoutPreferencesKey(), serialisation);
    }


    /**
     * Reads a serialisation of a view layout from the preferences system
     * @return The serialisation, or the empty string if no serialisation
     * was previously stored.
     */
    public String readViewLayout() {
        return getViewLayoutPreferences().getString(getLayoutPreferencesKey(), "");
    }

    private static String localizeViewConfig(String config) {
        if (config == null || config.isEmpty()) {
            return config;
        }
        Matcher matcher = COMPONENT_LABEL_PATTERN.matcher(config);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String componentLabel = matcher.group(2);
            String pluginId = matcher.group(4);
            String resolved = resolveComponentLabel(componentLabel, pluginId);
            matcher.appendReplacement(result, matcher.group(1) + escapeXmlAttribute(resolved) + matcher.group(3) + pluginId + matcher.group(5));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String resolveComponentLabel(String labelValue, String pluginId) {
        if (labelValue == null || pluginId == null) {
            return "";
        }
        String protegePropertyKey = null;
        if (labelValue.startsWith("@") && labelValue.length() > 1) {
            protegePropertyKey = labelValue.substring(1);
        }
        else {
            protegePropertyKey = resolveLegacyLabelKey(labelValue, pluginId);
        }
        if (protegePropertyKey == null) {
            return labelValue;
        }
        String resolved = ProtegeProperties.getInstance().getProperty(protegePropertyKey);
        return resolved != null ? resolved : labelValue;
    }

    private static String resolveLegacyLabelKey(String labelValue, String pluginId) {
        if ("org.protege.editor.owl.OWLMembersList".equals(pluginId) && "Individuals".equals(labelValue)) {
            return "i18n.owl.label.directInstances";
        }
        if ("org.protege.editor.owl.OWLInferredMembersList".equals(pluginId) && "Individuals (Inferred)".equals(labelValue)) {
            return "i18n.owl.label.directInstancesInferred";
        }
        if ("org.protege.editor.owl.OWLIndividualsList".equals(pluginId) && "Individuals".equals(labelValue)) {
            return "i18n.owl.label.individuals";
        }
        switch (labelValue) {
            case "Classes":
                return "i18n.owl.label.classes";
            case "Object properties":
                return "i18n.owl.label.objectProperties";
            case "Data properties":
                return "i18n.owl.label.dataProperties";
            case "Annotation properties":
                return "i18n.owl.label.annotationProperties";
            case "Datatypes":
                return "i18n.owl.label.datatypes";
            case "Selected entity":
                return "i18n.owl.label.selectedEntity";
            case "Class hierarchy":
                return "i18n.owl.label.classHierarchy";
            case "Class hierarchy (inferred)":
                return "i18n.owl.label.classHierarchyInferred";
            case "Object property hierarchy":
                return "i18n.owl.label.objectPropertyHierarchy";
            case "Data property hierarchy":
                return "i18n.owl.label.dataPropertyHierarchy";
            case "Annotation property hierarchy":
                return "i18n.owl.label.annotationPropertyHierarchy";
            case "Direct instances":
                return "i18n.owl.label.directInstances";
            case "Direct instances (inferred)":
                return "i18n.owl.label.directInstancesInferred";
            case "Ontology imports":
                return "i18n.owl.label.importedOntologies";
            case "Ontology Prefixes":
                return "i18n.owl.label.ontologyPrefixes";
            case "Ontology metrics":
            case "Metrics":
                return "i18n.owl.label.ontologyMetrics";
            case "General class axioms":
                return "i18n.owl.label.generalClassAxioms";
            case "Annotations":
                return "i18n.owl.label.annotations";
            case "Usage":
                return "i18n.owl.label.usage";
            case "Characteristics":
                return "i18n.owl.label.characteristics";
            case "Description":
                return "i18n.owl.label.description";
            case "Relationships":
                return "i18n.owl.label.propertyAssertions";
            case "Inferred axioms":
                return "i18n.owl.label.inferredAxioms";
            default:
                return null;
        }
    }

    private static String readFully(Reader reader) throws IOException {
        StringWriter writer = new StringWriter();
        char[] buffer = new char[8192];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, read);
        }
        return writer.toString();
    }

    private static String escapeXmlAttribute(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                    .replace("\"", "&quot;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }
}
