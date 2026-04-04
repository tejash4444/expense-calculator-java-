import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainFrame extends JFrame {
    private static final List<String> DEFAULT_CATEGORIES = Arrays.asList(
            "Food", "Travel", "Shopping", "Bills", "Health", "Education", "Entertainment", "Salary", "Other"
    );

    private final ExpenseManager expenseManager;
    private final BudgetManager budgetManager;

    private final JLabel monthLabel = new JLabel();
    private final JLabel incomeLabel = new JLabel();
    private final JLabel expenseLabel = new JLabel();
    private final JLabel balanceLabel = new JLabel();
    private final JLabel budgetLabel = new JLabel();
    private final JLabel budgetStatusLabel = new JLabel();

    private final JComboBox<String> typeCombo = new JComboBox<>(new String[]{"EXPENSE", "INCOME"});
    private final JComboBox<String> categoryCombo = new JComboBox<>(new DefaultComboBoxModel<>(DEFAULT_CATEGORIES.toArray(new String[0])));
    private final JTextField amountField = new JTextField(12);
    private final JTextField dateField = new JTextField(12);
    private final JTextField noteField = new JTextField(18);
    private final JTextField budgetField = new JTextField(10);
    private final JTextField searchField = new JTextField(16);

    private final JButton addOrUpdateButton = new JButton("Add Transaction");
    private final JButton clearButton = new JButton("Clear Form");
    private final JButton deleteButton = new JButton("Delete Selected");
    private final JButton setBudgetButton = new JButton("Set Budget");
    private final JButton prevMonthButton = new JButton("< Prev");
    private final JButton nextMonthButton = new JButton("Next >");

    private final JTextArea insightArea = new JTextArea();
    private final TransactionTableModel tableModel = new TransactionTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<TransactionTableModel> sorter = new TableRowSorter<>(tableModel);

    private YearMonth selectedMonth = YearMonth.now();
    private String editingTransactionId = null;

    public MainFrame() {
        super("Smart Expense Tracker - Java Only");
        StorageManager storageManager = new StorageManager(Paths.get("data"));
        this.expenseManager = new ExpenseManager(storageManager);
        this.budgetManager = new BudgetManager(storageManager);
        initializeLookAndFeel();
        initializeUi();
        refreshDashboard();
    }

    private void initializeLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private void initializeUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildRightPanel(), BorderLayout.EAST);

        categoryCombo.setEditable(true);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(24);
        table.setAutoCreateRowSorter(true);
        table.setRowSorter(sorter);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateFormFromSelectedRow();
            }
        });

        addOrUpdateButton.addActionListener(e -> saveTransaction());
        clearButton.addActionListener(e -> clearForm());
        deleteButton.addActionListener(e -> deleteSelectedTransaction());
        setBudgetButton.addActionListener(e -> saveBudget());
        prevMonthButton.addActionListener(e -> {
            selectedMonth = selectedMonth.minusMonths(1);
            refreshDashboard();
        });
        nextMonthButton.addActionListener(e -> {
            selectedMonth = selectedMonth.plusMonths(1);
            refreshDashboard();
        });
        searchField.addActionListener(e -> applySearchFilter());

        searchField.getDocument().addDocumentListener(SimpleDocumentListener.onChange(e -> applySearchFilter()));
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JLabel title = new JLabel("Smart Expense Tracker");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        panel.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        controls.add(prevMonthButton);
        controls.add(monthLabel);
        controls.add(nextMonthButton);
        panel.add(controls, BorderLayout.EAST);

        return panel;
    }

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 10));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Monthly Summary"));
        summaryPanel.add(makeSummaryCard("Income", incomeLabel));
        summaryPanel.add(makeSummaryCard("Expense", expenseLabel));
        summaryPanel.add(makeSummaryCard("Balance", balanceLabel));
        summaryPanel.add(makeSummaryCard("Budget", budgetLabel));
        summaryPanel.add(makeSummaryCard("Status", budgetStatusLabel));

        panel.add(summaryPanel, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new BorderLayout(10, 10));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Transactions"));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        tablePanel.add(searchPanel, BorderLayout.NORTH);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(700, 400));
        tablePanel.add(tableScroll, BorderLayout.CENTER);

        panel.add(tablePanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));
        panel.setPreferredSize(new Dimension(360, 0));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Add / Edit Transaction"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        addFormRow(formPanel, gbc, "Type", typeCombo);
        addFormRow(formPanel, gbc, "Category", categoryCombo);
        addFormRow(formPanel, gbc, "Amount", amountField);
        addFormRow(formPanel, gbc, "Date (yyyy-mm-dd)", dateField);
        addFormRow(formPanel, gbc, "Note", noteField);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.gridy++;
        formPanel.add(addOrUpdateButton, gbc);
        gbc.gridy++;
        formPanel.add(clearButton, gbc);
        gbc.gridy++;
        formPanel.add(deleteButton, gbc);

        JPanel budgetPanel = new JPanel(new GridBagLayout());
        budgetPanel.setBorder(BorderFactory.createTitledBorder("Monthly Budget"));
        GridBagConstraints bbc = new GridBagConstraints();
        bbc.insets = new Insets(6, 6, 6, 6);
        bbc.fill = GridBagConstraints.HORIZONTAL;
        bbc.gridx = 0;
        bbc.gridy = 0;
        addFormRow(budgetPanel, bbc, "Budget for selected month", budgetField);
        bbc.gridx = 0;
        bbc.gridy++;
        bbc.gridwidth = 2;
        budgetPanel.add(setBudgetButton, bbc);

        insightArea.setEditable(false);
        insightArea.setLineWrap(true);
        insightArea.setWrapStyleWord(true);
        insightArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JScrollPane insightsPane = new JScrollPane(insightArea);
        insightsPane.setBorder(BorderFactory.createTitledBorder("Smart Insights"));
        insightsPane.setPreferredSize(new Dimension(340, 260));

        panel.add(formPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(budgetPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(insightsPane);

        return panel;
    }

    private JPanel makeSummaryCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEtchedBorder(),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(150, 70));
        return card;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, String label, java.awt.Component component) {
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        panel.add(component, gbc);
        gbc.gridy++;
    }

    private void saveTransaction() {
        try {
            TransactionType type = TransactionType.valueOf(typeCombo.getSelectedItem().toString());
            String category = categoryCombo.getSelectedItem().toString().trim();
            double amount = Double.parseDouble(amountField.getText().trim());
            LocalDate date = LocalDate.parse(dateField.getText().trim());
            String note = noteField.getText().trim();

            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero.");
            }

            if (editingTransactionId == null) {
                Transaction transaction = new Transaction(type, amount, category, date, note);
                expenseManager.addTransaction(transaction);
                JOptionPane.showMessageDialog(this, "Transaction added successfully.");
            } else {
                Transaction updated = new Transaction(editingTransactionId, type, amount, category, date, note);
                expenseManager.updateTransaction(updated);
                JOptionPane.showMessageDialog(this, "Transaction updated successfully.");
            }

            clearForm();
            if (!YearMonth.from(date).equals(selectedMonth)) {
                selectedMonth = YearMonth.from(date);
            }
            refreshDashboard();
        } catch (NumberFormatException ex) {
            showError("Enter a valid amount.");
        } catch (DateTimeParseException ex) {
            showError("Enter date in yyyy-mm-dd format.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void saveBudget() {
        try {
            double budget = Double.parseDouble(budgetField.getText().trim());
            if (budget < 0) {
                throw new IllegalArgumentException("Budget cannot be negative.");
            }
            budgetManager.setBudget(selectedMonth, budget);
            refreshDashboard();
            JOptionPane.showMessageDialog(this, "Budget saved for " + selectedMonth + ".");
        } catch (NumberFormatException ex) {
            showError("Enter a valid budget amount.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteSelectedTransaction() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            showError("Select a transaction to delete.");
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Transaction selected = tableModel.getTransactionAt(modelRow);
        if (selected == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete selected transaction?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            expenseManager.deleteTransaction(selected.getId());
            clearForm();
            refreshDashboard();
        }
    }

    private void populateFormFromSelectedRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        Transaction selected = tableModel.getTransactionAt(modelRow);
        if (selected == null) {
            return;
        }

        editingTransactionId = selected.getId();
        typeCombo.setSelectedItem(selected.getType().name());
        categoryCombo.setSelectedItem(selected.getCategory());
        amountField.setText(String.valueOf(selected.getAmount()));
        dateField.setText(selected.getDate().toString());
        noteField.setText(selected.getNote());
        addOrUpdateButton.setText("Update Transaction");
    }

    private void clearForm() {
        editingTransactionId = null;
        typeCombo.setSelectedIndex(0);
        categoryCombo.setSelectedIndex(0);
        amountField.setText("");
        dateField.setText(LocalDate.now().toString());
        noteField.setText("");
        addOrUpdateButton.setText("Add Transaction");
        table.clearSelection();
    }

    private void applySearchFilter() {
        String text = searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (text.isBlank()) {
            sorter.setRowFilter(null);
            return;
        }
        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TransactionTableModel, ? extends Integer> entry) {
                for (int i = 0; i < entry.getValueCount(); i++) {
                    Object value = entry.getValue(i);
                    if (value != null && value.toString().toLowerCase(Locale.ROOT).contains(text)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void refreshDashboard() {
        monthLabel.setText(selectedMonth.toString());
        dateField.setText(LocalDate.now().toString());

        double income = expenseManager.getTotalIncome(selectedMonth);
        double expense = expenseManager.getTotalExpense(selectedMonth);
        double balance = expenseManager.getBalance(selectedMonth);
        double budget = budgetManager.getBudget(selectedMonth);

        incomeLabel.setText(String.format("%.2f", income));
        expenseLabel.setText(String.format("%.2f", expense));
        balanceLabel.setText(String.format("%.2f", balance));
        budgetLabel.setText(budget > 0 ? String.format("%.2f", budget) : "Not Set");
        budgetStatusLabel.setText(budgetManager.getBudgetStatus(selectedMonth, expense));
        budgetField.setText(budget > 0 ? String.format("%.2f", budget) : "");

        tableModel.setRows(expenseManager.getTransactionsForMonth(selectedMonth));
        insightArea.setText(expenseManager.buildInsights(selectedMonth, budget));
        applySearchFilter();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.clearForm();
            frame.setVisible(true);
        });
    }
}
