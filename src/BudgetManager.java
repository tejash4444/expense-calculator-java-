import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

public class BudgetManager {
    private final Map<YearMonth, Double> budgets;
    private final StorageManager storageManager;

    public BudgetManager(StorageManager storageManager) {
        this.storageManager = storageManager;
        this.budgets = new HashMap<>(storageManager.loadBudgets());
    }

    public void setBudget(YearMonth month, double amount) {
        budgets.put(month, amount);
        storageManager.saveBudgets(budgets);
    }

    public double getBudget(YearMonth month) {
        return budgets.getOrDefault(month, 0.0);
    }

    public double getRemainingBudget(YearMonth month, double expenses) {
        return getBudget(month) - expenses;
    }

    public String getBudgetStatus(YearMonth month, double expenses) {
        double budget = getBudget(month);
        if (budget <= 0) {
            return "No budget set for this month.";
        }
        double used = (expenses / budget) * 100.0;
        if (used >= 100.0) {
            return String.format("Budget exceeded by %.2f", expenses - budget);
        }
        if (used >= 85.0) {
            return String.format("You have used %.1f%% of your budget.", used);
        }
        return String.format("You have used %.1f%% of your budget.", used);
    }
}
