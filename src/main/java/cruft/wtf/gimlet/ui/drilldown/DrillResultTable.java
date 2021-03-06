package cruft.wtf.gimlet.ui.drilldown;


import cruft.wtf.gimlet.conf.Query;
import cruft.wtf.gimlet.event.EventDispatcher;
import cruft.wtf.gimlet.event.QueryExecuteEvent;
import cruft.wtf.gimlet.ui.ResultTable;
import cruft.wtf.gimlet.ui.ResultTableRow;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

/**
 * This class is a specialization of the regular {@link ResultTable}. The main difference is that this class allows us
 * to 'drill down'.
 */
public class DrillResultTable extends ResultTable {

    private static Logger logger = LoggerFactory.getLogger(DrillResultTable.class);

    private final Query query;

    public DrillResultTable(final Query query) {
        this.query = query;
        setRowFactory(param -> new DrillResultTableRow());
    }

    /**
     * TableRow for this {@link DrillResultTable}, and is a specialization on the basic {@link ResultTableRow}.
     * This table row contains extra items in the context menu for the drilldown functionality.
     */
    private class DrillResultTableRow extends ResultTableRow {

        public DrillResultTableRow() {

            if (!query.getSubQueries().isEmpty()) {
                contextMenu.getItems().add(new SeparatorMenuItem());
            }

            // Create a context menu, containing the direct sub queries for the original query.
            for (Query subQuery : query.getSubQueries()) {
                MenuItem item = new MenuItem(subQuery.getName());
                item.setMnemonicParsing(false);
                contextMenu.getItems().add(item);
                item.setOnAction(event -> {
                    executeDrillDown(subQuery);
                });
            }
        }

        public void executeDrillDown(final Query subquery) {
            logger.debug("Executing subquery '{}'", subquery.getName());

            ObservableList selectedItem = getSelectionModel().getSelectedItem();

            // Get all columns of a row, and put the values in a map where the key is the column name,
            // and the value is the actual value of that column in the selected row.
            Map<String, Object> map = new TreeMap<>();
            for (int i = 0; i < getTableView().getColumns().size(); i++) {
                TableColumn thecol = getTableView().getColumns().get(i);
                String columnName = thecol.getText();
                map.put(columnName, selectedItem.get(i));
            }

            // Create an event, post it on the event bus to notify listeners that we're going to execute a
            // drilldown query.
            QueryExecuteEvent executeEvent = new QueryExecuteEvent();
            executeEvent.setQuery(subquery);
            executeEvent.setColumnnMap(map);
            EventDispatcher.getInstance().post(executeEvent);
        }

        @Override
        protected void updateItem(ObservableList item, boolean empty) {
            super.updateItem(item, empty);

            if (item == null || empty) {
                return;
            }

            setContextMenu(contextMenu);
        }
    }
}
