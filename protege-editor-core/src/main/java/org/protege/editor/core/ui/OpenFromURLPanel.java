package org.protege.editor.core.ui;

import org.protege.editor.core.BookMarkedURIManager;
import org.protege.editor.core.ProtegeProperties;
import org.protege.editor.core.ui.list.MList;
import org.protege.editor.core.ui.list.MListItem;
import org.protege.editor.core.ui.list.MListSectionHeader;
import org.protege.editor.core.ui.util.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static javax.swing.JOptionPane.*;


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 12-May-2007<br><br>
 */
public class OpenFromURLPanel extends JPanel implements VerifiedInputEditor {

    private static final int PREF_WIDTH = 500;

    private static final int PREF_HEIGHT = 300;

    private static String getTitle() {
        return ProtegeProperties.getInstance().getProperty("i18n.core.openFromUrl.dialogTitle");
    }

    private static String getUrlFieldPlaceholder() {
        return ProtegeProperties.getInstance().getProperty("i18n.core.openFromUrl.urlFieldPlaceholder");
    }

    private static String getUrlFieldLabel() {
        return ProtegeProperties.getInstance().getProperty("i18n.core.openFromUrl.urlFieldLabel");
    }

    private static String getBookmarkedUrlsLabel() {
        return ProtegeProperties.getInstance().getProperty("i18n.core.openFromUrl.bookmarkedUrlsLabel");
    }

    private JTextField uriField;

    private MList bookmarksList;

    private List<InputVerificationStatusChangedListener> listeners =
            new ArrayList<>();


    public OpenFromURLPanel() {
        createUI();
    }


    private void createUI() {
        uriField = new AugmentedJTextField("", 45, getUrlFieldPlaceholder());
        uriField.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent event) {
                handleValueChanged();
            }
            public void removeUpdate(DocumentEvent event) {
                handleValueChanged();
            }
            public void changedUpdate(DocumentEvent event) {
                handleValueChanged();
            }
        });

        JPanel upperGroup = new JPanel(new BorderLayout());
        upperGroup.add(new FormLabel(getUrlFieldLabel()), BorderLayout.NORTH);
        upperGroup.add(uriField, BorderLayout.SOUTH);

        JPanel lowerGroup = new JPanel(new BorderLayout());
        lowerGroup.add(new FormLabel(getBookmarkedUrlsLabel()), BorderLayout.NORTH);
        bookmarksList = new MList() {
            protected void handleAdd() {
                addURI();
            }
            protected void handleDelete() {
                deleteSelectedBookmark();
            }
        };
        bookmarksList.setCellRenderer(new BookmarkedItemListRenderer());
        JScrollPane scrollPane = new JScrollPane(bookmarksList);
        lowerGroup.add(scrollPane);
        fillList();
        bookmarksList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateTextField();
            }
        });
        setLayout(new BorderLayout(7, 7));
        add(upperGroup, BorderLayout.NORTH);
        add(lowerGroup, BorderLayout.CENTER);
        setPreferredSize(new Dimension(PREF_WIDTH, PREF_HEIGHT));
    }


    private void handleValueChanged() {
        final boolean validURI = isValidURI();
        for (InputVerificationStatusChangedListener l : listeners){
            l.verifiedStatusChanged(validURI);
        }
    }

    protected boolean isValidURI(){
        final URI uri = getURI(false);
        return uri != null && uri.isAbsolute() && uri.getScheme() != null;        
    }

    public URI getURI() {
        return getURI(true);
    }

    private URI getURI(boolean showMessage) {
        try {
            return new URI(uriField.getText().trim());
        }
        catch (URISyntaxException e) {
            if (showMessage){
                showURIErrorMessage(e);
            }
        }
        return null;
    }

    private void updateTextField() {
        UrlListItem item = getSelUriListItem();
        if (item != null) {
            uriField.setText(item.uri.toString());
        }
    }


    private void addURI() {
        String uriString = JOptionPane.showInputDialog(this,
                                                       ProtegeProperties.getInstance().getProperty("i18n.core.openFromUrl.addPromptMessage"),
                                                       ProtegeProperties.getInstance().getProperty("i18n.core.openFromUrl.addPromptTitle"),
                                                       PLAIN_MESSAGE);
        if (uriString != null) {
            try {
                URI uri = new URI(uriString);
                BookMarkedURIManager.getInstance().add(uri);
            }
            catch (URISyntaxException e) {
                showURIErrorMessage(e);
            }
            fillList();
        }
    }


    private void showURIErrorMessage(URISyntaxException e) {
        JOptionPane.showMessageDialog(this,
                                      e.getMessage(),
                                      ProtegeProperties.getInstance().getProperty("i18n.core.openFromUrl.invalidUrlTitle"),
                                      ERROR_MESSAGE);
    }


    private void fillList() {
        List<Object> listData = new ArrayList<>();
        listData.add(new AddUrlItem());
        getBookmarkedUrls()
                .sorted()
                .map(UrlListItem::new)
                .forEach(listData::add);
        bookmarksList.setListData(listData.toArray());
    }

    private static Stream<URI> getBookmarkedUrls() {
        BookMarkedURIManager man = BookMarkedURIManager.getInstance();
        return man.getBookMarkedURIs().stream();
    }


    private void deleteSelectedBookmark() {
        Object selObj = bookmarksList.getSelectedValue();
        if (!(selObj instanceof UrlListItem)) {
            return;
        }
        UrlListItem item = (UrlListItem) selObj;
        BookMarkedURIManager.getInstance().remove(item.uri);
        fillList();
    }


    private UrlListItem getSelUriListItem() {
        if (bookmarksList.getSelectedValue() instanceof UrlListItem) {
            return (UrlListItem) bookmarksList.getSelectedValue();
        }
        return null;
    }


    public void addStatusChangedListener(InputVerificationStatusChangedListener listener) {
        listeners.add(listener);
        listener.verifiedStatusChanged(isValidURI());
    }


    public void removeStatusChangedListener(InputVerificationStatusChangedListener listener) {
        listeners.remove(listener);
    }


    private static class BookmarkedItemListRenderer extends DefaultListCellRenderer {

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof UrlListItem) {
                UrlListItem item = (UrlListItem) value;
                label.setText(item.uri.toString());
            }
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            return label;
        }
    }


    private static class AddUrlItem implements MListSectionHeader {

        public String getName() {
            return getBookmarkedUrlsLabel();
        }


        public boolean canAdd() {
            return true;
        }
    }


    private static class UrlListItem implements MListItem {

        private final URI uri;

        public UrlListItem(URI uri) {
            this.uri = uri;
        }

        public boolean isEditable() {
            return false;
        }

        public void handleEdit() {}

        public boolean isDeleteable() {
            return true;
        }

        public boolean handleDelete() {
            return true;
        }

        public String getTooltip() {
            return uri.toString();
        }
    }


    public static URI showDialog() {
        OpenFromURLPanel panel = new OpenFromURLPanel();
        int ret = JOptionPaneEx.showValidatingConfirmDialog(null, getTitle(),
                                                            panel,
                                                            PLAIN_MESSAGE,
                                                            OK_CANCEL_OPTION,
                                                            panel.uriField);
        if (ret == JOptionPane.OK_OPTION) {
            return panel.getURI();
        }
        return null;
    }

    public static void main(String[] args) {
        OpenFromURLPanel.showDialog();
    }
}
