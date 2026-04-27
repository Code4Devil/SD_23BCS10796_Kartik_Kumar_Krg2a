/**
 * Question 2: Implement a Banking System using the Single Responsibility Principle (SRP).
 * 
 * SRP states that a class should have only one reason to change.
 * In this implementation, we split the banking logic into specialized classes.
 */

import java.util.HashMap;
import java.util.Map;

// 1. Account Class: Responsible ONLY for holding account data.
class Account {
    private String accountNumber;
    private double balance;

    public Account(String accountNumber, double initialBalance) {
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}

// 2. TransactionProcessor: Responsible ONLY for the logic of deposits and withdrawals.
class TransactionProcessor {
    public void deposit(Account account, double amount) {
        if (amount > 0) {
            account.setBalance(account.getBalance() + amount);
            System.out.println("[Transaction] Deposited $" + amount + " to account " + account.getAccountNumber());
        }
    }

    public boolean withdraw(Account account, double amount) {
        if (amount > 0 && account.getBalance() >= amount) {
            account.setBalance(account.getBalance() - amount);
            System.out.println("[Transaction] Withdrew $" + amount + " from account " + account.getAccountNumber());
            return true;
        }
        System.out.println("[Transaction] Withdrawal failed for account " + account.getAccountNumber() + " (Insufficient funds)");
        return false;
    }
}

// 3. NotificationService: Responsible ONLY for notifying the user.
class NotificationService {
    public void sendEmail(String accountNumber, String message) {
        System.out.println("[Notification] Email sent to owner of " + accountNumber + ": " + message);
    }

    public void sendSMS(String accountNumber, String message) {
        System.out.println("[Notification] SMS sent to owner of " + accountNumber + ": " + message);
    }
}

// 4. AccountRepository: Responsible ONLY for managing account persistence (simulated database).
class AccountRepository {
    private Map<String, Account> database = new HashMap<>();

    public void save(Account account) {
        database.put(account.getAccountNumber(), account);
        System.out.println("[Database] Account " + account.getAccountNumber() + " saved.");
    }

    public Account findByAccountNumber(String accountNumber) {
        return database.get(accountNumber);
    }
}

// 5. BankingApp: The Main class to coordinate the system.
public class BankingSystem {
    public static void main(String[] args) {
        // Initialize services
        AccountRepository repository = new AccountRepository();
        TransactionProcessor processor = new TransactionProcessor();
        NotificationService notifier = new NotificationService();

        // Create and save an account
        Account myAccount = new Account("ACC-10796", 1000.0);
        repository.save(myAccount);

        System.out.println("\n--- Starting Transactions ---\n");

        // Perform a deposit
        processor.deposit(myAccount, 500.0);
        notifier.sendEmail(myAccount.getAccountNumber(), "Your account has been credited with $500.0. New balance: $" + myAccount.getBalance());

        // Perform a withdrawal
        boolean success = processor.withdraw(myAccount, 200.0);
        if (success) {
            notifier.sendSMS(myAccount.getAccountNumber(), "Withdrawal of $200.0 successful. Current balance: $" + myAccount.getBalance());
        }

        // Attempt a large withdrawal
        processor.withdraw(myAccount, 2000.0);

        System.out.println("\n--- Final Account Status ---");
        System.out.println("Account: " + myAccount.getAccountNumber());
        System.out.println("Final Balance: $" + myAccount.getBalance());
    }
}
