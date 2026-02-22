package dev.verrai.bootstrap;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.events.EventListener;
import dev.verrai.api.IsWidget;
import jakarta.enterprise.context.Dependent;
import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap table widget with optional sorting, filtering, and pagination.
 *
 * <p>The root element is a wrapper {@code <div>} that contains (in order):
 * an optional filter input, the {@code <table>}, and optional pagination controls.
 */
@Dependent
public class TableWidget extends Widget {

    public interface RowClickListener {
        void onRowClick(int rowIndex);
    }

    private final HTMLElement wrapper;
    private final HTMLElement table;
    private final HTMLElement thead;
    private final HTMLElement tbody;
    private HTMLElement filterInput;
    private HTMLElement paginationContainer;

    private String[] headers = new String[0];
    private final List<String[]> allRows = new ArrayList<>();

    private boolean sortable = false;
    private boolean filterable = false;
    private boolean striped = true;
    private boolean hoverable = true;
    private boolean bordered = false;

    private int pageSize = 0;
    private int currentPage = 0;
    private int sortColumn = -1;
    private boolean sortAscending = true;
    private String filterText = "";

    private RowClickListener rowClickListener;

    public TableWidget() {
        wrapper = (HTMLElement) Window.current().getDocument().createElement("div");
        this.element = wrapper;

        table = (HTMLElement) Window.current().getDocument().createElement("table");
        updateTableClass();

        thead = (HTMLElement) Window.current().getDocument().createElement("thead");
        table.appendChild(thead);

        tbody = (HTMLElement) Window.current().getDocument().createElement("tbody");
        table.appendChild(tbody);

        wrapper.appendChild(table);
    }

    // ---- public configuration ----

    public void setHeaders(String... headers) {
        this.headers = headers;
        renderHeaders();
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
        renderHeaders();
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
        if (filterable && filterInput == null) {
            filterInput = (HTMLElement) Window.current().getDocument().createElement("input");
            filterInput.setAttribute("type", "text");
            filterInput.setAttribute("placeholder", "Filter...");
            filterInput.setClassName("form-control mb-2");
            filterInput.addEventListener("input", e -> {
                HTMLElement input = (HTMLElement) e.getTarget();
                filterText = input.getAttribute("value") != null ? input.getAttribute("value") : "";
                // Read value via the attribute that TeaVM's HTMLInputElement exposes
                // We need to cast to HTMLInputElement to call getValue()
                if (input instanceof org.teavm.jso.dom.html.HTMLInputElement) {
                    filterText = ((org.teavm.jso.dom.html.HTMLInputElement) input).getValue();
                }
                currentPage = 0;
                render();
            });
            wrapper.insertBefore(filterInput, table);
        } else if (!filterable && filterInput != null) {
            wrapper.removeChild(filterInput);
            filterInput = null;
            filterText = "";
            render();
        }
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
        currentPage = 0;
        render();
    }

    public void setCurrentPage(int page) {
        this.currentPage = page;
        render();
    }

    public void setStriped(boolean striped) {
        this.striped = striped;
        updateTableClass();
    }

    public void setBordered(boolean bordered) {
        this.bordered = bordered;
        updateTableClass();
    }

    public void setHoverable(boolean hoverable) {
        this.hoverable = hoverable;
        updateTableClass();
    }

    public void setRowClickListener(RowClickListener listener) {
        this.rowClickListener = listener;
        render();
    }

    // ---- data mutation ----

    public void addRow(String... cells) {
        allRows.add(cells);
        render();
    }

    /**
     * Appends a row containing arbitrary widgets directly to tbody.
     * Widget rows are not subject to sort/filter/pagination.
     */
    public void addWidgetRow(IsWidget... widgets) {
        HTMLElement tr = (HTMLElement) Window.current().getDocument().createElement("tr");
        for (IsWidget w : widgets) {
            HTMLElement td = (HTMLElement) Window.current().getDocument().createElement("td");
            if (w != null && w.getElement() != null) {
                td.appendChild(w.getElement());
            }
            tr.appendChild(td);
        }
        tbody.appendChild(tr);
    }

    public void clearBody() {
        allRows.clear();
        tbody.setInnerText("");
        renderPagination(0);
    }

    // ---- accessors ----

    public int getRowCount() {
        return allRows.size();
    }

    public int getColumnCount() {
        return headers.length;
    }

    // ---- private rendering ----

