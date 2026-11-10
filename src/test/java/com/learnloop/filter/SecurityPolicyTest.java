package com.learnloop.filter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityPolicyTest {

    @Test
    void onlyClearlySafeReadMethodsBypassCsrfValidation() {
        assertTrue(CsrfFilter.isSafeMethod("GET"));
        assertTrue(CsrfFilter.isSafeMethod("HEAD"));
        assertTrue(CsrfFilter.isSafeMethod("OPTIONS"));
        assertTrue(CsrfFilter.isSafeMethod("get"));

        assertFalse(CsrfFilter.isSafeMethod("TRACE"));
        assertFalse(CsrfFilter.isSafeMethod("POST"));
        assertFalse(CsrfFilter.isSafeMethod("PUT"));
        assertFalse(CsrfFilter.isSafeMethod("DELETE"));
        assertFalse(CsrfFilter.isSafeMethod(null));
    }

    @Test
    void contentSecurityPolicyRestrictsExecutionAndEmbedding() {
        String policy = SecurityHeadersFilter.contentSecurityPolicy();

        assertTrue(policy.contains("default-src 'self'"));
        assertTrue(policy.contains("object-src 'none'"));
        assertTrue(policy.contains("frame-ancestors 'none'"));
        assertTrue(policy.contains("script-src 'self'"));
        assertTrue(policy.contains("style-src-elem 'self'"));
        assertTrue(policy.contains("style-src-attr 'unsafe-inline'"));
        assertTrue(policy.contains("form-action 'self'"));
        int scriptStart = policy.indexOf("script-src ");
        int scriptEnd = policy.indexOf(';', scriptStart);
        String scriptDirective = policy.substring(scriptStart, scriptEnd);
        assertFalse(scriptDirective.contains("'unsafe-inline'"));
        assertFalse(scriptDirective.contains("'unsafe-eval'"));
    }
}
