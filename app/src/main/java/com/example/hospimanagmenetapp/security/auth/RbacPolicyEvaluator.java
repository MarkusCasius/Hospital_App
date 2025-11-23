package com.example.hospimanagmenetapp.security.auth;

import android.content.Context;

import com.example.hospimanagmenetapp.util.SessionManager;

public class RbacPolicyEvaluator {

    // Rbac is an additional level of security which validates the logged in user's role (or whether
    // they have logged in) to ensure that they have the required clearance before navigating
    // to an activity with sensitive information

    // Simple matrix: ADMIN & RECEPTION & CLINICIAN can view list; only ADMIN & RECEPTION can book/reschedule
    public static boolean canViewAppointments(Context ctx) {
        String role = SessionManager.getCurrentRole(ctx);
        return "ADMIN".equals(role) || "RECEPTION".equals(role) || "CLINICIAN".equals(role);
    }

    public static boolean canBookOrReschedule(Context ctx) {
        String role = SessionManager.getCurrentRole(ctx);
        return "ADMIN".equals(role) || "RECEPTION".equals(role);
    }

    public static boolean canViewEhr(Context ctx) {
        String role = SessionManager.getCurrentRole(ctx);
        return "ADMIN".equals(role) || "RECEPTION".equals(role) || "CLINICIAN".equals(role);
    }

    public static boolean canEditEhr(Context ctx) {
        String role = SessionManager.getCurrentRole(ctx);
        return "ADMIN".equals(role) || "RECEPTION".equals(role);
    }
}
