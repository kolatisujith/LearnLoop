package com.learnloop.service;

import com.learnloop.model.LearningClass;

public record ClassCreationResult(LearningClass learningClass, String invitationToken) {
}
