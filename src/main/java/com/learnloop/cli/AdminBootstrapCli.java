package com.learnloop.cli;

import com.learnloop.dao.Database;
import com.learnloop.service.AdminBootstrapService;
import com.learnloop.service.ValidationException;

public final class AdminBootstrapCli {

    private AdminBootstrapCli() {
    }

    public static void main(String[] args) {
        String email = System.getenv("LEARNLOOP_ADMIN_EMAIL");
        String fullName = System.getenv("LEARNLOOP_ADMIN_NAME");
        String password = System.getenv("LEARNLOOP_ADMIN_PASSWORD");

        if (isBlank(email) || isBlank(fullName) || isBlank(password)) {
            System.err.println("Set LEARNLOOP_ADMIN_EMAIL, LEARNLOOP_ADMIN_NAME and "
                    + "LEARNLOOP_ADMIN_PASSWORD before running the bootstrap command.");
            System.exit(2);
            return;
        }

        try {
            long userId = new AdminBootstrapService(Database.fromConfiguration())
                    .bootstrap(email, fullName, password);
            System.out.println("First administrator created with user id " + userId + ".");
        } catch (ValidationException exception) {
            System.err.println("Administrator setup failed:");
            exception.fieldErrors().forEach((field, message) ->
                    System.err.println("  " + field + ": " + message));
            System.exit(1);
        } catch (IllegalStateException exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
        } catch (RuntimeException exception) {
            System.err.println("Administrator setup failed: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
