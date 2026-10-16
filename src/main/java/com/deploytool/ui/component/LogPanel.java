package com.deploytool.ui.component;

import com.deploytool.model.enums.LogCategory;
import com.deploytool.model.enums.LogLevel;
import com.deploytool.service.log.LogEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日志面板：分类彩色显示、自动滚动、清空、导出到 txt。
 * 采用 ListView 承载以便按级别着色（TextArea 无法逐行变色）。
 * 所有对外方法须在 FX 线程调用。
 */
public class LogPanel extends BorderPane {

    private static final int MAX_LINES = 5000; // 环形缓冲上限，防止长时间打包 OOM
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ObservableList<LogEvent> items = FXCollections.observableArrayList();
    private final ListView<LogEvent> listView = new ListView<>(items);
    private final Label filterLabel = new Label("全部日志");
    private volatile LogCategory filter = null; // null=全部

    public LogPanel() {
        setPadding(new Insets(6));
        listView.setCellFactory(lv -> new LogCell());
        listView.getItems().addListener((javafx.collections.ListChangeListener<LogEvent>) c -> {
            // 超出上限丢弃旧行
            while (items.size() > MAX_LINES) {
                items.remove(0);
            }
            if (!items.isEmpty()) {
                listView.scrollTo(items.size() - 1);
            }
        });

        HBox header = new HBox(8, filterLabel);
        header.setPadding(new Insets(0, 0, 4, 0));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().add(spacer);

        ContextMenu menu = new ContextMenu();
        MenuItem clearItem = new MenuItem("清空日志");
        clearItem.setOnAction(e -> items.clear());
        MenuItem allItem = new MenuItem("显示全部");
        allItem.setOnAction(e -> { filter = null; filterLabel.setText("全部日志"); reload(); });
        for (LogCategory cat : LogCategory.values()) {
            MenuItem mi = new MenuItem("仅看：" + cat.label());
            mi.setOnAction(e -> { filter = cat; filterLabel.setText(cat.label()); reload(); });
            menu.getItems().add(mi);
        }
        menu.getItems().addAll(allItem, clearItem);
        listView.setContextMenu(menu);

        setTop(header);
        setCenter(listView);
    }

    /** 追加一条日志（须在 FX 线程调用，由 LogController 通过 Platform.runLater 转入）。 */
    public void append(LogEvent event) {
        if (filter == null || filter == event.getCategory()) {
            items.add(event);
        }
    }

    public void exportTo(Path file) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (LogEvent e : items) {
            sb.append(e.getCategory().label()).append(" | ").append(e.getLevel()).append(" | ")
              .append(e.getMessage()).append(System.lineSeparator());
        }
        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void reload() {
        // 简单处理：切换过滤后仅保留提示，历史按新过滤继续累积显示
        items.clear();
    }

    /** 单元格：按级别着色 + 时间戳 + 分类前缀。 */
    private static class LogCell extends ListCell<LogEvent> {
        @Override
        protected void updateItem(LogEvent item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setStyle("");
                return;
            }
            String prefix = "[" + LocalDateTime.now().format(TS) + "][" + item.getCategory().label() + "] ";
            setText(prefix + item.getMessage());
            setStyle(colorStyle(item.getLevel()));
        }

        private String colorStyle(LogLevel level) {
            return switch (level) {
                case SUCCESS -> "-fx-text-fill: #1a7f37;";
                case ERROR -> "-fx-text-fill: #c62828;";
                case WARN -> "-fx-text-fill: #b26a00;";
                case INFO -> "-fx-text-fill: #222222;";
            };
        }
    }
}
