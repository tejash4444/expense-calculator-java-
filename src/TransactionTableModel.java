import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TransactionTableModel extends AbstractTableModel {
    private final String[] columns = {"Date", "Type", "Category", "Amount", "Note"};
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private List<Transaction> rows = new ArrayList<>();

    public void setRows(List<Transaction> rows) {
        this.rows = new ArrayList<>(rows);
        fireTableDataChanged();
    }

    public Transaction getTransactionAt(int row) {
        if (row < 0 || row >= rows.size()) {
            return null;
        }
        return rows.get(row);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Transaction t = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> formatter.format(t.getDate());
            case 1 -> t.getType().name();
            case 2 -> t.getCategory();
            case 3 -> String.format("%.2f", t.getAmount());
            case 4 -> t.getNote();
            default -> "";
        };
    }
}
