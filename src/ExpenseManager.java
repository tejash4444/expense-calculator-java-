import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ExpenseManager {
    private final List<Transaction> transactions;
    private final StorageManager storageManager;

    public ExpenseManager(StorageManager storageManager) {
        this.storageManager = storageManager;
        this.transactions = new ArrayList<>(storageManager.loadTransactions());
        sortTransactions();
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    public void addTransaction(Transaction transaction) {
        transactions.add(transaction);
        sortTransactions();
        save();
    }

    public void updateTransaction(Transaction updated) {
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).getId().equals(updated.getId())) {
                transactions.set(i, updated);
                sortTransactions();
                save();
                return;
            }
        }
    }

    public void deleteTransaction(String id) {
        transactions.removeIf(t -> t.getId().equals(id));
        save();
    }

    public List<Transaction> getTransactionsForMonth(YearMonth month) {
        return transactions.stream()
                .filter(t -> YearMonth.from(t.getDate()).equals(month))
                .sorted(Comparator.comparing(Transaction::getDate).reversed())
                .collect(Collectors.toList());
    }

    public double getTotalIncome(YearMonth month) {
        return sumByType(month, TransactionType.INCOME);
    }

    public double getTotalExpense(YearMonth month) {
        return sumByType(month, TransactionType.EXPENSE);
    }

    public double getBalance(YearMonth month) {
        return getTotalIncome(month) - getTotalExpense(month);
    }

    public double getOverallBalance() {
        double income = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();
        double expense = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();
        return income - expense;
    }

    public Map<String, Double> getExpenseByCategory(YearMonth month) {
        Map<String, Double> map = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .filter(t -> YearMonth.from(t.getDate()).equals(month))
                .collect(Collectors.groupingBy(
                        Transaction::getCategory,
                        Collectors.summingDouble(Transaction::getAmount)
                ));

        return map.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }

    public Optional<Map.Entry<String, Double>> getHighestExpenseCategory(YearMonth month) {
        return getExpenseByCategory(month).entrySet().stream().findFirst();
    }

    public double getPreviousMonthExpenseDifference(YearMonth currentMonth) {
        double current = getTotalExpense(currentMonth);
        double previous = getTotalExpense(currentMonth.minusMonths(1));
        return current - previous;
    }

    public String buildInsights(YearMonth month, double budget) {
        StringBuilder builder = new StringBuilder();
        double income = getTotalIncome(month);
        double expense = getTotalExpense(month);
        double balance = income - expense;

        builder.append("Month: ").append(month).append("\n");
        builder.append(String.format("Income: %.2f\n", income));
        builder.append(String.format("Expense: %.2f\n", expense));
        builder.append(String.format("Balance: %.2f\n\n", balance));

        if (budget > 0) {
            double used = expense / budget * 100.0;
            builder.append(String.format("Budget used: %.1f%%\n", used));
            if (used >= 100.0) {
                builder.append("Alert: You crossed your monthly budget.\n");
            } else if (used >= 85.0) {
                builder.append("Warning: You are close to your monthly budget.\n");
            } else {
                builder.append("Budget status: You are within your budget.\n");
            }
            builder.append('\n');
        }

        Optional<Map.Entry<String, Double>> topCategory = getHighestExpenseCategory(month);
        if (topCategory.isPresent()) {
            double totalExpense = Math.max(expense, 1.0);
            double percent = topCategory.get().getValue() / totalExpense * 100.0;
            builder.append("Top spending category: ")
                    .append(topCategory.get().getKey())
                    .append(String.format(" (%.2f, %.1f%% of expenses)\n", topCategory.get().getValue(), percent));
        } else {
            builder.append("Top spending category: No expenses recorded yet.\n");
        }

        double diff = getPreviousMonthExpenseDifference(month);
        if (diff > 0) {
            builder.append(String.format("You spent %.2f more than last month.\n", diff));
        } else if (diff < 0) {
            builder.append(String.format("You spent %.2f less than last month.\n", Math.abs(diff)));
        } else {
            builder.append("Your spending is the same as last month.\n");
        }

        Map<String, Double> byCategory = getExpenseByCategory(month);
        if (!byCategory.isEmpty()) {
            builder.append("\nCategory breakdown:\n");
            int count = 0;
            for (Map.Entry<String, Double> entry : byCategory.entrySet()) {
                builder.append("• ").append(entry.getKey())
                        .append(String.format(": %.2f\n", entry.getValue()));
                count++;
                if (count == 5) {
                    break;
                }
            }
        }

        return builder.toString();
    }

    private double sumByType(YearMonth month, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .filter(t -> YearMonth.from(t.getDate()).equals(month))
                .mapToDouble(Transaction::getAmount)
                .sum();
    }

    private void save() {
        storageManager.saveTransactions(transactions);
    }

    private void sortTransactions() {
        transactions.sort(Comparator.comparing(Transaction::getDate).reversed());
    }
}
