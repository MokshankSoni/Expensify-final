package com.example.expensify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.expensify.databinding.ActivityAddExpenseBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class AddExpenseActivity extends AppCompatActivity {

    ActivityAddExpenseBinding binding;
    private String type;
    private ExpenseModel expenseModel;
    private Spinner categorySpinner;
    private Button setBtn, deleteBtn; // Add buttons

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddExpenseBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        // Hide the Action Bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize the Spinner
        categorySpinner = binding.categorySpinner;

        // Initialize buttons
        setBtn = findViewById(R.id.setBtn);
        deleteBtn = findViewById(R.id.deleteBtn);

        List<Category> categories = new ArrayList<>();
        categories.add(new Category("Food", R.drawable.icon1));
        categories.add(new Category("Transport", R.drawable.icon2));
        categories.add(new Category("Household", R.drawable.icon3));
        categories.add(new Category("Cosmetics", R.drawable.icon4));
        categories.add(new Category("Cloth", R.drawable.icon5));
        categories.add(new Category("Education", R.drawable.icon6));
        categories.add(new Category("Health", R.drawable.icon7));
        categories.add(new Category("Other", R.drawable.icon8));

        CategoryAdapter categoryAdapter = new CategoryAdapter(this, categories);
        binding.categorySpinner.setAdapter(categoryAdapter);

        // Retrieve intent data
        type = getIntent().getStringExtra("type");
        expenseModel = (ExpenseModel) getIntent().getSerializableExtra("model");

        // Update screen if an existing expense is passed
        if (expenseModel != null) {
            type = expenseModel.getType();
            binding.amount.setText(String.valueOf(expenseModel.getAmount()));
            binding.note.setText(expenseModel.getNote());

            // Set Spinner selection based on the passed category
            for (int i = 0; i < categorySpinner.getCount(); i++) {
                Category cat = (Category) categorySpinner.getItemAtPosition(i);
                if (cat.getName().equals(expenseModel.getCategory())) {
                    categorySpinner.setSelection(i);
                    break;
                }
            }

            if (type.equals("Income")) {
                binding.incomeRadio.setChecked(true);
            } else {
                binding.expenseRadio.setChecked(true);
            }

            deleteBtn.setVisibility(View.VISIBLE); // Show delete button when editing an existing expense
        } else {
            deleteBtn.setVisibility(View.GONE); // Hide delete button when adding a new expense
        }

        // Set button click listeners
        setBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (expenseModel != null) {
                    updateExpense(); // Update an existing expense
                } else {
                    createExpense(); // Create a new expense
                }
            }
        });

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteExpense(); // Delete the existing expense
            }
        });

        // Radio button listeners
        binding.incomeRadio.setOnClickListener(view -> type = "Income");
        binding.expenseRadio.setOnClickListener(view -> type = "Expense");
    }

    private void deleteExpense() {
        String userId = FirebaseAuth.getInstance().getUid();

        if (userId == null) {
            Toast.makeText(AddExpenseActivity.this, "User is not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore
                .getInstance()
                .collection("users")
                .document(userId)
                .collection("expenses")
                .document(expenseModel.getExpenseId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddExpenseActivity.this, "Expense deleted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddExpenseActivity.this, "Failed to delete expense: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createExpense() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Log.e("FirestoreError", "User not logged in");
            return;
        }

        String uid = user.getUid();
        String amount = binding.amount.getText().toString();
        String note = binding.note.getText().toString();
        String category = categorySpinner.getSelectedItem().toString(); // Get selected category from Spinner

        boolean incomeChecked = binding.incomeRadio.isChecked();
        String type = incomeChecked ? "Income" : "Expense";

        if (amount.trim().isEmpty()) {
            binding.amount.setError("Empty");
            return;
        }

        String expenseId = UUID.randomUUID().toString();
        long currentTimeInMillis = Calendar.getInstance().getTimeInMillis();

        ExpenseModel expenseModel = new ExpenseModel(
                expenseId, note, category, type,
                Long.parseLong(amount),
                currentTimeInMillis,
                uid
        );

        FirebaseFirestore
                .getInstance()
                .collection("users")
                .document(uid)
                .collection("expenses")
                .document(expenseId)
                .set(expenseModel)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Expense added!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to add expense", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateExpense() {
        String expenseId = expenseModel.getExpenseId();
        String amount = binding.amount.getText().toString();
        String note = binding.note.getText().toString();
        String category = categorySpinner.getSelectedItem().toString(); // Get selected category from Spinner

        boolean incomeChecked = binding.incomeRadio.isChecked();
        type = incomeChecked ? "Income" : "Expense";

        if (amount.trim().isEmpty()) {
            binding.amount.setError("Empty");
            return;
        }
        long time = expenseModel.getTime();

        ExpenseModel model = new ExpenseModel(
                expenseId, note, category, type,
                Long.parseLong(amount),
                time,
                FirebaseAuth.getInstance().getUid()
        );

        FirebaseFirestore
                .getInstance()
                .collection("users")
                .document(FirebaseAuth.getInstance().getUid())
                .collection("expenses")
                .document(expenseId)
                .set(model)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AddExpenseActivity.this, "Expense updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddExpenseActivity.this, "Failed to update expense: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
