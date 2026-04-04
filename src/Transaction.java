import java.time.LocalDate;
import java.util.UUID;

public class Transaction {
    private String id;
    private TransactionType type;
    private double amount;
    private String category;
    private LocalDate date;
    private String note;

    public Transaction(TransactionType type, double amount, String category, LocalDate date, String note) {
        this(UUID.randomUUID().toString(), type, amount, category, date, note);
    }

    public Transaction(String id, TransactionType type, double amount, String category, LocalDate date, String note) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note == null ? "" : note;
    }

    public String getId() {
        return id;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note == null ? "" : note;
    }
}
