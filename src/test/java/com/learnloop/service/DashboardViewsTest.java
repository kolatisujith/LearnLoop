package com.learnloop.service;

import com.learnloop.service.DashboardViews.StudentJoinRequestRow;
import com.learnloop.service.DashboardViews.StudentMaterialRow;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardViewsTest {

    @Test
    void mapsJoinRequestStatusesForStudentFacingDisplay() {
        StudentJoinRequestRow pending = joinRequest("PENDING");
        assertEquals("Pending", pending.getStatusLabel());
        assertEquals("ll-state-pending", pending.getStatusCssClass());
        assertTrue(pending.isPending());

        StudentJoinRequestRow accepted = joinRequest("APPROVED");
        assertEquals("Accepted", accepted.getStatusLabel());
        assertEquals("ll-state-approved", accepted.getStatusCssClass());
        assertFalse(accepted.isPending());

        StudentJoinRequestRow rejected = joinRequest("REJECTED");
        assertEquals("Rejected", rejected.getStatusLabel());
        assertEquals("ll-state-rejected", rejected.getStatusCssClass());
        assertFalse(rejected.isPending());

        assertEquals("Cancelled", joinRequest("CANCELLED").getStatusLabel());
    }

    @Test
    void exposesOnlySafeHttpResourceUrls() {
        assertEquals("https://example.com/material",
                material("https://example.com/material").getSafeResourceUrl());
        assertEquals("HTTP://example.com/material",
                material("HTTP://example.com/material").getSafeResourceUrl());
        assertNull(material("javascript:alert(1)").getSafeResourceUrl());
        assertNull(material("data:text/html,unsafe").getSafeResourceUrl());
        assertNull(material(null).getSafeResourceUrl());
    }

    private static StudentJoinRequestRow joinRequest(String status) {
        return new StudentJoinRequestRow(
                1L,
                "Algebra Foundations",
                "MATH",
                status,
                null,
                new Date(),
                2L,
                "ACTIVE",
                "Mathematics",
                "Ada Teacher",
                "PENDING".equals(status) ? null : new Date());
    }

    private static StudentMaterialRow material(String resourceUrl) {
        return new StudentMaterialRow(
                1L,
                "Algebra Foundations",
                "MATH",
                "Formula sheet",
                "Reference",
                "LINK",
                null,
                resourceUrl,
                new Date());
    }
}
