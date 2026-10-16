package com.deploytool.controller;

import com.deploytool.AppContext;
import com.deploytool.model.DeployOutcome;
import com.deploytool.model.ServerConfig;
import com.deploytool.model.enums.LogCategory;
import com.deploytool.task.OneClickDeployTask;
import com.deploytool.task.RemoteCmdTask;
import com.deploytool.task.TaskRunner;
import com.deploytool.task.UploadTask;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.nio.file.Path;

/**
 * 上传 & 部署操作区控制器：上传Jar、执行停止/启动脚本、一键部署。
 * 依赖打包区产物与服务器区当前选择。
 */
public class DeployController {

    private final AppContext ctx;
    private final BuildController buildController;
    private final ServerConfigController serverController;

    private final Label jarLabel = new Label("待上传Jar：无");
    private final Button uploadBtn = new Button("上传Jar");
    private final Button stopBtn = new Button("执行停止脚本");
    private final Button startBtn = new Button("执行启动脚本");
    private final Button deployBtn = new Button("一键部署");
    private final ProgressBar progressBar = new ProgressBar(0);

    public DeployController(AppContext ctx, BuildController buildController,
                            ServerConfigController serverController) {
        this.ctx = ctx;
        this.buildController = buildController;
        this.serverController = serverController;
        buildLayout();
        wireEvents();
    }

    private void buildLayout() {
        HBox buttons = new HBox(8, uploadBtn, stopBtn, startBtn, deployBtn);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        this.rootNode = new VBox(10, jarLabel, buttons, progressBar);
    }

    private Node rootNode;

    public Node getRoot() { return rootNode; }

    private void wireEvents() {
        // 打包区完成后同步显示待上传 jar
        buildController.currentJar().ifPresent(this::showJar);
        uploadBtn.setOnAction(e -> upload());
        stopBtn.setOnAction(e -> runScript(RemoteCmdTask.Op.STOP));
        startBtn.setOnAction(e -> runScript(RemoteCmdTask.Op.START));
        deployBtn.setOnAction(e -> oneClickDeploy());
    }

    /** 打包区完成后可由 MainView 调用刷新。 */
    public void refreshJar() {
        buildController.currentJar().ifPresentOrElse(this::showJar, () -> jarLabel.setText("待上传Jar：无"));
    }

    private void showJar(Path jar) {
        jarLabel.setText("待上传Jar：" + jar.getFileName());
    }

    private void upload() {
        Path jar = buildController.currentJar().orElse(null);
        ServerConfig server = serverController.currentServer().orElse(null);
        if (jar == null || server == null) {
            ctx.logService().warn(LogCategory.SYSTEM, "缺少 Jar 或服务器配置，无法上传");
            return;
        }
        setBusy(true);
        UploadTask task = new UploadTask(ctx, server, jar);
        progressBar.progressProperty().bind(task.progressProperty());
        TaskRunner.onFinished(task, () -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1);
            setBusy(false);
        });
        TaskRunner.run(task);
    }

    private void runScript(RemoteCmdTask.Op op) {
        ServerConfig server = serverController.currentServer().orElse(null);
        if (server == null) {
            ctx.logService().warn(LogCategory.SYSTEM, "请先选择服务器配置");
            return;
        }
        setBusy(true);
        RemoteCmdTask task = new RemoteCmdTask(ctx, server, op);
        TaskRunner.onFinished(task, () -> setBusy(false));
        TaskRunner.run(task);
    }

    private void oneClickDeploy() {
        ServerConfig server = serverController.currentServer().orElse(null);
        if (server == null || buildController.currentProjectDir().isBlank()) {
            ctx.logService().warn(LogCategory.SYSTEM, "请先选择项目目录与服务器配置");
            return;
        }
        setBusy(true);
        progressBar.setProgress(0);
        Path projectDir = Path.of(buildController.currentProjectDir());
        OneClickDeployTask task = new OneClickDeployTask(ctx, projectDir,
                buildController.currentCommand(), server);
        progressBar.progressProperty().bind(task.progressProperty());
        task.setOnSucceeded(e -> {
            DeployOutcome outcome = task.getValue();
            if (outcome != null && outcome.isSuccess()) {
                ctx.logService().success(LogCategory.SYSTEM, "一键部署成功");
            }
            afterFinish();
        });
        task.setOnFailed(e -> afterFinish());
        TaskRunner.run(task);
    }

    private void afterFinish() {
        progressBar.progressProperty().unbind();
        setBusy(false);
        refreshJar();
    }

    private void setBusy(boolean busy) {
        uploadBtn.setDisable(busy);
        stopBtn.setDisable(busy);
        startBtn.setDisable(busy);
        deployBtn.setDisable(busy);
    }
}
