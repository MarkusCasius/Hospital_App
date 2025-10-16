package com.example.hospimanagmenetapp.util; // Utility classes for the app live here

public class ValidationUtils { // Simple holder for validation helper methods

    // NHS number validation (exactly 10 digits, with Mod 11 checksum)
    public static boolean validateNhsNumber(String nhs) { // Returns true if the NHS number is valid
        if (nhs == null) return false;                   // Null input cannot be valid

        String digits = nhs.replaceAll("\\s+", "");      // Strip all whitespace so "123 456 7890" still works
        if (!digits.matches("\\d{10}")) return false;    // Must be exactly 10 numeric digits

        int sum = 0;                                     // Accumulator for weighted sum across first 9 digits
        for (int i = 0; i < 9; i++) {                    // Process digits 0..8 (the 10th is the check digit)
            int d = digits.charAt(i) - '0';              // Convert character to its integer value
            sum += d * (10 - i);                         // Weighting: 10 for first digit down to 2 for ninth
        }

        int check = 11 - (sum % 11);                     // Compute raw check value per Mod 11 rule
        if (check == 11) check = 0;                      // If remainder produced 11, check digit is 0
        if (check == 10) return false;                   // A result of 10 is invalid per NHS rules

        int provided = digits.charAt(9) - '0';           // Extract the provided check digit (last digit)
        return check == provided;                        // Valid only if calculated and provided digits match
    }
}