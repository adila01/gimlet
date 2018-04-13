package cruft.wtf.gimlet;

import javafx.collections.ObservableList;

import java.util.Arrays;

/**
 * This class is responsible for exporting generic {@link javax.swing.text.TableView} data
 * (stored as an ObservableList of ObservableLists) to copy/paste format. Whatever, we'll see.
 */
public class DataExporter {

    private static final String SEP = System.getProperty("line.separator");

    /**
     * Exports to a basic String representation using the given options.
     *
     * @param columnNames The column names.
     * @param tableData   The actual table data.
     * @param opts        The options.
     * @return The String.
     */
    public static String exportToBasicString(
            final String[] columnNames,
            final ObservableList<ObservableList> tableData,
            final Options opts) {

        int[] colWidths = new int[columnNames.length];
        Arrays.fill(colWidths, 0);

        StringBuilder b = new StringBuilder();

        // We do two passes here. If fitWidth is set to true, we are determining the
        // maximum amount of characters which are used per column, so we can pad the
        // data in the output to get a nice looking table. If it's set to false however,
        // no padding will be done in the second pass.
        if (opts.fitWidth) {
            // Iterate over the column names. They may be longer than the actual content.
            for (int colIdx = 0; colIdx < columnNames.length; colIdx++) {
                colWidths[colIdx] = columnNames[colIdx].length();
            }

            // Iterate over each row first.
            for (ObservableList row : tableData) {
                // Then check each column, determine the maximum size.
                for (int col = 0; col < row.size(); col++) {
                    String colData = String.valueOf(row.get(col));
                    colWidths[col] = Math.max(colWidths[col], colData.length());
                }
            }
        }

        // Calculate the total length of a row, by adding all column widths.
        // This int is used to write a separator between the column names and
        // the actual data.
        int totalRowLength = 0;
        for (int w : colWidths) {
            totalRowLength += w;
            totalRowLength += opts.columnSeparator.length();
        }

        if (opts.includeColNames) {
            for (int col = 0; col < columnNames.length; col++) {
                b.append(padString(columnNames[col], colWidths[col], ' '));
                b.append(opts.columnSeparator);
            }
            b.append(SEP);
            b.append(padString("", totalRowLength, opts.columnAndDataSeparator));
            b.append(SEP);
        }

        // Iterate over all rows and columns, pad if necessary, and replace newlines,
        // carriage returns and tabs.
        for (ObservableList row : tableData) {
            // Then check each column, determine the maximum size.
            for (int col = 0; col < row.size(); col++) {
                String colData = String.valueOf(row.get(col));
                colData = colData.replace('\n', ' ');
                colData = colData.replace('\t', ' ');
                colData = colData.replace('\r', ' ');
                b.append(padString(colData, colWidths[col], ' '));
                b.append(opts.columnSeparator);
            }
            b.append(SEP);
        }


        return b.toString();
    }

    /**
     * Pads a string to the right with the requested padChar, to a maximum of len size, using the source
     * as a String.
     *
     * @param source  The String source.
     * @param len     The length to pad to.
     * @param padChar The padding character.
     * @return The new, padded String.
     */
    private static String padString(final String source, int len, char padChar) {
        StringBuilder b = new StringBuilder(source);
        int diff = len - source.length();
        if (diff > 0) {
            char[] fill = new char[diff];
            Arrays.fill(fill, padChar);
            b.append(fill);
        }

        return b.toString();
    }

    public static class Options {
        public boolean includeColNames = true;

        public boolean fitWidth = false;

        public String replaceChars = "\n\r";

        public String columnSeparator = " ";

        public char columnAndDataSeparator = '=';
    }
}
