package com.learnloop.service;

import java.util.List;
import java.util.Objects;

public record PagedResult<T>(
        List<T> items,
        String query,
        int page,
        int pageSize,
        long totalItems
) {

    public PagedResult {
        items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (page < 1) {
            throw new IllegalArgumentException("Page number must be at least one.");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("Page size must be at least one.");
        }
        if (totalItems < 0) {
            throw new IllegalArgumentException("Total item count cannot be negative.");
        }
    }

    public List<T> getItems() {
        return items;
    }

    public String getQuery() {
        return query;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        if (totalItems == 0) {
            return 0;
        }
        long pages = (totalItems + pageSize - 1) / pageSize;
        return pages > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pages;
    }

    public boolean isHasPrevious() {
        return page > 1;
    }

    public boolean isHasNext() {
        return page < getTotalPages();
    }

    public int getPreviousPage() {
        return Math.max(1, page - 1);
    }

    public int getNextPage() {
        return Math.min(Math.max(1, getTotalPages()), page + 1);
    }

    public long getFirstItemNumber() {
        if (items.isEmpty()) {
            return 0;
        }
        return (long) (page - 1) * pageSize + 1;
    }

    public long getLastItemNumber() {
        return (long) (page - 1) * pageSize + items.size();
    }

    public boolean isHasQuery() {
        return query != null && !query.isBlank();
    }

    public boolean hasQuery() {
        return isHasQuery();
    }
}
