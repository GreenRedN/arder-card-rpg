package com.green.fantasysim.api.dto;

/**
 * Standard error payload for REST API responses.
 * - error: high-level category
 * - message: human-readable details
 * - path: request path (useful for debugging)
 */
public record ApiError(String error, String message, String path) {}