    private void render() {
        tbody.setInnerText("");

        // 1. filter
        List<String[]> filtered = new ArrayList<>();
        for (String[] row : allRows) {
            if (filterText == null || filterText.isEmpty()) {
                filtered.add(row);
            } else {
                String lower = filterText.toLowerCase();
                for (String cell : row) {
                    if (cell != null && cell.toLowerCase().contains(lower)) {
                        filtered.add(row);
                        break;
                    }
                }
            }
        }

        // 2. sort
        if (sortColumn >= 0) {
            final int col = sortColumn;
            final boolean asc = sortAscending;
            filtered.sort((a, b) -> {
                String va = col < a.length ? a[col] : "";
                String vb = col < b.length ? b[col] : "";
                if (va == null) va = "";
                if (vb == null) vb = "";
                int cmp = va.compareTo(vb);
                return asc ? cmp : -cmp;
            });
        }

        int total = filtered.size();

        // 3. paginate
        List<String[]> page;
        if (pageSize > 0 && total > 0) {
            int start = currentPage * pageSize;
            if (start >= total) {
                currentPage = 0;
                start = 0;
            }
            int end = Math.min(start + pageSize, total);
            page = filtered.subList(start, end);
        } else {
            page = filtered;
        }

        // 4. render tbody
        final int[] renderedIndex = {0};
        for (String[] row : page) {
            HTMLElement tr = (HTMLElement) Window.current().getDocument().createElement("tr");
            final int rowIdx = renderedIndex[0];
            if (rowClickListener != null) {
                tr.addEventListener("click", e -> rowClickListener.onRowClick(rowIdx));
                tr.setAttribute("style", "cursor:pointer");
            }
            for (String cell : row) {
                HTMLElement td = (HTMLElement) Window.current().getDocument().createElement("td");
                td.setInnerText(cell != null ? cell : "");
                tr.appendChild(td);
            }
            tbody.appendChild(tr);
            renderedIndex[0]++;
        }

        renderPagination(total);
    }

    private void renderHeaders() {
        thead.setInnerText("");
        if (headers.length == 0) return;

        HTMLElement tr = (HTMLElement) Window.current().getDocument().createElement("tr");
        for (int i = 0; i < headers.length; i++) {
            HTMLElement th = (HTMLElement) Window.current().getDocument().createElement("th");
            th.setInnerText(headers[i]);
            if (sortable) {
                th.setAttribute("style", "cursor:pointer; user-select:none;");
                final int col = i;
                th.addEventListener("click", e -> {
                    if (sortColumn == col) {
                        sortAscending = !sortAscending;
                    } else {
                        sortColumn = col;
                        sortAscending = true;
                    }
                    render();
                });
            }
            tr.appendChild(th);
        }
        thead.appendChild(tr);
    }

    private void renderPagination(int total) {
        // Remove existing pagination container if present
        if (paginationContainer != null && paginationContainer.getParentNode() != null) {
            wrapper.removeChild(paginationContainer);
            paginationContainer = null;
        }

        if (pageSize <= 0 || total == 0) return;

        int totalPages = (total + pageSize - 1) / pageSize;
        if (totalPages <= 1) return;

        paginationContainer = (HTMLElement) Window.current().getDocument().createElement("nav");
        HTMLElement ul = (HTMLElement) Window.current().getDocument().createElement("ul");
        ul.setClassName("pagination mt-2");

        // Previous
        HTMLElement prevLi = (HTMLElement) Window.current().getDocument().createElement("li");
        prevLi.setClassName("page-item" + (currentPage == 0 ? " disabled" : ""));
        HTMLElement prevA = (HTMLElement) Window.current().getDocument().createElement("a");
        prevA.setClassName("page-link");
        prevA.setAttribute("href", "#");
        prevA.setInnerText("Previous");
        prevA.addEventListener("click", e -> {
            e.preventDefault();
            if (currentPage > 0) {
                currentPage--;
                render();
            }
        });
        prevLi.appendChild(prevA);
        ul.appendChild(prevLi);

        // Page indicator
        HTMLElement infoLi = (HTMLElement) Window.current().getDocument().createElement("li");
        infoLi.setClassName("page-item disabled");
        HTMLElement infoA = (HTMLElement) Window.current().getDocument().createElement("a");
        infoA.setClassName("page-link");
        infoA.setInnerText("Page " + (currentPage + 1) + " of " + totalPages);
        infoLi.appendChild(infoA);
        ul.appendChild(infoLi);

        // Next
        HTMLElement nextLi = (HTMLElement) Window.current().getDocument().createElement("li");
        nextLi.setClassName("page-item" + (currentPage >= totalPages - 1 ? " disabled" : ""));
        HTMLElement nextA = (HTMLElement) Window.current().getDocument().createElement("a");
        nextA.setClassName("page-link");
        nextA.setAttribute("href", "#");
        nextA.setInnerText("Next");
        nextA.addEventListener("click", e -> {
            e.preventDefault();
            if (currentPage < totalPages - 1) {
                currentPage++;
                render();
            }
        });
        nextLi.appendChild(nextA);
        ul.appendChild(nextLi);

        paginationContainer.appendChild(ul);
        wrapper.appendChild(paginationContainer);
    }

    private void updateTableClass() {
        StringBuilder cls = new StringBuilder("table");
        if (striped) cls.append(" table-striped");
        if (hoverable) cls.append(" table-hover");
        if (bordered) cls.append(" table-bordered");
        table.setClassName(cls.toString());
    }
}
