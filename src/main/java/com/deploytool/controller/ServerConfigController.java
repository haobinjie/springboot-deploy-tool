package com.deploytool.controller;

import com.deploytool.AppContext;
import com.deploytool.model.ServerConfig;
import com.deploytool.task.TaskRunner;
import com.deploytool.task.TestConnectionTask;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Optional;

/**
 * 服务器配置区控制器：维护环境下拉框 + 表单字段，负责新增/保存/删除/测试连接。
 * 字段编辑写入当前编辑的 ServerConfig（新增时为临时对象），保存落盘到 servers.json。
 */
public class ServerConfigController {

    private final AppContext ctx;

    private final ComboBox<ServerConfig> combo = new ComboBox<>();
    private final TextField nameField = new TextField();
    private final TextField hostField = new TextField();
    private final TextField portField = new TextField("22");
    private final TextField userField = new TextField();
    private final TextField passField = new TextField();
    private final TextField remoteDirField = new TextField();
    private final TextField startScriptField = new TextField();
    private final TextField stopScriptField = new TextField();

    /** 当前正在编辑的配置（可能尚未持久化）。 */
    private ServerConfig editing;

    private final Button testBtn = new Button("测试连接");
    private final Button newBtn = new Button("新增");
    private final Button saveBtn = new Button("保存");
    private final Button deleteBtn = new Button("删除");

    public ServerConfigController(AppContext ctx) {
        this.ctx = ctx;
        passField.setStyle("-fx-prompt-text: '密码(V1.0明文保存)';");
        buildLayout();
        wireEvents();
        refresh();
    }

    private void buildLayout() {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        int r = 0;
        grid.addRow(r++, new Label("环境选择"), combo);
        grid.addRow(r++, new Label("环境名称"), nameField);
        grid.addRow(r++, new Label("IP/主机"), hostField);
        grid.addRow(r++, new Label("SSH端口"), portField);
        grid.addRow(r++, new Label("账号"), userField);
        grid.addRow(r++, new Label("密码"), passField);
        grid.addRow(r++, new Label("远程目录"), remoteDirField);
        grid.addRow(r++, new Label("启动脚本"), startScriptField);
        grid.addRow(r++, new Label("停止脚本"), stopScriptField);

        HBox buttons = new HBox(8, testBtn, newBtn, saveBtn, deleteBtn);
        javafx.scene.layout.VBox root = new javafx.scene.layout.VBox(10, grid, buttons);
        this.rootNode = root;

        combo.setMaxWidth(Double.MAX_VALUE);
        List<Node> allFields = List.of(combo, nameField, hostField, portField, userField,
                passField, remoteDirField, startScriptField, stopScriptField);
        allFields.forEach(f -> {
            if (f instanceof javafx.scene.control.Control c) c.setMaxWidth(Double.MAX_VALUE);
        });
    }

    private Node rootNode;

    public Node getRoot() {
        return rootNode;
    }

    private void wireEvents() {
        combo.valueProperty().addListener((o, old, val) -> {
            if (val != null) {
                editing = val;
                ctx.configService().select(val.getId());
                loadIntoForm(val);
            }
        });
        newBtn.setOnAction(e -> startNew());
        saveBtn.setOnAction(e -> save());
        deleteBtn.setOnAction(e -> delete());
        testBtn.setOnAction(e -> testConnection());
    }

    /** 刷新下拉框数据并保持选择。 */
    public final void refresh() {
        List<ServerConfig> servers = ctx.configService().listServers();
        combo.setItems(FXCollections.observableArrayList(servers));
        Optional<ServerConfig> selected = ctx.configService().findSelected();
        if (selected.isPresent()) {
            combo.setValue(selected.get());
            loadIntoForm(selected.get());
        } else if (!servers.isEmpty()) {
            combo.setValue(servers.get(0));
        } else {
            startNew();
        }
    }

    private void startNew() {
        editing = new ServerConfig();
        editing.setPort(22);
        loadIntoForm(editing);
        combo.getSelectionModel().clearSelection();
    }

    private void loadIntoForm(ServerConfig s) {
        nameField.setText(s.getName());
        hostField.setText(s.getHost());
        portField.setText(String.valueOf(s.getPort()));
        userField.setText(s.getUsername());
        passField.setText(s.getPassword());
        remoteDirField.setText(s.getRemoteDir());
        startScriptField.setText(s.getStartScript());
        stopScriptField.setText(s.getStopScript());
    }

    private void save() {
        if (editing == null) editing = new ServerConfig();
        editing.setName(nameField.getText());
        editing.setHost(hostField.getText());
        try {
            editing.setPort(Integer.parseInt(portField.getText().trim()));
        } catch (NumberFormatException ex) {
            editing.setPort(22);
        }
        editing.setUsername(userField.getText());
        editing.setPassword(passField.getText());
        editing.setRemoteDir(remoteDirField.getText());
        editing.setStartScript(startScriptField.getText());
        editing.setStopScript(stopScriptField.getText());

        ServerConfig saved = ctx.configService().upsert(editing);
        ctx.configService().select(saved.getId());
        refresh();
        ctx.logService().info(com.deploytool.model.enums.LogCategory.SYSTEM,
                "已保存环境配置：" + saved.display());
    }

    private void delete() {
        ServerConfig cur = combo.getValue();
        if (cur != null && cur.getId() != null) {
            ctx.configService().delete(cur.getId());
            refresh();
            ctx.logService().info(com.deploytool.model.enums.LogCategory.SYSTEM,
                    "已删除环境：" + cur.display());
        }
    }

    private void testConnection() {
        ServerConfig target = editing != null ? editing : combo.getValue();
        if (target == null || target.getHost() == null) {
            ctx.logService().warn(com.deploytool.model.enums.LogCategory.SYSTEM, "请先填写服务器信息");
            return;
        }
        // 用当前表单值构造临时对象，测试未保存的配置也能生效
        ServerConfig probe = new ServerConfig();
        probe.setName(nameField.getText());
        probe.setHost(hostField.getText());
        try {
            probe.setPort(Integer.parseInt(portField.getText().trim()));
        } catch (NumberFormatException ex) {
            probe.setPort(22);
        }
        probe.setUsername(userField.getText());
        probe.setPassword(passField.getText());

        testBtn.setDisable(true);
        TestConnectionTask task = new TestConnectionTask(ctx, probe);
        task.setOnSucceeded(e -> testBtn.setDisable(false));
        task.setOnFailed(e -> testBtn.setDisable(false));
        TaskRunner.run(task);
    }

    /** 供部署区获取当前选中环境。 */
    public Optional<ServerConfig> currentServer() {
        return Optional.ofNullable(combo.getValue());
    }
}
