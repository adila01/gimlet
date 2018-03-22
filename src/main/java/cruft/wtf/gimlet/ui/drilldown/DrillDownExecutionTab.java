package cruft.wtf.gimlet.ui.drilldown;

import cruft.wtf.gimlet.conf.Query;
import cruft.wtf.gimlet.event.EventDispatcher;
import cruft.wtf.gimlet.event.QueryExecutedEvent;
import cruft.wtf.gimlet.jdbc.NamedParameterPreparedStatement;
import cruft.wtf.gimlet.jdbc.task.NamedQueryTask;
import cruft.wtf.gimlet.ui.Images;
import cruft.wtf.gimlet.ui.dialog.ParamInputDialog;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * This tab contains the logic for connecting and executing a named and parameterized query.
 */
public class DrillDownExecutionTab extends Tab {

    private final Query query;

    private final DrillDownTab drillDownTab;

    private final DrillResultTable table;

    private Map<String, Object> columnMap;

    private Button btnRerun;

    private Set<String> uniqueParams;

    public DrillDownExecutionTab(final DrillDownTab drillDownTab, final Query query, final Map<String, Object> columnMap) {
        this.drillDownTab = drillDownTab;
        this.table = new DrillResultTable(query);
        this.query = query;
        this.columnMap = columnMap;

        BorderPane pane = new BorderPane();
        Label lbl = new Label(query.getDescription());
        lbl.setPadding(new Insets(5));

        // Recompile the query so we know the unique parameters. This is used by the re-running
        NamedParameterPreparedStatement.ParseResult pr = NamedParameterPreparedStatement.parse(query.getContent());
        uniqueParams = pr.getUniqueParameters();

        btnRerun = new Button();
        btnRerun.setTooltip(new Tooltip("Re-run the query"));
        btnRerun.setGraphic(Images.RUN.imageView());
        btnRerun.setOnAction(event -> openParameterDialog());

        ToggleButton btnLol = new ToggleButton();
        btnLol.setGraphic(Images.TABLE_COLUMN_WIDTH.imageView());
        btnLol.setTooltip(new Tooltip("Toggle fit columns to table width"));
        btnLol.setOnAction(event -> {
            if (btnLol.isSelected()) {
                table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            } else {
                table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
            }
        });

        ToolBar bar = new ToolBar(btnRerun, btnLol);
        VBox box = new VBox(lbl, bar);
        pane.setTop(box);
        pane.setCenter(table);

        setText(query.getName());
        setGraphic(Images.CLOCK.imageView());
        setContent(pane);
    }

    /**
     * Opens up the parameters dialog (if uniqueParams is not empty) to request new user input.
     */
    private void openParameterDialog() {
        if (!uniqueParams.isEmpty()) {
            Map<String, Object> prevValues = new HashMap<>();
            // Gather the previously entered values:
            uniqueParams.forEach(s -> prevValues.put(s, this.columnMap.get(s)));

            // Open dialog with the previous entered values.
            ParamInputDialog dlg = new ParamInputDialog(prevValues);
            Optional<Map<String, Object>> opt = dlg.showAndWait();
            opt.ifPresent(stringObjectMap -> {
                // Update the column map so they are shown when rerunning the query.
                this.columnMap = stringObjectMap;
                this.executeQuery(stringObjectMap);
            });
        } else {
            executeQuery();
        }
    }

    /**
     * Executes the query with the columnMap as parameter values.
     */
    public void executeQuery() {
        this.executeQuery(columnMap);
    }

    /**
     * Execute the query.
     */
    public void executeQuery(final Map<String, Object> columnMap) {
        Connection connection = this.drillDownTab.getConnectionTab().getConnection();

        NamedQueryTask namedQueryTask = new NamedQueryTask(connection, query.getContent(), 100, columnMap);

        namedQueryTask.setOnScheduled(event -> {
            btnRerun.setDisable(true);
            table.getItems().clear();
            table.getColumns().clear();
        });

        namedQueryTask.setOnFailed(event -> {
            btnRerun.setDisable(false);

            setGraphic(Images.WARNING.imageView());
            // When the query failed, we add a textarea to the tab instead of the table.
            // This textarea contains the stacktrace information.
            StringWriter sw = new StringWriter();
            namedQueryTask.getException().printStackTrace(new PrintWriter(sw));
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder
                    .append("Source query:\n\n")
                    .append(namedQueryTask.getQuery())
                    .append("\n\n");

            if (!namedQueryTask.getNamedProperties().isEmpty()) {
                stringBuilder.append("Named parameters given:\n\n");
                namedQueryTask.getNamedProperties().forEach((s, o) -> {
                    stringBuilder.append(String.format("\t%s = %s\n", s, o));
                });
            } else {
                stringBuilder.append("The query does not contain named parameters.\n");
            }

            stringBuilder
                    .append("\nStacktrace:\n\n")
                    .append(sw.toString());
            TextArea area = new TextArea(stringBuilder.toString());
            area.getStyleClass().add("error-text");
            area.setEditable(false);

            setContent(area);
        });

        namedQueryTask.setOnSucceeded(event -> {
            btnRerun.setDisable(false);
            setGraphic(Images.SPREADSHEET.imageView());
            table.setColumns(namedQueryTask.columnProperty());

            if (namedQueryTask.getRowCount() <= 0) {
                table.setPlaceHolderNoResults();
            } else {
                table.setItems(namedQueryTask.getValue());
            }

            QueryExecutedEvent qee = new QueryExecutedEvent();
            qee.setQuery(namedQueryTask.getQuery());
            qee.setRuntime(namedQueryTask.getProcessingTime());
            qee.setRowCount(namedQueryTask.getRowCount());
            EventDispatcher.getInstance().post(qee);
        });

        Thread t = new Thread(namedQueryTask, "Gimlet named query task");
        t.setDaemon(true);
        t.start();
    }
}
