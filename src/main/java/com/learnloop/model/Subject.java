package com.learnloop.model;

public record Subject(
        long id,
        String code,
        String name,
        String description,
        boolean active
) {
}
