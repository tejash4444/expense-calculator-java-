# Smart Expense Tracker (Java Only)

A desktop expense tracker made using **only Java** and the standard library.

## Features
- Add income and expense transactions
- Edit and delete transactions
- Monthly summary (income, expense, balance)
- Monthly budget setting
- Budget warning when near or over limit
- Search transactions
- Category-wise spending analysis
- Month-to-month spending comparison
- Data stored locally in CSV files

## Project Structure
```
SmartExpenseTracker/
├── src/
│   ├── BudgetManager.java
│   ├── ExpenseManager.java
│   ├── MainFrame.java
│   ├── SimpleDocumentListener.java
│   ├── StorageManager.java
│   ├── Transaction.java
│   ├── TransactionTableModel.java
│   └── TransactionType.java
├── data/
│   ├── budgets.csv
│   └── transactions.csv
└── README.md
```

## How to Run
Open terminal in the `SmartExpenseTracker` folder and run:

### Compile
```bash
javac -d out src/*.java
```

### Run
```bash
java -cp out MainFrame
```

## Notes
- `data/transactions.csv` stores all transactions.
- `data/budgets.csv` stores monthly budgets.
- Date format is: `yyyy-mm-dd`
