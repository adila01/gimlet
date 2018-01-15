package cruft.wtf.gimlet.ui;


import cruft.wtf.gimlet.Column;
import cruft.wtf.gimlet.Utils;
import cruft.wtf.gimlet.jdbc.SimpleQueryTask;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.util.List;

/**
 * This class represents a generic {@link TableView} for SQL queries. The table columns are therefore variadic depending
 * on the query executed. The table expects a {@link ResultSet} to iterate over. The class is not responsible for closing
 * the resources, merely displaying the data.
 *
 * @see SimpleQueryTask
 */
public class ResultTable extends TableView<ObservableList> {

    private static Logger logger = LoggerFactory.getLogger(ResultTable.class);


    public ResultTable() {
        setEditable(false);
        // FIXME: table menu button shows columns, but mnemonic is parsed (_). Disable that somehow.
        setTableMenuButtonVisible(true);
        setPlaceholder(new Label("Query is running..."));
        setColumnResizePolicy(UNCONSTRAINED_RESIZE_POLICY);

    }

    /**
     * Sets the placeholder to mention that there are no results found.
     */
    public void setPlaceHolderNoResults() {
        setPlaceholder(new Label("No results for query."));
    }

    /**
     * Sets the columns.
     * <p>
     * TODO: since we now got a SimpleObjectProperty, do we really need the Column type? Maybe later.
     *
     * @param columnList The list of columns.
     */
    @SuppressWarnings("unchecked")
    public void setColumns(List<Column> columnList) {
        for (int i = 0; i < columnList.size(); i++) {
            final int colnum = i;
            TableColumn<ObservableList, Object> col = new TableColumn<>(columnList.get(colnum).getColumnName());
            // every column has a type dependent on the ResultSet. So just use SimpleObjectProperty.
            col.setCellValueFactory(param -> new SimpleObjectProperty(param.getValue().get(colnum)));
            col.setCellFactory(param -> {
                return new Cell();
            });
            getColumns().add(col);
        }
    }

    /**
     * Cell for the ResultTable.
     */
    private class Cell extends TextFieldTableCell<ObservableList, Object> {

        public Cell() {

        }

        @Override
        public void updateItem(Object item, boolean empty) {
            super.updateItem(item, empty);

            if (empty) {
                return;
            }

            // clear style, prevents possible display bug?
            getStyleClass().remove("null");
            getStyleClass().remove("abbrev");

            // TODO: double click on cells view their full content in a text area

            if (item == null) {
                // add a style class so it's easy to see it's nulled (from the database).
                getStyleClass().add("null");
                // set a string indicator. The text is centered via CSS.
                setText("<NULL>");
                return;
            }

            // If the cell content is larger than 32 chars, abbreviate it and mark it as such.
            // User should be able to double click on the cell to display its full data.
            String s = item.toString().replace('\n', ' ').replace('\r', ' ').trim();
            if (s.length() >= 32) {
                getStyleClass().add("abbrev");
                setText(Utils.abbrev(s, 32));
            }
        }
    }
}
