package com.deploytool.ui;

import com.deploytool.AppContext;
import com.deploytool.controller.BuildController;
import com.deploytool.controller.DeployController;
import com.deploytool.controller.LogController;
import com.deploytool.controller.ServerConfigController;
import com.deploytool.ui.component.LogPanel;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * 主窗口视图：装配四功能区（打包区 / 服务器配置区 / 上传&部署区 / 日志区）。
 * 仅负责布局与控制器编排，不含业务逻辑。
 */
public class MainView extends BorderPane {

    private final AppContext ctx;
    private final LogPanel logPanel = new LogPanel();
    private final LogController logController;
    private final BuildController buildController;
    private final ServerConfigController serverController;
    private final DeployController deployController;

    // 方案A：日志区可拖拽/最大化
    private final SplitPane split = new SplitPane();
    private final Button maximizeBtn = new Button("最大化日志");
    private ScrollPane configContainer;      // 上半区（配置区），最大化时临时移出
    private boolean logMaximized = false;

    public MainView(AppContext ctx) {
        this.ctx = ctx;

        this.logController = new LogController(ctx, logPanel);
        this.buildController = new BuildController(ctx);
        this.serverController = new ServerConfigController(ctx);
        this.deployController = new DeployController(ctx, buildController, serverController);

        // 打包产物就绪 → 刷新部署区待上传 jar
        buildController.setOnProductReady(deployController::refreshJar);

        setTop(menuBar());
        setCenter(content());
    }

    private MenuBar menuBar() {
        Menu fileMenu = new Menu("文件");
        javafx.scene.control.MenuItem exportLog = new javafx.scene.control.MenuItem("导出日志…");
        exportLog.setOnAction(e -> exportLog());
        fileMenu.getItems().add(exportLog);

        Menu helpMenu = new Menu("帮助");
        javafx.scene.control.MenuItem about = new javafx.scene.control.MenuItem("关于");
        about.setOnAction(e -> showAbout());
        helpMenu.getItems().add(about);

        return new MenuBar(fileMenu, helpMenu);
    }

    private VBox content() {
        TitledPane buildPane = titledCollapsible("项目打包区", buildController.getRoot());
        TitledPane serverPane = titledCollapsible("服务器配置区", serverController.getRoot());
        TitledPane deployPane = titledCollapsible("上传 & 部署操作区", deployController.getRoot());

        VBox configBox = new VBox(12, buildPane, serverPane, deployPane);
        configBox.setPadding(new Insets(12));

        // 上半区放入 ScrollPane：窗口偏小时配置区可滚动，不侵占日志空间
        configContainer = new ScrollPane(configBox);
        configContainer.setFitToWidth(true);
        configContainer.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // 日志区：不可折叠，头部带「最大化/还原」按钮
        TitledPane logPane = new TitledPane("日志输出区", logPanel);
        logPane.setCollapsible(false);
        logPane.setPadding(new Insets(4));
        logPane.setGraphic(maximizeBtn);

        // 垂直 SplitPane：上=配置区，下=日志区，分隔条可拖动放大日志
        split.setOrientation(Orientation.VERTICAL);
        split.getItems().addAll(configContainer, logPane);
        split.setDividerPositions(0.42);
        SplitPane.setResizableWithParent(configContainer, false);

        maximizeBtn.setOnAction(e -> toggleLogMaximize());

        VBox root = new VBox(split);
        VBox.setVgrow(split, Priority.ALWAYS);
        return root;
    }

    /** 最大化/还原日志区：移出或恢复上半配置区。 */
    private void toggleLogMaximize() {
        if (!logMaximized) {
            split.getItems().remove(configContainer);
            maximizeBtn.setText("还原配置区");
            logMaximized = true;
        } else {
            split.getItems().add(0, configContainer);
            split.setDividerPositions(0.42);
            maximizeBtn.setText("最大化日志");
            logMaximized = false;
        }
    }

    /** 可折叠标题面板：不操作时可收起，为日志腾出空间。 */
    private TitledPane titledCollapsible(String title, javafx.scene.Node content) {
        TitledPane tp = new TitledPane(title, content);
        tp.setCollapsible(true);
        tp.setExpanded(true);
        tp.setPadding(new Insets(8));
        return tp;
    }

    private void exportLog() {
        FileChooser fc = new FileChooser();
        fc.setTitle("导出日志");
        fc.setInitialFileName("deploy-log.txt");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("文本文件", "*.txt"));
        File file = fc.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (file != null) {
            try {
                logPanel.exportTo(file.toPath());
                ctx.logService().success(com.deploytool.model.enums.LogCategory.SYSTEM,
                        "日志已导出：" + file.getAbsolutePath());
            } catch (Exception ex) {
                ctx.logService().error(com.deploytool.model.enums.LogCategory.SYSTEM,
                        "导出失败：" + ex.getMessage());
            }
        }
    }

    private void showAbout() {
        String version = VersionInfo.current();
        new Alert(Alert.AlertType.INFORMATION,
                "SpringBoot Deploy Tool\n版本：" + version + "\n\n技术栈：Java 17 + JavaFX + SSHJ + Maven",
                ButtonType.OK).showAndWait();
    }

    /** 释放日志订阅等资源。 */
    public void dispose() {
        logController.dispose();
    }
}
