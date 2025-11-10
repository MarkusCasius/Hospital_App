package com.example.hospimanagmenetapp.security.auth;

import android.content.Context;

import com.example.hospimanagmenetapp.util.SessionManager;

public class RbacPolicyEvaluator {

    // Simple matrix: ADMIN & RECEPTION & CLINICIAN can view list; only ADMIN & RECEPTION can book/reschedule
    public static boolean canViewAppointments(Context ctx) {
        String role = SessionManager.getCurrentRole(ctx);
        return "ADMIN".equals(role) || "RECEPTION".equals(role) || "CLINICIAN".equals(role);
    }

    public static boolean canBookOrReschedule(Context ctx) {
        String role = SessionManager.getCurrentRole(ctx);
        return "ADMIN".equals(role) || "RECEPTION".equals(role);
    }
}
