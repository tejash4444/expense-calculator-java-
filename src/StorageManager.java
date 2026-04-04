import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageManager {
    private final Path dataDir;
    private final Path transactionsFile;
    private final Path budgetsFile;

    public StorageManager(Path dataDir) {
        this.dataDir = dataDir;
        this.transactionsFile = dataDir.resolve("transactions.csv");
        this.budgetsFile = dataDir.resolve("budgets.csv");
        ensureFiles();
    }

    private void ensureFiles() {
        try {
            Files.createDirectories(dataDir);
            if (!Files.exists(transactionsFile)) {
                Files.writeString(transactionsFile, "id,type,amount,category,date,note\n", StandardCharsets.UTF_8);
            }
            if (!Files.exists(budgetsFile)) {
                Files.writeString(budgetsFile, "month,budget\n", StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage files.", e);
        }
    }

    public List<Transaction> loadTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(transactionsFile, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                List<String> parts = parseCsvLine(line);
                if (parts.size() < 6) {
                    continue;
                }
                Transaction transaction = new Transaction(
                        parts.get(0),
                        TransactionType.valueOf(parts.get(1)),
                        Double.parseDouble(parts.get(2)),
                        parts.get(3),
                        LocalDate.parse(parts.get(4)),
                        parts.get(5)
                );
                transactions.add(transaction);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load transactions.", e);
        }
        return transactions;
    }

    public void saveTransactions(List<Transaction> transactions) {
        try (BufferedWriter writer = Files.newBufferedWriter(transactionsFile, StandardCharsets.UTF_8)) {
            writer.write("id,type,amount,category,date,note\n");
            for (Transaction transaction : transactions) {
                writer.write(toCsv(List.of(
                        transaction.getId(),
                        transaction.getType().name(),
                        String.valueOf(transaction.getAmount()),
                        transaction.getCategory(),
                        transaction.getDate().toString(),
                        transaction.getNote()
                )));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not save transactions.", e);
        }
    }

    public Map<YearMonth, Double> loadBudgets() {
        Map<YearMonth, Double> budgets = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(budgetsFile, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                List<String> parts = parseCsvLine(line);
                if (parts.size() < 2) {
                    continue;
                }
                budgets.put(YearMonth.parse(parts.get(0)), Double.parseDouble(parts.get(1)));
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load budgets.", e);
        }
        return budgets;
    }

    public void saveBudgets(Map<YearMonth, Double> budgets) {
        try (BufferedWriter writer = Files.newBufferedWriter(budgetsFile, StandardCharsets.UTF_8)) {
            writer.write("month,budget\n");
            for (Map.Entry<YearMonth, Double> entry : budgets.entrySet()) {
                writer.write(toCsv(List.of(entry.getKey().toString(), String.valueOf(entry.getValue()))));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not save budgets.", e);
        }
    }

    private String toCsv(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(escape(values.get(i))).append('"');
        }
        return builder.toString();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "\"\"");
    }

    private List<String> parseCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts;
    }
}
